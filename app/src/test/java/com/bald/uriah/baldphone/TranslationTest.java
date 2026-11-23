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

import org.junit.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * vophone is used in Portuguese: a text missing from values-pt-rBR shows up in English, and a text
 * whose %1$d / %s differ from the English one crashes the screen that shows it.
 */
public class TranslationTest {
    private static final Pattern FORMAT = Pattern.compile("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[sdfxXc]");

    private static Map<String, String> strings(String path) {
        final Map<String, String> strings = new TreeMap<>();
        for (final Element string : TestFiles.elements(TestFiles.xml(path), "string"))
            if (!"false".equals(string.getAttribute("translatable")))
                strings.put(string.getAttribute("name"), string.getTextContent());
        return strings;
    }

    private static List<String> formats(String text) {
        final List<String> formats = new ArrayList<>();
        final Matcher matcher = FORMAT.matcher(text.replace("%%", ""));
        while (matcher.find())
            formats.add(matcher.group());
        Collections.sort(formats);
        return formats;
    }

    @Test
    public void everyTextIsInPortuguese() {
        final Map<String, String> english = strings("src/main/res/values/strings.xml");
        final Map<String, String> portuguese = strings("src/main/res/values-pt-rBR/strings.xml");
        final List<String> missing = new ArrayList<>();
        for (final String name : english.keySet())
            if (!portuguese.containsKey(name))
                missing.add(name);
        assertTrue("texts missing in values-pt-rBR/strings.xml (they would show in English): " + missing, missing.isEmpty());
    }

    @Test
    public void portugueseTextsHaveTheSamePlaceholders() {
        final Map<String, String> english = strings("src/main/res/values/strings.xml");
        final Map<String, String> portuguese = strings("src/main/res/values-pt-rBR/strings.xml");
        for (final Map.Entry<String, String> entry : portuguese.entrySet())
            if (english.containsKey(entry.getKey()))
                assertEquals("placeholders of \"" + entry.getKey() + "\"", formats(english.get(entry.getKey())), formats(entry.getValue()));
    }
}
