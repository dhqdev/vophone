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

import com.bald.uriah.baldphone.utils.GithubUpdater;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Phones only install a release whose number is bigger than theirs (utils/GithubUpdater.java).
 * A wrong number here would stop every phone from updating.
 */
public class VersionCodeTest {
    @Test
    public void tagsBecomeTheBuildGradleVersionCode() {
        // Same formula as app/build.gradle and release.yml: 1000000 + major*10000 + minor*100 + patch.
        assertEquals(1010002, GithubUpdater.versionCodeOf("v1.0.2"));
        assertEquals(1010203, GithubUpdater.versionCodeOf("1.2.3"));
        assertEquals(1020000, GithubUpdater.versionCodeOf(" v2.0.0 "));
    }

    @Test
    public void everyNextVersionIsBigger() {
        assertTrue(GithubUpdater.versionCodeOf("v1.0.99") < GithubUpdater.versionCodeOf("v1.1.0"));
        assertTrue(GithubUpdater.versionCodeOf("v1.99.99") < GithubUpdater.versionCodeOf("v2.0.0"));
        // Local builds use versionCode 103: every release must be newer.
        assertTrue(GithubUpdater.versionCodeOf("v0.0.0") > 103);
    }

    @Test
    public void badTagsAreIgnored() {
        assertEquals(-1, GithubUpdater.versionCodeOf("beta"));
        assertEquals(-1, GithubUpdater.versionCodeOf("v1.2"));
        assertEquals(-1, GithubUpdater.versionCodeOf("v1.100.0"));
        assertEquals(-1, GithubUpdater.versionCodeOf("v1.0.100"));
        assertEquals(-1, GithubUpdater.versionCodeOf("v1.0.0-rc1"));
    }
}
