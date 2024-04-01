/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Convenient methods to deal with running JDK.
 */
public final class JkUtilsJdk {

    private JkUtilsJdk() {
        // Can not instantiate
    }

    /**
     * Returns the tool library file of the running JDK.
     */
    public static Path toolsJar() {
        final String jdkLocation = System.getProperty("java.home");
        final Path javaHome = Paths.get(jdkLocation);
        return javaHome.resolve("../lib/tools.jar").normalize().toAbsolutePath();
    }

    public static Path javaHome() {
        final String jdkLocation = System.getProperty("java.home");
        return Paths.get(jdkLocation);
    }

    public static int runningMajorVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        }
        return Integer.parseInt(version);
    }

}
