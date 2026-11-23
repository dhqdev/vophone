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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BDB;
import com.bald.uriah.baldphone.utils.BDialog;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.CallManager;
import com.bald.uriah.baldphone.utils.FirstRunDefaults;
import com.bald.uriah.baldphone.utils.S;

import java.util.List;
import java.util.Locale;

/**
 * Big-button call screen for incoming and ongoing calls.
 * Opened by {@link com.bald.uriah.baldphone.services.BaldInCallService} when vophone is the default phone app.
 */
@TargetApi(Build.VERSION_CODES.M)
public class CallActivity extends AppCompatActivity implements CallManager.Listener {
    private static final String TAG = CallActivity.class.getSimpleName();
    private static final int FINISH_DELAY_MS = 1500;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ImageView iv_photo;
    private static final int[] DTMF_KEY_IDS = {
            R.id.dtmf_0, R.id.dtmf_1, R.id.dtmf_2, R.id.dtmf_3, R.id.dtmf_4, R.id.dtmf_5,
            R.id.dtmf_6, R.id.dtmf_7, R.id.dtmf_8, R.id.dtmf_9, R.id.dtmf_star, R.id.dtmf_hash};
    private static final char[] DTMF_KEYS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '#'};
    private TextView tv_name, tv_number, tv_status, tv_dtmf;
    private View incomingButtons, ongoingButtons, callInfo, callKeypad, callActions;
    private Button bt_speaker, bt_mute, bt_keypad, bt_resume;
    private boolean keypadShown;
    private final StringBuilder dtmfDigits = new StringBuilder();
    @Nullable
    private Call keypadCall;
    @Nullable
    private PowerManager.WakeLock proximityWakeLock;
    @Nullable
    private String shownNumber;
    private boolean finishScheduled;
    @Nullable
    private Call simChooserShownFor;
    @Nullable
    private Call volumeRaisedFor;
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateUi();
            handler.postDelayed(this, 1000);
        }
    };

    public static Intent newIntent(Context context) {
        return new Intent(context, CallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(S.getTheme(this));
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.activity_call);
        iv_photo = findViewById(R.id.iv_photo);
        tv_name = findViewById(R.id.tv_name);
        tv_number = findViewById(R.id.tv_number);
        tv_status = findViewById(R.id.tv_status);
        tv_dtmf = findViewById(R.id.tv_dtmf);
        incomingButtons = findViewById(R.id.incoming_buttons);
        ongoingButtons = findViewById(R.id.ongoing_buttons);
        callInfo = findViewById(R.id.call_info);
        callKeypad = findViewById(R.id.call_keypad);
        callActions = findViewById(R.id.call_actions);
        bt_speaker = findViewById(R.id.bt_speaker);
        bt_mute = findViewById(R.id.bt_mute);
        bt_keypad = findViewById(R.id.bt_keypad);
        bt_resume = findViewById(R.id.bt_resume);

        findViewById(R.id.bt_answer).setOnClickListener(v -> {
            final Call call = CallManager.getPrimaryCall();
            if (call != null)
                CallManager.answer(call);
        });
        final View.OnClickListener hangUp = v -> {
            final Call call = CallManager.getPrimaryCall();
            if (call != null)
                CallManager.hangUp(call);
        };
        findViewById(R.id.bt_decline).setOnClickListener(hangUp);
        findViewById(R.id.bt_hangup).setOnClickListener(hangUp);
        bt_speaker.setOnClickListener(v -> CallManager.setSpeaker(!CallManager.isSpeakerOn()));
        bt_mute.setOnClickListener(v -> CallManager.setMuted(!CallManager.isMuted()));
        bt_keypad.setOnClickListener(v -> showKeypad(!keypadShown));
        bt_resume.setOnClickListener(v -> {
            final Call call = CallManager.getPrimaryCall();
            if (call != null)
                CallManager.resume(call);
        });
        setupKeypad();

        final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "vophone:call");
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallManager.addListener(this);
        CallManager.setCallScreenVisible(true);
        handler.post(ticker);
    }

    @Override
    protected void onStop() {
        CallManager.removeListener(this);
        CallManager.setCallScreenVisible(false);
        handler.removeCallbacks(ticker);
        setProximityScreenOff(false);
        super.onStop();
    }

    @Override
    public void onCallStateChanged() {
        updateUi();
    }

    @Override
    public void onBackPressed() {
        if (keypadShown) {
            showKeypad(false);
            return;
        }
        // Do not leave the call screen by mistake while a call is going on.
        if (!CallManager.hasOngoingCall())
            super.onBackPressed();
    }

    /**
     * The tone plays while the key is held down, like on any phone.
     */
    private void setupKeypad() {
        for (int i = 0; i < DTMF_KEY_IDS.length; i++) {
            final char digit = DTMF_KEYS[i];
            findViewById(DTMF_KEY_IDS[i]).setOnTouchListener((v, event) -> {
                final Call call = CallManager.getPrimaryCall();
                if (call == null)
                    return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        CallManager.startDtmf(call, digit);
                        dtmfDigits.append(digit);
                        tv_dtmf.setText(dtmfDigits);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        CallManager.stopDtmf(call);
                        break;
                }
                return false; // keep the pressed look and the click sound
            });
        }
    }

    private void showKeypad(boolean show) {
        keypadShown = show;
        callKeypad.setVisibility(show ? View.VISIBLE : View.GONE);
        callInfo.setVisibility(show ? View.GONE : View.VISIBLE);
        bt_keypad.setText(show ? R.string.call_keypad_close : R.string.call_keypad_open);
    }

    private void updateUi() {
        final Call call = CallManager.getPrimaryCall();
        if (call == null) {
            scheduleFinish(0);
            return;
        }
        bindCaller(call);
        if (keypadCall != call) { // a different call: start with an empty keypad
            keypadCall = call;
            dtmfDigits.setLength(0);
            tv_dtmf.setText(dtmfDigits);
        }
        final int state = CallManager.getState(call);
        final boolean muted = CallManager.isMuted();
        boolean keepScreenOn = false;
        switch (state) {
            case Call.STATE_RINGING:
                tv_status.setText(R.string.incoming_call);
                showButtons(true, false);
                setProximityScreenOff(false);
                keepScreenOn = true;
                break;
            case Call.STATE_ACTIVE:
                if (volumeRaisedFor != call) {
                    volumeRaisedFor = call;
                    FirstRunDefaults.setCallVolumeToMax(this);
                }
                final long connectTime = call.getDetails().getConnectTimeMillis();
                final String status = connectTime > 0
                        ? getString(R.string.in_call) + "  " + formatDuration(System.currentTimeMillis() - connectTime)
                        : getString(R.string.in_call);
                // Muted by mistake, the user would think the call is broken: say it clearly.
                tv_status.setText(muted ? status + "\n" + getString(R.string.muted_hint) : status);
                showButtons(false, true);
                setProximityScreenOff(true);
                break;
            case Call.STATE_HOLDING:
                tv_status.setText(R.string.on_hold);
                showButtons(false, true);
                // Screen on: the user has to see and press "back to the call"
                setProximityScreenOff(false);
                break;
            case Call.STATE_SELECT_PHONE_ACCOUNT:
                // dual SIM without a default SIM: the phone app has to ask which SIM to use
                tv_status.setText(R.string.choose_sim);
                showButtons(false, true);
                askForSim(call);
                break;
            case Call.STATE_DISCONNECTING:
            case Call.STATE_DISCONNECTED:
                tv_status.setText(R.string.call_ended);
                showButtons(false, false);
                setProximityScreenOff(false);
                scheduleFinish(FINISH_DELAY_MS);
                break;
            default: // new, dialing, connecting...
                tv_status.setText(R.string.call_status_calling);
                showButtons(false, true);
                setProximityScreenOff(true);
                break;
        }
        if (keepScreenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // On hold: only "back to the call" and "hang up". The keypad only works in an answered call.
        final boolean holding = state == Call.STATE_HOLDING;
        bt_resume.setVisibility(holding ? View.VISIBLE : View.GONE);
        callActions.setVisibility(holding ? View.GONE : View.VISIBLE);
        bt_keypad.setEnabled(state == Call.STATE_ACTIVE);
        bt_keypad.setAlpha(state == Call.STATE_ACTIVE ? 1f : 0.5f);
        if (keypadShown && state != Call.STATE_ACTIVE)
            showKeypad(false);

        final boolean speakerOn = CallManager.isSpeakerOn();
        bt_speaker.setText(speakerOn ? R.string.speaker_turn_off : R.string.speaker_turn_on);
        bt_speaker.setBackgroundResource(speakerOn ? R.drawable.call_button_blue : R.drawable.call_button_gray);
        bt_mute.setText(muted ? R.string.mute_turn_off : R.string.mute_turn_on);
        bt_mute.setBackgroundResource(muted ? R.drawable.call_button_blue : R.drawable.call_button_gray);
    }

    @SuppressWarnings("deprecation")
    private void askForSim(Call call) {
        if (simChooserShownFor == call)
            return;
        simChooserShownFor = call;
        final TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
        List<PhoneAccountHandle> handles = null;
        final Bundle extras = call.getDetails().getIntentExtras();
        if (extras != null)
            handles = extras.getParcelableArrayList(Call.AVAILABLE_PHONE_ACCOUNTS);
        if ((handles == null || handles.isEmpty()) && telecomManager != null) {
            try {
                handles = telecomManager.getCallCapablePhoneAccounts();
            } catch (SecurityException e) {
                Log.e(TAG, "no permission to list SIMs", e);
            }
        }
        if (handles == null || handles.isEmpty()) {
            BaldToast.error(this);
            call.disconnect();
            return;
        }
        if (handles.size() == 1) {
            call.phoneAccountSelected(handles.get(0), false);
            return;
        }
        final List<PhoneAccountHandle> finalHandles = handles;
        final CharSequence[] names = new CharSequence[handles.size()];
        for (int i = 0; i < handles.size(); i++) {
            final PhoneAccount account = telecomManager == null ? null : telecomManager.getPhoneAccount(handles.get(i));
            names[i] = account != null && !TextUtils.isEmpty(account.getLabel()) ? account.getLabel() : "SIM " + (i + 1);
        }
        BDB.from(this)
                .setTitle(R.string.choose_sim)
                .setSubText(R.string.choose_sim_subtext)
                .setOptions(names)
                .addFlag(BDialog.FLAG_OK | BDialog.FLAG_CANCEL | BDialog.FLAG_NOT_CANCELABLE)
                .setPositiveButtonListener(params -> {
                    call.phoneAccountSelected(finalHandles.get((Integer) params[0]), false);
                    return true;
                })
                .setNegativeButtonListener(params -> {
                    call.disconnect();
                    return true;
                })
                .show();
    }

    private void showButtons(boolean incoming, boolean ongoing) {
        incomingButtons.setVisibility(incoming ? View.VISIBLE : View.GONE);
        ongoingButtons.setVisibility(ongoing ? View.VISIBLE : View.GONE);
    }

    private void scheduleFinish(long delayMs) {
        if (finishScheduled)
            return;
        finishScheduled = true;
        handler.postDelayed(() -> {
            // a new call may have arrived in the meantime
            if (CallManager.hasOngoingCall()) {
                finishScheduled = false;
                updateUi();
            } else {
                finish();
            }
        }, delayMs);
    }

    private void bindCaller(Call call) {
        final Uri handle = call.getDetails().getHandle();
        final String number = handle == null ? "" : handle.getSchemeSpecificPart();
        if (number.equals(shownNumber))
            return;
        shownNumber = number;

        String name = null, photo = null;
        if (!number.isEmpty() && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            final Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            try (Cursor cursor = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(0);
                    photo = cursor.getString(1);
                }
            } catch (Exception e) {
                Log.e(TAG, "contact lookup failed", e);
            }
        }
        if (TextUtils.isEmpty(name))
            name = call.getDetails().getCallerDisplayName();

        if (TextUtils.isEmpty(name)) {
            tv_name.setText(number.isEmpty() ? getString(R.string.unknown_number) : number);
            tv_number.setVisibility(View.GONE);
        } else {
            tv_name.setText(name);
            tv_number.setText(number);
            tv_number.setVisibility(number.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (photo != null)
            iv_photo.setImageURI(Uri.parse(photo));
        else
            iv_photo.setImageResource(R.drawable.face_on_button);
    }

    private void setProximityScreenOff(boolean on) {
        if (proximityWakeLock == null)
            return;
        if (on && !proximityWakeLock.isHeld())
            proximityWakeLock.acquire();
        else if (!on && proximityWakeLock.isHeld())
            proximityWakeLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
    }

    private static String formatDuration(long millis) {
        final long seconds = Math.max(0, millis / 1000);
        final long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h > 0
                ? String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
                : String.format(Locale.getDefault(), "%d:%02d", m, s);
    }
}
