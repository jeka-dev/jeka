/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class PropertiesHandler {

    // Reads the properties from the baseDir/jeka.properties
    static JkProperties readJekaPropertiesFile(Path baseDir) {
        Path jekaPropertiesFile = baseDir.resolve(JkConstants.PROPERTIES_FILE);
        if (Files.exists(jekaPropertiesFile)) {
            return JkProperties.ofFile(jekaPropertiesFile);
        }
        return JkProperties.EMPTY;
    }

    static JkProperties constructRunbaseProperties(Path baseDir) {
        JkProperties result = JkProperties.ofSysPropsThenEnv()
                    .withFallback(readJekaPropertiesRecursively(JkUtilsPath.relativizeFromWorkingDir(baseDir)));
        Path globalPropertiesFile = JkLocator.getGlobalPropertiesFile();
        if (Files.exists(globalPropertiesFile)) {
            result = result.withFallback(JkProperties.ofFile(globalPropertiesFile));
        }
        return result;
    }

    /**
     * Reads the properties from the baseDir/jeka.properties and its ancestors.
     *
     * Takes also in account properties defined in parent project dirs if any.
     * this doesn't take in account System and global props.
     */
    static JkProperties readJekaPropertiesRecursively(Path baseDir) {
        baseDir = baseDir.toAbsolutePath().normalize();
        JkProperties result = readJekaPropertiesFile(baseDir);
        return readJekaPropertiesRecursively(result, baseDir, true);
    }

    private static JkProperties readJekaPropertiesRecursively(JkProperties props, Path baseDir, boolean origin) {
        if (origin) {
            props = mergeUnderscore(props);
        } else {
            props = withoutUnderscoreProps(props);
        }
        Path parentDir = baseDir.getParent();

        // Stop if parent dir has no jeka.properties file
        if (parentDir != null) {
            Path parentPropsFile = parentDir.resolve(JkConstants.PROPERTIES_FILE);
            if (Files.exists(parentPropsFile)) {
                JkProperties parentProps = JkProperties.ofFile(parentPropsFile);
                JkProperties parentChainProps = readJekaPropertiesRecursively(parentProps, parentDir, false);
                props = resolvePlus(props, parentChainProps);
                props = props.withFallback(parentChainProps);
            }
        }
        return props;
    }

    private static JkProperties mergeUnderscore(JkProperties properties) {
        Map<String, String> result = new HashMap<>();
        properties.getAllStartingWith("", true).forEach((key, value) -> {
            if (!key.startsWith("_")) {
                result.put(key, value);
            }
        });
        properties.getAllStartingWith("_", true).forEach((key, value) -> {
            String sanitizedKey = key.substring(1);
            if (!sanitizedKey.trim().isEmpty()) {
                result.put(sanitizedKey, value);
            }
        });
        return JkProperties.ofMap(properties.getSource(), result);
    }

    private static JkProperties withoutUnderscoreProps(JkProperties properties) {
        Map<String, String> result = new HashMap<>();
        properties.getAllStartingWith("", true).forEach((key, value) -> {
            if (!key.startsWith("_")) {
                result.put(key, value);
            }
        });
        return JkProperties.ofMap(properties.getSource(), result);
    }

    public static JkProperties resolvePlus(JkProperties properties, JkProperties parentProps) {
        Map<String, String> result = new HashMap<>();
        properties.getAllStartingWith("", true).forEach((key, value) -> {
            if (!key.startsWith("+")) {
                result.put(key, value);
            } else {
                String propName = key.substring(1);
                String thisValue = properties.get(key);
                String parentValue = parentProps.get(propName);
                final String resolvedValue;
                if (parentValue == null || parentValue.isEmpty()) {
                    resolvedValue = thisValue;
                } else {
                    resolvedValue = parentValue + " " + thisValue;
                }
                result.put(propName, resolvedValue);
            }
        });
        return JkProperties.ofMap(properties.getSource(), result);
    }
}
