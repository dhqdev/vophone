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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * In the vophone ROM, a privileged permission that vophone asks for but that is not listed in
 * rom/vendor/vophone/privapp-permissions-vophone.xml stops the phone from booting
 * (ro.control_privapp_permissions=enforce). This keeps the two lists together.
 */
public class RomPermissionsTest {
    /**
     * Marked "ProtectedPermissions" in the manifest but not privileged (an app op the user allows):
     * they must not be in the privapp list.
     */
    private static final Set<String> PROTECTED_BUT_NOT_PRIVILEGED = new HashSet<>(Arrays.asList(
            "android.permission.WRITE_SETTINGS"
    ));

    private static Set<String> requested(boolean onlyProtected) {
        final Set<String> permissions = new TreeSet<>();
        final Document manifest = TestFiles.xml("src/main/AndroidManifest.xml");
        for (final Element permission : TestFiles.elements(manifest, "uses-permission"))
            if (!onlyProtected || permission.getAttribute("tools:ignore").contains("ProtectedPermissions"))
                permissions.add(permission.getAttribute("android:name"));
        return permissions;
    }

    private static Set<String> listed(String path) {
        final Set<String> permissions = new TreeSet<>();
        for (final Element permission : TestFiles.elements(TestFiles.xml(path), "permission"))
            permissions.add(permission.getAttribute("name"));
        return permissions;
    }

    @Test
    public void everyPrivilegedPermissionIsAllowedInTheRom() {
        final Set<String> privileged = requested(true);
        privileged.removeAll(PROTECTED_BUT_NOT_PRIVILEGED);
        assertEquals("privileged permissions in AndroidManifest.xml vs rom/vendor/vophone/privapp-permissions-vophone.xml",
                privileged, listed("../rom/vendor/vophone/privapp-permissions-vophone.xml"));
    }

    @Test
    public void romOnlyGrantsPermissionsVophoneAsksFor() {
        final Set<String> granted = listed("../rom/vendor/vophone/default-permissions-vophone.xml");
        granted.removeAll(requested(false));
        assertTrue("granted in default-permissions-vophone.xml but not asked for in AndroidManifest.xml: " + granted, granted.isEmpty());
    }
}
