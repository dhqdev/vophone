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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.bald.uriah.baldphone.R;

/**
 * The big tiles of the vophone home screen (layout/vophone_tile.xml) and the actions behind them.
 */
public final class Tiles {
    private static final String TAG = Tiles.class.getSimpleName();

    public static final String WHATSAPP = "com.whatsapp";
    public static final String[] FACEBOOK = {"com.facebook.katana", "com.facebook.lite"};
    /**
     * App stores, best first. Google Play comes with the phone (or with MindTheGapps in the vophone ROM);
     * Aurora Store and F-Droid work without a Google account.
     */
    private static final String[] APP_STORES = {"com.android.vending", "com.aurora.store", "org.fdroid.fdroid", "com.sec.android.app.samsungapps", "com.xiaomi.market"};

    public static final int GREEN = 0xFF2E7D32, WHATSAPP_GREEN = 0xFF1FAF54, ORANGE = 0xFFEF6C00, FACEBOOK_BLUE = 0xFF1877F2,
            RED = 0xFFC62828, PURPLE = 0xFF6A1B9A, BLUE = 0xFF1565C0, TEAL = 0xFF00796B, AMBER = 0xFFFF8F00,
            PINK = 0xFFC2185B, INDIGO = 0xFF3949AB, CYAN = 0xFF00838F, BROWN = 0xFF6D4C41, GRAY = 0xFF546E7A, DEEP_ORANGE = 0xFFD84315;

    private Tiles() {
    }

    /**
     * Fills a tile: a white glyph on a colored rounded square, and the name below.
     */
    public static View setup(View tile, @DrawableRes int glyph, @ColorInt int color, @StringRes int label, View.OnClickListener onClickListener) {
        final Context context = tile.getContext();
        return setup(tile, coloredIcon(context, glyph, color), context.getText(label), onClickListener);
    }

    public static View setup(View tile, Drawable icon, CharSequence label, View.OnClickListener onClickListener) {
        ((ImageView) tile.findViewById(R.id.tile_icon)).setImageDrawable(icon);
        ((TextView) tile.findViewById(R.id.tile_label)).setText(label);
        tile.setContentDescription(label);
        tile.setOnClickListener(onClickListener);
        return tile;
    }

    /**
     * For the smaller tiles of the "Mais" screen.
     */
    public static void makeSmall(View tile) {
        final TextView label = tile.findViewById(R.id.tile_label);
        label.getLayoutParams().height = dp(tile.getContext(), 26);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label, 10, 18, 1, TypedValue.COMPLEX_UNIT_DIP);
    }

    public static void setBadge(View tile, boolean visible) {
        tile.findViewById(R.id.tile_badge).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static Drawable coloredIcon(Context context, @DrawableRes int glyph, @ColorInt int color) {
        final GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(context, 22));
        background.setColor(color);
        final Drawable foreground = ContextCompat.getDrawable(context, glyph).mutate();
        foreground.setTint(Color.WHITE);
        final int size = dp(context, 96);
        final int inset = size * 20 / 100;
        background.setSize(size, size);
        return new LayerDrawable(new Drawable[]{background, new InsetDrawable(foreground, inset)});
    }

    /**
     * The real icon of an installed app (like any home screen shows it), or our own icon if it is not installed.
     */
    public static Drawable appIcon(Context context, @Nullable String packageName, @DrawableRes int glyph, @ColorInt int color) {
        if (packageName != null) {
            try {
                return context.getPackageManager().getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException ignore) {
            }
        }
        return coloredIcon(context, glyph, color);
    }

    @Nullable
    public static String firstInstalled(Context context, String... packageNames) {
        final PackageManager pm = context.getPackageManager();
        for (final String packageName : packageNames)
            if (pm.getLaunchIntentForPackage(packageName) != null)
                return packageName;
        return null;
    }

    /**
     * Opens the app, or its page in the app store so it can be installed.
     */
    public static void openAppOrStorePage(Context context, String... packageNames) {
        final String installed = firstInstalled(context, packageNames);
        if (installed != null) {
            start(context, context.getPackageManager().getLaunchIntentForPackage(installed));
            return;
        }
        // (checked before starting: the home screen activity swallows "activity not found" errors)
        final Intent storePage = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageNames[0]));
        if (storePage.resolveActivity(context.getPackageManager()) != null)
            start(context, storePage);
        else
            start(context, new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageNames[0])));
    }

    public static void openAppStore(Context context) {
        final String store = firstInstalled(context, APP_STORES);
        if (store != null) {
            start(context, context.getPackageManager().getLaunchIntentForPackage(store));
            return;
        }
        final Intent anyStore = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MARKET);
        if (anyStore.resolveActivity(context.getPackageManager()) != null)
            start(context, anyStore);
        else
            BaldToast.from(context).setType(BaldToast.TYPE_ERROR).setText(R.string.no_app_store).show();
    }

    public static void openCamera(Context context) {
        final PackageManager pm = context.getPackageManager();
        final ResolveInfo resolveInfo = pm.resolveActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            start(context, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    .setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)));
            return;
        }
        final ResolveInfo capture = pm.resolveActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), PackageManager.MATCH_DEFAULT_ONLY);
        final Intent launch = capture == null || capture.activityInfo == null ? null : pm.getLaunchIntentForPackage(capture.activityInfo.packageName);
        if (launch != null)
            start(context, launch);
        else
            BaldToast.error(context);
    }

    public static void start(Context context, @Nullable Intent intent) {
        if (intent == null) {
            BaldToast.error(context);
            return;
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Log.e(TAG, "could not open " + intent, e);
            BaldToast.error(context);
        }
    }

    private static int dp(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
