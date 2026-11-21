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
package com.bald.uriah.baldphone.broadcast_receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.HomeScreenActivity;
import com.bald.uriah.baldphone.utils.BaldToast;

/**
 * Result of installing a vophone update (see utils/GithubUpdater.java).
 * When Android needs the user to confirm, shows its "Update this app?" screen.
 * After the update, reopens vophone's home screen.
 */
public class UpdateInstallReceiver extends BroadcastReceiver {
    public static final String EXTRA_INTERACTIVE = "interactive";
    private static final String TAG = UpdateInstallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            // While vophone was being replaced, Android may have shown its own home screen:
            // bring vophone's home screen back right away.
            try {
                context.startActivity(new Intent(context, HomeScreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Log.e(TAG, "could not reopen the home screen after the update", e);
            }
            return;
        }
        final int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                final Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) {
                    try {
                        context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Exception e) {
                        Log.e(TAG, "could not ask to confirm the update", e);
                    }
                }
                break;
            case PackageInstaller.STATUS_SUCCESS:
                Log.i(TAG, "vophone updated");
                break;
            default:
                Log.e(TAG, "update failed: " + status + " " + intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
                if (intent.getBooleanExtra(EXTRA_INTERACTIVE, false))
                    BaldToast.from(context).setType(BaldToast.TYPE_ERROR).setText(R.string.update_failed).show();
        }
    }
}
