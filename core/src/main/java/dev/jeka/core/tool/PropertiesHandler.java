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
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class PropertiesHandler {

    static JkProperties constructRunbaseProperties(Path baseDir) {
        JkProperties result = JkProperties.ofSysPropsThenEnv()
                    .withFallback(readJekaPropertiesRecursively(JkUtilsPath.relativizeFromWorkingDir(baseDir)));
        return result.withFallback(globalProperties());
    }

    /**
     * Returns the properties used to interpret the command line and to set up engines and runbases.
     * Admin KBeans are agnostic to the current dir, so jeka.properties files are ignored for them.
     */
    static JkProperties constructProperties(Path baseDir, boolean adminMode) {
        return adminMode ? JkProperties.ofStandardProperties() : constructRunbaseProperties(baseDir);
    }

    /**
     * Reads the properties from the baseDir/jeka.properties and its ancestors.
     * <p>
     * Takes also in account properties defined in parent project dirs if any.
     * The result does not include System, environment and global props, but values can reference them using '${xxx}'.
     * <p>
     * Each file is resolved against its own parent chain, so that a child can't alter references declared in a parent.
     */
    static JkProperties readJekaPropertiesRecursively(Path baseDir) {
        return readJekaPropertiesRecursively(baseDir.toAbsolutePath().normalize(), true);
    }

    private static JkProperties readJekaPropertiesRecursively(Path baseDir, boolean origin) {
        Path file = baseDir.resolve(JkConstants.PROPERTIES_FILE);
        Map<String, String> rawProps = Files.exists(file) ? readRawProps(file) : new HashMap<>();
        rawProps = origin ? mergeUnderscore(rawProps) : withoutUnderscoreProps(rawProps);

        // Stop if parent dir has no jeka.properties file
        JkProperties parentProps = JkProperties.EMPTY;
        Path parentDir = baseDir.getParent();
        if (parentDir != null && Files.exists(parentDir.resolve(JkConstants.PROPERTIES_FILE))) {
            parentProps = readJekaPropertiesRecursively(parentDir, false);
            rawProps = resolvePlus(rawProps, parentProps);
        }
        return interpolate(file, rawProps, parentProps).withFallback(parentProps);
    }

    private static JkProperties interpolate(Path file, Map<String, String> rawProps, JkProperties parentProps) {
        if (rawProps.isEmpty()) {
            return JkProperties.EMPTY;
        }
        String source = JkProperties.ofFile(file).getSource();
        JkProperties context = JkProperties.ofSysPropsThenEnv()
                .withFallback(JkProperties.ofMap(source, rawProps))
                .withFallback(parentProps)
                .withFallback(globalProperties());
        Map<String, String> result = new HashMap<>();
        rawProps.forEach((key, value) -> result.put(key, JkUtilsString.interpolate(value, token -> {
            String tokenValue = context.get(token);
            if (tokenValue == null) {
                throw new IllegalArgumentException(String.format(
                        "Property '%s' references '${%s}' which is not defined (from %s).", key, token, source));
            }
            return tokenValue;
        })));
        return JkProperties.ofMap(source, result);
    }

    private static JkProperties globalProperties() {
        Path globalPropertiesFile = JkLocator.getGlobalPropertiesFile();
        return Files.exists(globalPropertiesFile) ? JkProperties.ofFile(globalPropertiesFile) : JkProperties.EMPTY;
    }

    private static Map<String, String> readRawProps(Path file) {
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(file)) {
            properties.load(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> result = new HashMap<>();
        properties.stringPropertyNames().forEach(name -> result.put(name, properties.getProperty(name)));
        return result;
    }

    private static Map<String, String> mergeUnderscore(Map<String, String> props) {
        Map<String, String> result = withoutUnderscoreProps(props);
        props.forEach((key, value) -> {
            if (key.startsWith("_") && !key.substring(1).trim().isEmpty()) {
                result.put(key.substring(1), value);
            }
        });
        return result;
    }

    private static Map<String, String> withoutUnderscoreProps(Map<String, String> props) {
        Map<String, String> result = new HashMap<>();
        props.forEach((key, value) -> {
            if (!key.startsWith("_")) {
                result.put(key, value);
            }
        });
        return result;
    }

    private static Map<String, String> resolvePlus(Map<String, String> props, JkProperties parentProps) {
        Map<String, String> result = new HashMap<>();
        props.forEach((key, value) -> {
            if (!key.startsWith("+")) {
                result.putIfAbsent(key, value);
            } else {
                String propName = key.substring(1);
                String parentValue = parentProps.get(propName);
                result.put(propName, JkUtilsString.isBlank(parentValue) ? value : parentValue + " " + value);
            }
        });
        return result;
    }
}
