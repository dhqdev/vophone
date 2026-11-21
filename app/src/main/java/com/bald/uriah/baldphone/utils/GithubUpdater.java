/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bald.uriah.baldphone.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.broadcast_receivers.UpdateInstallReceiver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates vophone from the GitHub releases of {@link BuildConfig#UPDATE_REPO}.
 * <p>
 * Every tag "vX.Y.Z" pushed to GitHub builds an APK and publishes it as a release
 * (.github/workflows/release.yml). vophone looks for a newer release at most every few hours
 * (and when "Procurar atualizações" is pressed in the settings), downloads the APK and installs it:
 * <ul>
 * <li>silently when vophone is the device owner (kiosk setup) or a privileged app of the vophone ROM;</li>
 * <li>otherwise Android shows its own "Update this app?" screen.</li>
 * </ul>
 * Automatic checks skip mobile data (only Wi-Fi) and never run during a call.
 */
public final class GithubUpdater {
    private static final String TAG = GithubUpdater.class.getSimpleName();
    private static final String LAST_CHECK_KEY = "GITHUB_UPDATER_LAST_CHECK";
    private static final long CHECK_EVERY_MS = 6 * 60 * 60 * 1000L;
    private static final Pattern VERSION = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static volatile boolean running;

    private GithubUpdater() {
    }

    /**
     * Same formula as versionCode in app/build.gradle: v1.2.3 -> 1010203.
     *
     * @return -1 if the tag is not like v1.2.3
     */
    public static int versionCodeOf(String tag) {
        final Matcher matcher = VERSION.matcher(tag.trim());
        if (!matcher.matches())
            return -1;
        final int major = Integer.parseInt(matcher.group(1)), minor = Integer.parseInt(matcher.group(2)), patch = Integer.parseInt(matcher.group(3));
        if (minor > 99 || patch > 99)
            return -1;
        return 1000000 + major * 10000 + minor * 100 + patch;
    }

    /**
     * Called by the home screen: checks for a new version if the last check is old enough.
     */
    public static void checkIfDue(Context context) {
        final SharedPreferences prefs = BPrefs.get(context);
        if (System.currentTimeMillis() - prefs.getLong(LAST_CHECK_KEY, 0) < CHECK_EVERY_MS)
            return;
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || connectivityManager.getActiveNetworkInfo() == null || connectivityManager.isActiveNetworkMetered())
            return; // no internet, or mobile data: try again later on Wi-Fi
        prefs.edit().putLong(LAST_CHECK_KEY, System.currentTimeMillis()).apply();
        check(context, false);
    }

    /**
     * @param interactive true when the user asked for it: tell them what happens.
     */
    public static void check(Context context, boolean interactive) {
        if (running) {
            if (interactive)
                toast(context, R.string.update_already_running, BaldToast.TYPE_INFORMATIVE);
            return;
        }
        running = true;
        final Context appContext = context.getApplicationContext();
        if (interactive)
            toast(appContext, R.string.update_checking, BaldToast.TYPE_INFORMATIVE);
        new Thread(() -> {
            try {
                run(appContext, interactive);
            } catch (Exception e) {
                Log.e(TAG, "update failed", e);
                if (interactive)
                    toast(appContext, R.string.update_failed, BaldToast.TYPE_ERROR);
            } finally {
                running = false;
            }
        }, "vophone-updater").start();
    }

    private static void run(Context context, boolean interactive) throws Exception {
        final JSONObject release = new JSONObject(new String(get("https://api.github.com/repos/" + BuildConfig.UPDATE_REPO + "/releases/latest"), "UTF-8"));
        final String tag = release.optString("tag_name");
        final int latestCode = versionCodeOf(tag);
        final String apkUrl = apkUrl(release);
        if (latestCode <= installedVersionCode(context) || apkUrl == null) {
            if (interactive)
                toast(context, R.string.baldphone_is_up_to_date, BaldToast.TYPE_INFORMATIVE);
            return;
        }
        Log.i(TAG, "updating to " + tag);
        if (interactive)
            toast(context, R.string.downloading_updates, BaldToast.TYPE_INFORMATIVE);
        final File apk = new File(context.getCacheDir(), "vophone-update.apk");
        download(apkUrl, apk);
        install(context, apk, interactive);
    }

    @SuppressWarnings("deprecation")
    private static int installedVersionCode(Context context) throws PackageManager.NameNotFoundException {
        final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return packageInfo.versionCode;
    }

    @Nullable
    private static String apkUrl(JSONObject release) {
        final JSONArray assets = release.optJSONArray("assets");
        if (assets == null)
            return null;
        for (int i = 0; i < assets.length(); i++) {
            final JSONObject asset = assets.optJSONObject(i);
            if (asset != null && asset.optString("name").endsWith(".apk"))
                return asset.optString("browser_download_url");
        }
        return null;
    }

    private static void install(Context context, File apk, boolean interactive) throws IOException {
        final PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(context.getPackageName());
        final int sessionId = packageInstaller.createSession(params);
        try (PackageInstaller.Session session = packageInstaller.openSession(sessionId)) {
            try (InputStream in = new FileInputStream(apk);
                 OutputStream out = session.openWrite("vophone.apk", 0, apk.length())) {
                copy(in, out);
                session.fsync(out);
            }
            final Intent statusIntent = new Intent(context, UpdateInstallReceiver.class)
                    .putExtra(UpdateInstallReceiver.EXTRA_INTERACTIVE, interactive);
            final int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, statusIntent, flags);
            // Device owner / privileged (ROM): installs right away. Otherwise the receiver shows Android's confirmation.
            session.commit(pendingIntent.getIntentSender());
        }
    }

    private static byte[] get(String url) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = open(url)) {
            copy(in, out);
        }
        return out.toByteArray();
    }

    private static void download(String url, File file) throws IOException {
        try (InputStream in = open(url); OutputStream out = new FileOutputStream(file)) {
            copy(in, out);
        }
    }

    private static InputStream open(String url) throws IOException {
        // GitHub sends the APK through redirects (github.com -> objects.githubusercontent.com), all https.
        for (int redirects = 0; redirects < 5; redirects++) {
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("Accept", url.contains("api.github.com") ? "application/vnd.github+json" : "application/octet-stream");
            connection.setRequestProperty("User-Agent", "vophone-updater");
            connection.setInstanceFollowRedirects(false);
            final int code = connection.getResponseCode();
            if (code >= 300 && code < 400) {
                final String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || !location.startsWith("https://"))
                    throw new IOException("bad redirect from " + url);
                url = location;
                continue;
            }
            if (code != HttpURLConnection.HTTP_OK)
                throw new IOException("HTTP " + code + " for " + url);
            return connection.getInputStream();
        }
        throw new IOException("too many redirects");
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1)
            out.write(buffer, 0, read);
    }

    static void toast(Context context, int text, int type) {
        new Handler(Looper.getMainLooper()).post(() -> BaldToast.from(context).setType(type).setText(text).show());
    }
}
