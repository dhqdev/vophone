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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.bald.uriah.baldphone.R;

/**
 * The screens that ring: the alarm screen and the pill reminder screen.
 * <p>
 * They are opened from a broadcast receiver, while vophone may be in the background. Since Android 10
 * that only works with "appear on top" allowed, as device owner (kiosk) or with the
 * START_ACTIVITIES_FROM_BACKGROUND permission (vophone ROM). Otherwise the screen would silently not open
 * and the alarm would be lost, so a ringing full screen notification opens it instead
 * (straight away with the screen off, as a pop-up to tap while the phone is in use).
 */
public final class RingingScreens {
    private static final String TAG = RingingScreens.class.getSimpleName();
    public static final int ALARM_NOTIFICATION_ID = 4211, PILL_NOTIFICATION_ID = 4212;
    private static final String CHANNEL_ID = "vophone_alarms";
    private static final String START_ACTIVITIES_FROM_BACKGROUND = "android.permission.START_ACTIVITIES_FROM_BACKGROUND";
    private static final long[] VIBRATION_PATTERN = {0, 800, 800};
    private static final long NOTIFICATION_TIMEOUT_MS = 10 * 60 * 1000L;
    private static final AudioAttributes ALARM_ATTRIBUTES = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

    private RingingScreens() {
    }

    /**
     * {@link PendingIntent#FLAG_IMMUTABLE} where it exists: nobody else needs to change these intents
     * (and Android 12+ requires saying it as soon as targetSdk is raised).
     */
    public static int immutable(int flags) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? flags | PendingIntent.FLAG_IMMUTABLE : flags;
    }

    /**
     * Opens a ringing screen from a broadcast receiver (see the class comment).
     *
     * @param notificationId {@link #ALARM_NOTIFICATION_ID} or {@link #PILL_NOTIFICATION_ID}
     */
    public static void open(Context context, Intent screen, int notificationId, @StringRes int title, @Nullable CharSequence text) {
        screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (canStartFromBackground(context)) {
            try {
                context.startActivity(screen);
                return;
            } catch (Exception e) {
                Log.e(TAG, "could not open the ringing screen, showing a notification instead", e);
            }
        }
        showNotification(context, screen, notificationId, title, text);
    }

    /**
     * Called by the ringing screen once it is showing: it rings by itself.
     */
    public static void cancelNotification(Context context, int notificationId) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.cancel(notificationId);
    }

    /**
     * @return true if vophone may open a screen while it is in the background (see the class comment).
     */
    public static boolean canStartFromBackground(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return true;
        if (Settings.canDrawOverlays(context))
            return true;
        if (context.checkSelfPermission(START_ACTIVITIES_FROM_BACKGROUND) == PackageManager.PERMISSION_GRANTED)
            return true;
        final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    private static void showNotification(Context context, Intent screen, int notificationId, @StringRes int title, @Nullable CharSequence text) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.alarms_channel), NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, ALARM_ATTRIBUTES);
            channel.enableVibration(true);
            channel.setVibrationPattern(VIBRATION_PATTERN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);
            notificationManager.createNotificationChannel(channel);
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, screen, immutable(PendingIntent.FLAG_UPDATE_CURRENT));
        final Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_alarm)
                .setContentTitle(context.getString(title))
                .setContentText(text)
                .setCategory(notificationId == ALARM_NOTIFICATION_ID ? NotificationCompat.CATEGORY_ALARM : NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, android.media.AudioManager.STREAM_ALARM) // before Android 8
                .setVibrate(VIBRATION_PATTERN)
                .setTimeoutAfter(NOTIFICATION_TIMEOUT_MS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .build();
        notification.flags |= Notification.FLAG_INSISTENT; // keeps ringing until it is opened
        try {
            notificationManager.notify(notificationId, notification);
        } catch (Exception e) {
            Log.e(TAG, "could not show the alarm notification", e);
        }
    }

    /**
     * Sound (looping where Android allows it) and vibration of a ringing screen, until {@link #stop()}.
     * Elderly users may not hear a short sound once, or not hear at all: it keeps going, and vibrates too.
     */
    public static final class Ringer {
        @Nullable
        private final Ringtone ringtone;
        @Nullable
        private final Vibrator vibrator;
        private boolean ringing;

        public Ringer(Context context, @Nullable Ringtone ringtone) {
            this.ringtone = ringtone;
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        @SuppressWarnings("deprecation")
        public void start() {
            if (ringing)
                return;
            ringing = true;
            if (ringtone != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        ringtone.setLooping(true);
                    ringtone.play();
                } catch (Exception e) {
                    Log.e(TAG, "could not play the sound", e);
                }
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0), ALARM_ATTRIBUTES);
                    else
                        vibrator.vibrate(VIBRATION_PATTERN, 0, ALARM_ATTRIBUTES);
                } catch (Exception e) {
                    Log.e(TAG, "could not vibrate", e);
                }
            }
        }

        public void stop() {
            ringing = false;
            if (ringtone != null) {
                try {
                    ringtone.stop();
                } catch (Exception e) {
                    Log.e(TAG, "could not stop the sound", e);
                }
            }
            if (vibrator != null)
                vibrator.cancel();
        }
    }
}
