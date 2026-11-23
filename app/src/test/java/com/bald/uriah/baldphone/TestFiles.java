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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Reads the project's own files (layouts, strings, manifest, ROM files) in plain JVM tests.
 * Gradle runs unit tests from the app/ folder; the repository root is one level up.
 */
final class TestFiles {
    private TestFiles() {
    }

    /**
     * @param path relative to the app/ folder, for example "src/main/AndroidManifest.xml" or "../rom/..."
     */
    static File file(String path) {
        final File fromApp = new File(path);
        if (fromApp.exists())
            return fromApp;
        final File fromRoot = new File("app", path);
        if (fromRoot.exists())
            return fromRoot;
        throw new AssertionError("file not found: " + path + " (working directory " + new File(".").getAbsolutePath() + ")");
    }

    static Document xml(String path) {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file(path));
        } catch (Exception e) {
            throw new AssertionError("could not read " + path, e);
        }
    }

    static List<Element> children(Element parent) {
        final List<Element> elements = new ArrayList<>();
        final NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE)
                elements.add((Element) nodes.item(i));
        return elements;
    }

    static List<Element> elements(Document document, String tag) {
        final List<Element> elements = new ArrayList<>();
        final NodeList nodes = document.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++)
            elements.add((Element) nodes.item(i));
        return elements;
    }
}
