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
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.contacts.AddContactActivity;
import com.bald.uriah.baldphone.adapters.ContactRecyclerViewAdapter;
import com.bald.uriah.baldphone.databases.contacts.Contact;
import com.bald.uriah.baldphone.databases.contacts.MiniContact;
import com.bald.uriah.baldphone.utils.BDB;
import com.bald.uriah.baldphone.utils.BDialog;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.D;

import java.util.ArrayList;
import java.util.List;

import static android.media.AudioManager.STREAM_SYSTEM;

public class DialerActivity extends BaldActivity {
    private static final String TAG = DialerActivity.class.getSimpleName();
    public static final String SORT_ORDER =
            "upper(" + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + ") ASC";
    private final static String[] PROJECTION =
            {ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data._ID, ContactsContract.Contacts.PHOTO_URI, ContactsContract.Data.LOOKUP_KEY, ContactsContract.Data.STARRED};
    private static final String NUMBER_STATE = "NUMBER_STATE";
    private static final int TONE_DURATION = 300 * D.MILLISECOND;
    private static final int TONE_VOLUME = 75; // 0-100

    private ToneGenerator dtmfGenerator;
    private ContentResolver contentResolver;
    private ContactRecyclerViewAdapter contactRecyclerViewAdapter;
    private RecyclerView recyclerView;
    private TextView tv_number;
    private View b_call, b_clear, b_hash, b_sulamit, b_backspace, empty_view;
    private View[] numpad;
    private StringBuilder number = new StringBuilder();
    private boolean playDialSounds;

    private static Uri telUri(final CharSequence number) {
        return Uri.parse(("tel:" + number).replace("#", Uri.encode("#")));
    }

