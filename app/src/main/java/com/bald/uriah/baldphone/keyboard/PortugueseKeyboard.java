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
package com.bald.uriah.baldphone.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.bald.uriah.baldphone.R;

/**
 * vophone: big keyboard for Portuguese (Brazil). The English keys, plus a row of accented letters
 * that is always visible (elderly users do not discover long presses) and ç after l, like Brazilian
 * keyboards. The comma is on the numbers keyboard. Layout: pt_keyboard_layout.xml.
 */
@SuppressLint("ViewConstructor")
public class PortugueseKeyboard extends BaldKeyboard implements BaldKeyboard.Capitalised {
    public static final int LANGUAGE_ID = 3;
    public static final char[] ptKeyboardCodes = new char[]{
            'á', 'â', 'ã', 'à', 'é', 'ê', 'í', 'ó', 'ô', 'õ', 'ú',
            'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
            'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'ç',
            SHIFT, 'z', 'x', 'c', 'v', 'b', 'n', 'm', BACKSPACE,
            NUMBERS, LANGUAGE, SPEECH_TO_TEXT, ' ', HIDE, '.', ENTER,
    };
    public static final char[] ptKeyboardCodesCAPS = new char[]{
            'Á', 'Â', 'Ã', 'À', 'É', 'Ê', 'Í', 'Ó', 'Ô', 'Õ', 'Ú',
            'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
            'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'Ç',
            SHIFT, 'Z', 'X', 'C', 'V', 'B', 'N', 'M', BACKSPACE,
            NUMBERS, LANGUAGE, SPEECH_TO_TEXT, ' ', HIDE, '.', ENTER,
    };

    private boolean caps;

    public PortugueseKeyboard(Context context, OnClickListener onClickListener, Runnable backspace, int imeOptions) {
        super(context, onClickListener, backspace, imeOptions);
    }

    @Override
    public void setCaps() {
        caps = !caps;
        final char[] codes = codes();
        for (int i = 0; i < children.length; i++) {
            if (children[i] == null)
                continue;
            children[i].setTag(codes[i]);
            if (children[i] instanceof TextView)
                ((TextView) children[i]).setText(new char[]{codes[i]}, 0, 1);
        }
    }

    @Override
    protected int layout() {
        return R.layout.pt_keyboard_layout;
    }

    @Override
    int nextLanguage() {
        return EnglishKeyboard.LANGUAGE_ID;
    }

    @Override
    protected char[] codes() {
        return caps ? ptKeyboardCodesCAPS : ptKeyboardCodes;
    }
}
