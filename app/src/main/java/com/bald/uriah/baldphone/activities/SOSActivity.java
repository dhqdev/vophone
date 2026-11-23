/*
 * Copyright 2019 Uriah Shaul Mandel
 *
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

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.contacts.ContactsActivity;
import com.bald.uriah.baldphone.adapters.ContactRecyclerViewAdapter;
import com.bald.uriah.baldphone.databases.contacts.Contact;
import com.bald.uriah.baldphone.databases.contacts.MiniContact;
import com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.views.BaldLinearLayoutButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.bald.uriah.baldphone.databases.home_screen_pins.HomeScreenPinHelper.SHARED_PREFS_KEY;

/**
 * SOS screen: the two emergency contacts and the emergency number.
 * It opens without asking for any permission first: in an emergency the big button must work at once.
 * If calling was never allowed, pressing a button asks for that one permission and then calls.
 */
public class SOSActivity extends BaldActivity {
    private static final String TAG = SOSActivity.class.getSimpleName();
    private static final int MAX_PINNED_CONTACTS = 2;
    private static final int REQUEST_CALL_PERMISSION = 7192;
    @Nullable
    private String pendingNumber;
    /**
     * Emergency numbers the user can choose from in the settings (Brazil first, 112 as fallback).
     * Must stay in the same order as {@link #EMERGENCY_NUMBER_LABELS}.
     */
    public static final String[] EMERGENCY_NUMBERS = {"192", "193", "190", "112"};
    @StringRes
    public static final int[] EMERGENCY_NUMBER_LABELS = {
            R.string.emergency_samu,
            R.string.emergency_firefighters,
            R.string.emergency_police,
            R.string.emergency_international
    };
    /**
     * Short versions of {@link #EMERGENCY_NUMBER_LABELS} for the big button on the SOS screen.
     */
    @StringRes
    private static final int[] EMERGENCY_NUMBER_SHORT_LABELS = {
            R.string.emergency_samu_short,
            R.string.emergency_firefighters_short,
            R.string.emergency_police_short,
            R.string.emergency_international_short
    };
    private BaldLinearLayoutButton[] ec;
    private BaldLinearLayoutButton ecReal;

    private void setupEC(BaldLinearLayoutButton baldLinearLayoutButton, MiniContact miniContact) {
        if (miniContact.photo != null)
            ((ImageView) baldLinearLayoutButton.getChildAt(0)).setImageURI(Uri.parse(miniContact.photo));
        else
            ((ImageView) baldLinearLayoutButton.getChildAt(0)).setImageResource(R.drawable.face_on_button);

        ((TextView) baldLinearLayoutButton.getChildAt(1)).setText(miniContact.name);
        baldLinearLayoutButton.setOnClickListener(v -> {
            try {
                callNow(Contact.fromLookupKey(miniContact.lookupKey, getContentResolver()).getPhoneList().get(0).second.toString());
            } catch (Exception e) { // the contact has no number anymore
                Log.e(TAG, "could not get the number of " + miniContact.name, e);
                BaldToast.error(this);
            }
        });
    }

