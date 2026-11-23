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
package com.bald.uriah.baldphone;

import com.bald.uriah.baldphone.keyboard.EnglishKeyboard;
import com.bald.uriah.baldphone.keyboard.HebrewKeyboard;
import com.bald.uriah.baldphone.keyboard.NumberKeyboard;
import com.bald.uriah.baldphone.keyboard.PortugueseKeyboard;

import org.junit.Test;
import org.w3c.dom.Element;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * BaldKeyboard gives the n-th key of the layout the n-th code of the keyboard's list. One key too many or
 * too few shifts every letter after it (or crashes the keyboard), so each layout must match its list.
 */
public class KeyboardLayoutTest {
    private static void check(String layout, char[] codes) {
        final List<Element> keys = TestFiles.children(TestFiles.xml("src/main/res/layout/" + layout).getDocumentElement());
        // The first child is a Space that only gives the keyboard its height.
        assertEquals(layout + ": the first view must be the Space", "Space", keys.get(0).getTagName());
        assertEquals(layout + ": number of keys", codes.length, keys.size() - 1);
        for (int i = 0; i < codes.length; i++) {
            final Element key = keys.get(i + 1);
            final String text = key.getAttribute("android:text").replace("\\'", "'");
            if (text.length() == 1)
                assertEquals(layout + ": key " + key.getAttribute("android:id") + " shows a different letter than it types",
                        String.valueOf(codes[i]), text);
        }
    }

    @Test
    public void portuguese() {
        check("pt_keyboard_layout.xml", PortugueseKeyboard.ptKeyboardCodes);
    }

    @Test
    public void portugueseCapitals() {
        assertEquals(PortugueseKeyboard.ptKeyboardCodes.length, PortugueseKeyboard.ptKeyboardCodesCAPS.length);
        for (int i = 0; i < PortugueseKeyboard.ptKeyboardCodes.length; i++)
            assertEquals("Shift on key " + i, Character.toUpperCase(PortugueseKeyboard.ptKeyboardCodes[i]), PortugueseKeyboard.ptKeyboardCodesCAPS[i]);
    }

    @Test
    public void english() {
        check("us_keyboard_layout.xml", EnglishKeyboard.usKeyboardCodes);
        assertEquals(EnglishKeyboard.usKeyboardCodes.length, EnglishKeyboard.usKeyboardCodesCAPS.length);
    }

    @Test
    public void hebrew() {
        check("he_keyboard_layout.xml", HebrewKeyboard.hebrewKeyboardCodes);
    }

    @Test
    public void numbers() {
        check("numbers_keyboard_layout.xml", NumberKeyboard.numbersKeyboardsCodes);
    }
}
