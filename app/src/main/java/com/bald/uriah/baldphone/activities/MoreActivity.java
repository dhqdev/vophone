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
package com.bald.uriah.baldphone.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.alarms.AlarmsActivity;
import com.bald.uriah.baldphone.activities.contacts.ContactsActivity;
import com.bald.uriah.baldphone.activities.media.PhotosActivity;
import com.bald.uriah.baldphone.activities.media.VideosActivity;
import com.bald.uriah.baldphone.activities.pills.PillsActivity;
import com.bald.uriah.baldphone.databases.calls.CallLogsHelper;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.Tiles;

/**
 * "Mais" tile of the home screen: everything that does not fit there, as big tiles,
 * including the app store to download other apps.
 */
public class MoreActivity extends BaldActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);
        tile(R.id.tile_contacts, R.drawable.human_on_background, Tiles.BLUE, R.string.contacts,
                v -> startActivity(new Intent(this, ContactsActivity.class)));
        tile(R.id.tile_recent, R.drawable.history_on_background, Tiles.TEAL, R.string.recent,
                v -> startActivity(new Intent(this, RecentActivity.class)));
        tile(R.id.tile_dialer, R.drawable.glyph_dialpad, Tiles.GREEN, R.string.dialer,
                v -> startActivity(new Intent(this, DialerActivity.class)));
        tile(R.id.tile_messages, R.drawable.message_on_button, Tiles.AMBER, R.string.messages, v -> {
            final String smsPackage = Telephony.Sms.getDefaultSmsPackage(this);
            Tiles.start(this, smsPackage == null ? null : getPackageManager().getLaunchIntentForPackage(smsPackage));
        });
        tile(R.id.tile_photos, R.drawable.photo_on_button, Tiles.PINK, R.string.photos,
                v -> startActivity(new Intent(this, PhotosActivity.class)));
        tile(R.id.tile_videos, R.drawable.movie_on_background, Tiles.RED, R.string.videos,
                v -> startActivity(new Intent(this, VideosActivity.class)));
        tile(R.id.tile_alarms, R.drawable.alarm_on_background, Tiles.INDIGO, R.string.alarms,
                v -> startActivity(new Intent(this, AlarmsActivity.class)));
        tile(R.id.tile_pills, R.drawable.pill, Tiles.CYAN, R.string.pills,
                v -> startActivity(new Intent(this, PillsActivity.class)));
        tile(R.id.tile_notifications, R.drawable.notification_some_on_background, Tiles.DEEP_ORANGE, R.string.notifications,
                v -> startActivity(new Intent(this, NotificationsActivity.class)));
        tile(R.id.tile_store, R.drawable.glyph_store, Tiles.WHATSAPP_GREEN, R.string.tile_app_store,
                v -> Tiles.openAppStore(this));
        tile(R.id.tile_all_apps, R.drawable.apps_on_button, Tiles.PURPLE, R.string.tile_all_apps,
                v -> startActivity(new Intent(this, AppsActivity.class)));
        tile(R.id.tile_internet, R.drawable.web_on_button, Tiles.BLUE, R.string.internet,
                v -> Tiles.start(this, new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))));
        tile(R.id.tile_maps, R.drawable.map_on_button, Tiles.GREEN, R.string.maps,
                v -> Tiles.start(this, new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))));
        tile(R.id.tile_assistant, R.drawable.voice_on_button, Tiles.INDIGO, R.string.assistant, v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                BaldToast.from(this).setType(BaldToast.TYPE_ERROR).setText(R.string.your_phone_doesnt_have_assistant_installed).show();
            }
        });
        tile(R.id.tile_settings, R.drawable.settings_on_button, Tiles.GRAY, R.string.settings,
                v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void tile(int id, int glyph, int color, int label, View.OnClickListener onClickListener) {
        final View tile = findViewById(id);
        Tiles.setup(tile, glyph, color, label, onClickListener);
        Tiles.makeSmall(tile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tiles.setBadge(findViewById(R.id.tile_recent), !CallLogsHelper.isAllReadSafe(getContentResolver()));
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_NONE;
    }
}
