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

package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsString;

import java.io.File;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Provides information about Jeka running instance.
 */
public final class JkInfo {

    private static String version;

    private static boolean logIvyVerboseMessages;

    public static final String JEKA_MODULE_ID = "dev.jeka:jeka-core";

    public JkInfo() {
    }

    /**
     * Returns the current Jeka version.
     */
    public static String getJekaVersion() {
        if (JkUtilsString.isBlank(version)) {
            final Class<?> clazz = JkInfo.class;
            final String className = clazz.getSimpleName() + ".class";
            final String classPath = clazz.getResource(className).toString();
            if (!classPath.startsWith("jar")) {
                // Class not from JAR
                final String relativePath = clazz.getName().replace('.', File.separatorChar)
                        + ".class";
                final String classFolder = classPath.substring(0,
                        classPath.length() - relativePath.length() - 1);
                final String manifestPath = classFolder + "/META-INF/MANIFEST.MF";
                version = readVersionFrom(manifestPath);
            } else {
                final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
                        + "/META-INF/MANIFEST.MF";
                version = readVersionFrom(manifestPath);
            }
        }
        return version;
    }

    public static String getVersion() {
        return version;
    }

    public static void setVersion(String version) {
        JkInfo.version = version;
    }

    public static boolean isLogIvyVerboseMessages() {
        return logIvyVerboseMessages;
    }

    public static void setLogIvyVerboseMessages(boolean logIvyVerboseMessages) {
        JkInfo.logIvyVerboseMessages = logIvyVerboseMessages;
    }

    private static String readVersionFrom(String manifestPath) {
        Manifest manifest;
        try {
            manifest = new Manifest(new URL(manifestPath).openStream());
            final Attributes attrs = manifest.getMainAttributes();
            return attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
