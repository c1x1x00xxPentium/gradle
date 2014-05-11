/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.fixtures.jvm;

import net.rubygrapefruit.platform.SystemInfo;
import net.rubygrapefruit.platform.WindowsRegistry;
import org.gradle.internal.jvm.Jvm;
import org.gradle.internal.nativeplatform.services.NativeServices;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.util.VersionNumber;

import java.io.File;
import java.util.*;

public class InstalledJvmLocator {
    private final OperatingSystem operatingSystem = OperatingSystem.current();
    private final WindowsRegistry windowsRegistry = NativeServices.getInstance().get(WindowsRegistry.class);
    private final SystemInfo systemInfo = NativeServices.getInstance().get(SystemInfo.class);
    private final Jvm currentJvm = Jvm.current();

    /**
     * Discovers JVMs installed on the local machine. Returns the details of each JVM that can be determined efficiently, without running the JVM.
     *
     * @return The JVMs, ordered from highest to lowest Java version. Will include the current JVM.
     */
    public List<JvmInstallation> findJvms() {
        Map<File, JvmInstallation> installs = new HashMap<File, JvmInstallation>();
        Collection<JvmInstallation> jvms;
        if (operatingSystem.isMacOsX()) {
            jvms = new OsXInstalledJvmLocator().findJvms();
        } else if (OperatingSystem.current().isWindows()) {
            jvms = new WindowsOracleJvmLocator(windowsRegistry, systemInfo).findJvms();
        } else if (OperatingSystem.current().isLinux()) {
            jvms = new UbuntuJvmLocator().findJvms();
        } else {
            jvms = Collections.emptySet();
        }
        for (JvmInstallation jvm : jvms) {
            if (!installs.containsKey(jvm.getJavaHome())) {
                installs.put(jvm.getJavaHome(), jvm);
            }
        }
        if (!installs.containsKey(currentJvm.getJavaHome())) {
            installs.put(currentJvm.getJavaHome(), new JvmInstallation(currentJvm.getJavaVersion(), VersionNumber.parse(System.getProperty("java.version")), currentJvm.getJavaHome(), false, JvmInstallation.Arch.Unknown));
        }

        List<JvmInstallation> result = new ArrayList<JvmInstallation>(installs.values());
        Collections.sort(result, new Comparator<JvmInstallation>() {
            public int compare(JvmInstallation o1, JvmInstallation o2) {
                return o2.getVersion().compareTo(o1.getVersion());
            }
        });
        return result;
    }
}
