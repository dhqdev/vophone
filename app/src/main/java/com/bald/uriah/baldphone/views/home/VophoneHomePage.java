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

package com.bald.uriah.baldphone.views.home;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.HomeScreenActivity;
import com.bald.uriah.baldphone.activities.MoreActivity;
import com.bald.uriah.baldphone.activities.PhoneMenuActivity;
import com.bald.uriah.baldphone.activities.SOSActivity;
import com.bald.uriah.baldphone.databases.calls.CallLogsHelper;
import com.bald.uriah.baldphone.services.NotificationListenerService;
import com.bald.uriah.baldphone.utils.Tiles;

import java.util.ArrayList;

/**
 * The vophone home screen: six big tiles in two columns.
 * Telefone, WhatsApp, Câmera, Facebook, SOS and "Mais" (everything else, including the app store).
 * The date, clock and battery are in the bar above it (layout/home_screen_top_bar.xml).
 */
@SuppressLint("ViewConstructor")
public class VophoneHomePage extends HomeView {
    public static final String TAG = VophoneHomePage.class.getSimpleName();
    private View phone, whatsapp, camera, facebook, sos, more;
    private boolean registered;

    /**
     * New WhatsApp messages and missed calls show a red dot on their tile.
     */
    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ArrayList<String> packages = intent.getStringArrayListExtra("packages");
            Tiles.setBadge(whatsapp, packages != null && packages.contains(Tiles.WHATSAPP));
            Tiles.setBadge(phone, !CallLogsHelper.isAllReadSafe(context.getContentResolver()));
        }
    };

    public VophoneHomePage(@NonNull HomeScreenActivity homeScreen) {
        super(homeScreen, homeScreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
        final View view = inflater.inflate(R.layout.home_main_page, container, false);
        phone = view.findViewById(R.id.tile_phone);
        whatsapp = view.findViewById(R.id.tile_whatsapp);
        camera = view.findViewById(R.id.tile_camera);
        facebook = view.findViewById(R.id.tile_facebook);
        sos = view.findViewById(R.id.tile_sos);
        more = view.findViewById(R.id.tile_more);

        Tiles.setup(phone, R.drawable.phone_on_button, Tiles.GREEN, R.string.phone,
                v -> activity.startActivity(new Intent(activity, PhoneMenuActivity.class)));
        Tiles.setup(camera, R.drawable.camera_on_background, Tiles.ORANGE, R.string.camera,
                v -> Tiles.openCamera(activity));
        Tiles.setup(sos, R.drawable.emergency_on_button, Tiles.RED, R.string.sos, v -> {
            activity.startActivity(new Intent(activity, SOSActivity.class));
            activity.overridePendingTransition(R.anim.slide_in_down, R.anim.nothing);
        });
        Tiles.setup(more, R.drawable.apps_on_button, Tiles.PURPLE, R.string.tile_more,
                v -> activity.startActivity(new Intent(activity, MoreActivity.class)));
        updateAppTiles();
        return view;
    }

    /**
     * WhatsApp and Facebook show their own icon once installed; before that the tile opens the app store page.
     */
    private void updateAppTiles() {
        final Context context = activity;
        Tiles.setup(whatsapp,
                Tiles.appIcon(context, Tiles.firstInstalled(context, Tiles.WHATSAPP), R.drawable.whatsapp_on_button, Tiles.WHATSAPP_GREEN),
                context.getText(R.string.tile_whatsapp),
                v -> Tiles.openAppOrStorePage(context, Tiles.WHATSAPP));
        Tiles.setup(facebook,
                Tiles.appIcon(context, Tiles.firstInstalled(context, Tiles.FACEBOOK), R.drawable.glyph_facebook_f, Tiles.FACEBOOK_BLUE),
                context.getText(R.string.tile_facebook),
                v -> Tiles.openAppOrStorePage(context, Tiles.FACEBOOK));
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE)
            updateAppTiles(); // WhatsApp or Facebook may have just been installed
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!registered) {
            LocalBroadcastManager.getInstance(activity).registerReceiver(notificationReceiver,
                    new IntentFilter(NotificationListenerService.HOME_SCREEN_ACTIVITY_BROADCAST));
            registered = true;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (registered) {
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(notificationReceiver);
            registered = false;
        }
        super.onDetachedFromWindow();
    }
}