    /**
     * Calls right away (no SIM question); asks for the call permission first if it was never given.
     */
    private void callNow(String number) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            DialerActivity.call(number, this, true);
        } else {
            pendingNumber = number;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CALL_PERMISSION || pendingNumber == null)
            return;
        final String number = pendingNumber;
        pendingNumber = null;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            DialerActivity.call(number, this, true);
        else
            dialWithAnotherApp(number);
    }

    /**
     * Calling is not allowed for vophone: open another phone app with the number typed in, so one tap calls.
     */
    private void dialWithAnotherApp(String number) {
        final Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        for (final ResolveInfo resolveInfo : getPackageManager().queryIntentActivities(dial, PackageManager.MATCH_DEFAULT_ONLY)) {
            if (resolveInfo.activityInfo != null && !getPackageName().equals(resolveInfo.activityInfo.packageName)) {
                try {
                    startActivity(dial.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "could not open " + resolveInfo.activityInfo.packageName, e);
                }
            }
        }
        BaldToast.error(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;
        setContentView(R.layout.activity_sos);
        ec = new BaldLinearLayoutButton[]{findViewById(R.id.ec1), findViewById(R.id.ec2)};
        ecReal = findViewById(R.id.ec_real);
        setupYoutube(4);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final View.OnClickListener addContactListener = v -> startActivity(new Intent(v.getContext().getApplicationContext(), ContactsActivity.class).putExtra(ContactsActivity.INTENT_EXTRA_CONTACT_ADAPTER_MODE, ContactRecyclerViewAdapter.MODE_SOS));
        // Without the contacts permission the two buttons stay "Add", which asks for it.
        final List<MiniContact> miniContacts =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        ? PinHelper.getAllPinnedContacts(this) : null;
        final int size = miniContacts == null ? 0 : miniContacts.size();
        for (int i = 0; i < MAX_PINNED_CONTACTS; i++) {
            if (size > i)
                setupEC(ec[i], miniContacts.get(i));
            else
                ec[i].setOnClickListener(addContactListener);
        }

        final String emergencyNumber = getEmergencyNumber(this);
        ((TextView) findViewById(R.id.tv_ec_real)).setText(getEmergencyLabel(this, emergencyNumber));
        ecReal.setOnClickListener((v) -> callNow(emergencyNumber));
    }

    public static String getEmergencyNumber(Context context) {
        final String number = BPrefs.get(context).getString(BPrefs.EMERGENCY_NUMBER_KEY, BPrefs.EMERGENCY_NUMBER_DEFAULT_VALUE);
        return emergencyNumberIndex(number) == -1 ? BPrefs.EMERGENCY_NUMBER_DEFAULT_VALUE : number;
    }

    public static int emergencyNumberIndex(String number) {
        for (int i = 0; i < EMERGENCY_NUMBERS.length; i++)
            if (EMERGENCY_NUMBERS[i].equals(number))
                return i;
        return -1;
    }

    private static CharSequence getEmergencyLabel(Context context, String number) {
        final int index = emergencyNumberIndex(number);
        return index == -1 ? context.getString(R.string.emergency_services) : context.getString(EMERGENCY_NUMBER_SHORT_LABELS[index]);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.slide_out_up);
    }

    public static class PinHelper {
        /**
         * @return true if succeeded
         */
        public static boolean pinContact(Context context, String lookupKey) {
            final SharedPreferences sharedPreferences = context.getSharedPreferences(HomeScreenPinHelper.PinnedContactPreferences.KEY, Context.MODE_PRIVATE);
            final Set<String> befSet = sharedPreferences.getStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, null);
            final Set<String> newSet;
            if (befSet == null)
                newSet = new HashSet<>();
            else {
                if (befSet.size() >= SOSActivity.MAX_PINNED_CONTACTS)
                    return false;
                else
                    newSet = new HashSet<>(befSet);
            }
            newSet.add(lookupKey);
            sharedPreferences.edit().putStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, newSet).apply();
            return true;
        }

        public static boolean isPinned(Context context, String lookupKey) {
            Set<String> set = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE).getStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, null);
            return set != null && set.contains(lookupKey);
        }

        public static void removeContact(Context context, String lookupKey) {
            final SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE);
            final Set<String> befSet = sharedPreferences.getStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, null);
            final Set<String> newSet;
            if (befSet == null)
                newSet = new HashSet<>();
            else
                newSet = new HashSet<>(befSet);
            newSet.remove(lookupKey);
            sharedPreferences.edit().putStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, newSet).apply();
        }

        /**
         * @param context
         * @return sorted list of lookup keys
         */
        private static List<MiniContact> getAllPinnedContacts(Context context) {
            final Set<String> lookupKeys = context.getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE).getStringSet(HomeScreenPinHelper.PinnedContactPreferences.SOS_KEY, null);
            if (lookupKeys == null)
                return null;
            final List<MiniContact> ret = new ArrayList<>(lookupKeys.size());
            final ContentResolver contentResolver = context.getContentResolver();
            for (String lookupKey :
                    lookupKeys) {
                try (Cursor cursor = contentResolver.query(
                        ContactsContract.Contacts.CONTENT_URI,
                        MiniContact.PROJECTION,
                        ContactsContract.Data.LOOKUP_KEY + " = ?",
                        new String[]{
                                lookupKey
                        }, null)) {
                    if (cursor.moveToFirst()) {
                        ret.add(new MiniContact(
                                lookupKey,
                                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
                                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)),
                                cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID)),
                                cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.STARRED)) == 1
                        ));
                    } else {
                        removeContact(context, lookupKey);
                    }

                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
            Collections.sort(ret, (o1, o2) -> o1.name.compareTo(o2.name));
            return ret;
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_NONE; // see the class comment
    }
}