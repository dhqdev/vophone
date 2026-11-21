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
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.contacts.ContactsActivity;
import com.bald.uriah.baldphone.databases.calls.CallLogsHelper;
import com.bald.uriah.baldphone.utils.Tiles;

/**
 * "Telefone" tile of the home screen: call someone from the contacts, dial a number, or see the last calls.
 */
public class PhoneMenuActivity extends BaldActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_menu);
        Tiles.setup(findViewById(R.id.tile_contacts), R.drawable.human_on_background, Tiles.BLUE, R.string.contacts,
                v -> startActivity(new Intent(this, ContactsActivity.class)));
        Tiles.setup(findViewById(R.id.tile_dialer), R.drawable.glyph_dialpad, Tiles.GREEN, R.string.tile_dial_number,
                v -> startActivity(new Intent(this, DialerActivity.class)));
        Tiles.setup(findViewById(R.id.tile_recent), R.drawable.history_on_background, Tiles.TEAL, R.string.tile_recent_calls,
                v -> startActivity(new Intent(this, RecentActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Red dot on "Últimas ligações" while there is a missed call nobody looked at.
        Tiles.setBadge(findViewById(R.id.tile_recent), !CallLogsHelper.isAllReadSafe(getContentResolver()));
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_NONE;
    }
}