    /**
     * @param phoneAccount the SIM to call from, or null for the phone's default.
     */
    public static void call(final CharSequence number, final Context context, @Nullable PhoneAccountHandle phoneAccount) {
        final Intent intent = new Intent(Intent.ACTION_CALL).setData(telUri(number));
        if (phoneAccount != null)
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccount);
        try {
            context.startActivity(intent);
        } catch (Exception e) { // no permission anymore, or nothing can make calls
            Log.e(TAG, "could not call", e);
            BaldToast.error(context);
        }
    }

    /**
     * The SIMs that can make calls (empty if that cannot be known).
     * Phone accounts are what Android itself uses to pick the SIM, so this works on every Android version
     * (matching SubscriptionInfo.getIccId() does not: since Android 10 it is empty for normal apps).
     */
    @SuppressLint("MissingPermission")
    private static List<PhoneAccountHandle> callCapableSims(final Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M)
            return new ArrayList<>();
        try {
            final TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                final List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
                if (accounts != null)
                    return accounts;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "no permission to list the SIMs", e);
        }
        return new ArrayList<>();
    }

    private static CharSequence simName(final Context context, final PhoneAccountHandle handle, final int index) {
        final TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        final PhoneAccount account = telecomManager == null ? null : telecomManager.getPhoneAccount(handle);
        final CharSequence label = account == null ? null : account.getLabel();
        final CharSequence sim = "SIM " + (index + 1);
        return TextUtils.isEmpty(label) ? sim : sim + " - " + label;
    }

    public static void call(final CharSequence number, final Context context, final boolean directly) {
        if (TextUtils.isEmpty(number)) {
            BaldToast.from(context).setType(BaldToast.TYPE_ERROR).setText(R.string.type_a_number_first).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            BaldToast.error(context);
            return;
        }
        if (!directly && BPrefs.get(context).getBoolean(BPrefs.DUAL_SIM_KEY, BPrefs.DUAL_SIM_DEFAULT_VALUE)) {
            final List<PhoneAccountHandle> sims = callCapableSims(context);
            if (sims.size() > 1) {
                final CharSequence[] simNames = new CharSequence[sims.size()];
                for (int i = 0; i < sims.size(); i++)
                    simNames[i] = simName(context, sims.get(i), i);
                BDB.from(context)
                        .addFlag(BDialog.FLAG_OK | BDialog.FLAG_CANCEL)
                        .setTitle(R.string.choose_sim)
                        .setSubText(R.string.choose_sim_subtext)
                        .setOptions(simNames)
                        .setPositiveButtonListener(params -> {
                            call(number, context, sims.get((Integer) params[0]));
                            return true;
                        }).show();
                return;
            }
        }
        call(number, context, (PhoneAccountHandle) null);
    }

    public static void call(final MiniContact miniContact, final Context context) {
        try {
            call(Contact.fromLookupKey(miniContact.lookupKey, context.getContentResolver()).getPhoneList().get(0).second, context, false);
        } catch (Exception e) {
            BaldToast.error(context);
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;
        setContentView(R.layout.dialer);
        contentResolver = getContentResolver();
        playDialSounds = BPrefs.get(this).getBoolean(BPrefs.DIALER_SOUNDS_KEY, BPrefs.DIALER_SOUNDS_DEFAULT_VALUE) && !testing;
        if (playDialSounds)
            dtmfGenerator = new ToneGenerator(STREAM_SYSTEM, TONE_VOLUME);
        attachXml();
        setOnClickListeners();
        setupYoutube(2);
        searchForContact();
        // "tel:" links, e.g. emergency numbers redirected here by the system when vophone is the default phone app
        final Uri data = getIntent() == null ? null : getIntent().getData();
        if (savedInstanceState == null && data != null && "tel".equals(data.getScheme()) && data.getSchemeSpecificPart() != null)
            setNumber(data.getSchemeSpecificPart());
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(NUMBER_STATE, number);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final CharSequence charSequence = savedInstanceState.getCharSequence(NUMBER_STATE);
        if (charSequence != null) {
            setNumber(charSequence);
        }
    }

    private void getContactsByNumberFilter() {
        final Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(number.toString()));
        final Cursor contactsCursor = contentResolver.query(uri, PROJECTION, null, null, SORT_ORDER);
        if (contactRecyclerViewAdapter != null) {
            contactRecyclerViewAdapter.changeCursor(contactsCursor);
        } else {
            contactRecyclerViewAdapter = new ContactRecyclerViewAdapter(this, contactsCursor, recyclerView, ContactRecyclerViewAdapter.MODE_DEFAULT);
            recyclerView.setAdapter(contactRecyclerViewAdapter);
        }
    }

    private void attachXml() {
        recyclerView = findViewById(R.id.contacts_recycler_view);
        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getDrawable(R.drawable.ll_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);

        tv_number = findViewById(R.id.tv_number);
        numpad = new View[]{
                findViewById(R.id.b_0),
                findViewById(R.id.b_1),
                findViewById(R.id.b_2),
                findViewById(R.id.b_3),
                findViewById(R.id.b_4),
                findViewById(R.id.b_5),
                findViewById(R.id.b_6),
                findViewById(R.id.b_7),
                findViewById(R.id.b_8),
                findViewById(R.id.b_9)
        };
        empty_view = findViewById(R.id.empty_view);
        b_call = findViewById(R.id.b_call);
        b_clear = findViewById(R.id.b_clear);
        b_backspace = findViewById(R.id.b_backspace);
        b_sulamit = findViewById(R.id.b_sulamit);
        b_hash = findViewById(R.id.b_hash);
    }

    private void setOnClickListeners() {
        for (char i = '0'; i <= '9'; i++)
            numpad[i - '0'].setOnClickListener(new DialerClickListener(i, i - '0'));
        b_sulamit.setOnClickListener(new DialerClickListener('*', ToneGenerator.TONE_DTMF_S));
        b_hash.setOnClickListener(new DialerClickListener('#', ToneGenerator.TONE_DTMF_P));

        b_call.setOnClickListener(v -> call(number, this, false));
        b_clear.setOnClickListener(v -> {
            number.setLength(0);
            tv_number.setText(number);
            searchForContact();
        });
        b_backspace.setOnClickListener(v -> {
            number.setLength(number.length() > 1 ? number.length() - 1 : 0);
            tv_number.setText(number);
            searchForContact();
        });
        empty_view.setOnClickListener(v -> {
            startActivity(new Intent(this, AddContactActivity.class)
                    .putExtra(AddContactActivity.CONTACT_NUMBER, (CharSequence) number));
        });

    }

    private void searchForContact() {
        getContactsByNumberFilter();
    }

    public void setNumber(@NonNull CharSequence charSequence) {
        number = new StringBuilder(charSequence);
        tv_number.setText(number);
        searchForContact();

    }

    private class DialerClickListener implements View.OnClickListener {
        private final char c;
        private final int tone;

        DialerClickListener(final char c, final int tone) {
            this.c = c;
            this.tone = tone;
        }

        @Override
        public void onClick(final View v) {
            number.append(c);
            tv_number.setText(number);
            searchForContact();
            if (playDialSounds)
                dtmfGenerator.startTone(tone, TONE_DURATION);
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_READ_CONTACTS | PERMISSION_CALL_PHONE;
    }
}