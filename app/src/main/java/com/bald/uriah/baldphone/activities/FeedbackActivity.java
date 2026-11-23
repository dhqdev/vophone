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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.BuildConfig;
import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BaldToast;

public class FeedbackActivity extends BaldActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        final EditText et_feedback = findViewById(R.id.et_feedback);
        findViewById(R.id.bt_send).setOnClickListener(v -> {
            final CharSequence text = et_feedback.getText();
            if (text.length() == 0)
                BaldToast.from(v.getContext()).setType(BaldToast.TYPE_ERROR).setText(R.string.feedback_cannot_be_empty).show();
            else {
                // vophone: suggestions go to the project page (BaldPhone's e-mail is not read for vophone).
                final Uri page = Uri.parse(SettingsActivity.VOPHONE_PAGE + "/issues/new").buildUpon()
                        .appendQueryParameter("title", getString(R.string.feedback_title))
                        .appendQueryParameter("body", text + "\n\n---\nvophone " + BuildConfig.VERSION_NAME
                                + " / Android " + Build.VERSION.RELEASE + " / " + Build.MANUFACTURER + " " + Build.MODEL)
                        .build();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, page));
                    finish();
                } catch (ActivityNotFoundException e) {
                    BaldToast.from(this)
                            .setText(R.string.no_browser)
                            .setType(BaldToast.TYPE_ERROR)
                            .show();
                }
            }
        });
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_NONE;
    }
}
