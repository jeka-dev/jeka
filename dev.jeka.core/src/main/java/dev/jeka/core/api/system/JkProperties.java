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

import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.text.JkColumnText;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A set of name-value pairs. Names and values are typed as <code>String</code>.<p>
 *
 * This object provides interpolation and fallback features.
 * <p>
 *     Part of the value can be expressed by referencing the name of another property. For example, if we have
 *     <pre>
 *         foo=bar
 *         foo2=${foo}${foo}
 *     </pre>
 *     then <code>foo2=barbar</code>
 *
 * <p>
 *     <code>JkProperties</code> objects can be combined to form a chain of resolution using a fallback mechanism.
 */
public final class JkProperties {

    /**
     * A predicate used to determine if a given string is a sensitive key.
     * A sensitive key is determined by its ending, which could be "password", "secret", "token", or "pwd".
     * Case-insensitive comparison is used.
     */
    public static final Predicate<String> SENSITIVE_KEY_PATTERN = key ->
        key.toLowerCase().endsWith("password")
                || key.toLowerCase().endsWith("secret")
                || key.toLowerCase().endsWith("token")
                || key.toLowerCase().endsWith("pwd");

    /**
     * Environment variables exposed as JkProperties.<p>
     * Each environment variable is exposed with 2 names : <ul>
     *     <li>Its original name as it is known by the OS (e.g MY_ENV_VAR)</li>
     *     <li>Its dot-lowercase name counterpart (e.g. my.env.var)</li>
     * </ul>
     */
    public static final JkProperties ENVIRONMENT_VARIABLES = ofEnvironmentVariables();

    private static final String ENV_VARS_NAME = "Environment Variables";

    private static final String SYS_PROPS_NAME = "System Properties";

    public static final JkProperties EMPTY = JkProperties.ofMap("", Collections.emptyMap());


    private final String source;

    private final Map<String, String> props;

    private final JkProperties fallback;

    private JkProperties(String source, Map<String, String> props, JkProperties fallback) {
        this.source = source;
        this.props = props;
        this.fallback = fallback;
    }

    public static JkProperties ofMap(String sourceName, Map<String, String> props) {
        return new JkProperties(sourceName, Collections.unmodifiableMap(new HashMap<>(props)), null);
    }

    public static JkProperties ofMap(Map<String, String> props) {
        return ofMap("map", props);
    }

    public static JkProperties ofSysPropsThenEnv() {
        return ofSystemProperties().withFallback(ENVIRONMENT_VARIABLES);
    }

    /**
     * Returns an instance of JkProperties from system properties and environment variables,
     * with fallback to global.properties if they exist.
     */
    public static JkProperties ofSysPropsThenEnvThenGlobalProperties() {
        Path globalPropsFile = JkLocator.getGlobalPropertiesFile();
        if (Files.exists(globalPropsFile)) {
            return ofSysPropsThenEnv().withFallback(ofFile(globalPropsFile));
        }
        return ofSysPropsThenEnv();
    }

    /**
     * Returns an instance of JkProperties by loading properties from the specified file.
     */
    public static JkProperties ofFile(Path propertyFile) {
        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(propertyFile.toFile())) {
            properties.load(is);
            Path workingDir = Paths.get("").toAbsolutePath();
            String source = propertyFile.toString();
            if (propertyFile.isAbsolute() && propertyFile.startsWith(workingDir)) {
                source = workingDir.relativize(propertyFile).toString();
            }
            return ofMap(source, JkUtilsIterable.propertiesToMap(properties));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(propertyFile + " not found");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a JkProperties instance from this one plus the specified fallback.
     */
    public JkProperties withFallback(JkProperties fallback) {
        if (fallback == null || fallback == EMPTY) {
            return this;
        }
        if (this.fallback == null) {
            return new JkProperties(this.source, this.props, fallback);
        }
        return new JkProperties(this.source, this.props, this.fallback.withFallback(fallback));
    }

    /**
     * Returns the value associated with the specified property name or <code>null</code> if no value has been
     * specified for this name.
     * <p>
     * See <a href="https://google.github.io/styleguide/shellguide.html#s7.3-constants-and-environment-variable-names">Environment variable conventions</a>
     */
    public String get(String propertyName) {
        String rawValue = getRawValue(propertyName);
        if (rawValue == null) {
            return null;
        }
        return interpolate(rawValue);
    }

    /**
     * Returns the value associated with the specified property name or the defaultValue if no value has been
     * specified for this name.
     */
    public String get(String propertyName, String defaultValue) {
        return Optional.ofNullable(get(propertyName)).orElse(defaultValue);
    }

    /**
     * Checks if the given property name exists in the properties.
     */
    public boolean containsKey(String propName) {
        boolean result = props.containsKey(propName);
        if (result) {
            return result;
        }
        if (fallback != null) {
            return fallback.containsKey(propName);
        }
        return false;
    }

    private String getRawValue(String propName) {
        String result =  props.get(propName);
        if (result != null) {
            return result;
        }
        if (fallback != null) {
            return fallback.getRawValue(propName);
        }
        return null;
    }

    private static JkProperties ofEnvironmentVariables() {
        Map<String, String> props = new HashMap<>();
        for (String varName : System.getenv().keySet() ) {
            String value = System.getenv(varName);
            props.put(varName, value);
            props.put(lowerCase(varName), value);
        }
        return new JkProperties(ENV_VARS_NAME, Collections.unmodifiableMap(props), null);
    }

    // The system properties are likely to change during the run, so we cannot cache it.
    private static JkProperties ofSystemProperties() {
        Map<String, String> props = new HashMap<>();
        for (String propName : System.getProperties().stringPropertyNames() ) {
            props.put(propName, System.getProperty(propName));
        }
        return new JkProperties(SYS_PROPS_NAME, Collections.unmodifiableMap(props), null);
    }

    private String interpolate(String string) {
        return JkUtilsString.interpolate(string, this::get);
    }

    private static String lowerCase(String value) {
        return value.toLowerCase().replace('_', '.');
    }

    public Map<String,String> getAllStartingWith(String prefix, boolean keepPrefix) {
        Map<String, String> result = new HashMap<>();
        for (String key : find(prefix)) {
            String resultKey = keepPrefix ? key : JkUtilsString.substringAfterFirst(key, prefix);
            result.put(resultKey, get(key));
        }
        return result;
    }

    /**
     * Returns all keys with the specified prefix. If prefix is <code>null</code> or empty,
     * then returns all keys.
     */
    public Set<String> find(String prefix) {
        Set<String> result = new HashSet<>();
        if (prefix == null || prefix.isEmpty()) {
            result = new LinkedHashSet<>(props.keySet());
        } else {
            result.addAll(props.keySet().stream()
                    .filter(name -> name.startsWith(prefix))
                    .collect(Collectors.toSet())
            );
        }
        if (fallback != null) {
            result.addAll(fallback.find(prefix));
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("== " + source + " ==").append("\n");
        props.forEach( (key, val) -> result.append("  " + key +  "=" + val + "\n"));
        if (fallback != null) {
            result.append(fallback);
        }
        return result.toString();
    }

    /**
     * Converts the properties to a formatted column text representation.
     *
     * @param keyMaxLength The maximum length of the key column.
     * @param valueMaxLength The maximum length of the value column.
     * @param canTruncate Flag indicating whether the values can be truncated if they exceed the maximum length.
     */
    public JkColumnText toColumnText(int keyMaxLength, int valueMaxLength, boolean canTruncate) {
        Set<String> keys = find("");
        JkProperties systemLess = systemLess();
        if (systemLess == null) {
            return JkColumnText.ofSingle(valueMaxLength, valueMaxLength);
        }
        JkColumnText columnsText = JkColumnText
                .ofSingle(1, keyMaxLength)
                .addColumn(1, valueMaxLength)
                .addColumn(5, 60);
        for (String key : keys) {
            if (systemLess.get(key) == null) {
                continue;
            }
            String value = get(key);
            if (SENSITIVE_KEY_PATTERN.test(key)) {
                value = "***";
            }
            String source = getSourceDefining(key).source;
            if (source.endsWith("global.properties")) {
                source = "global.properties";
            } else {
                Path sourcePath = Paths.get(source);
                if (sourcePath.isAbsolute()) {
                    Path relPath = Paths.get(".").toAbsolutePath().relativize(sourcePath);
                    source = relPath.toString();
                }

            }
            if (canTruncate && value != null && value.length() > valueMaxLength) {
                value = JkUtilsString.ellipse(value, valueMaxLength-3);
            }
            if (canTruncate && key.length() > keyMaxLength) {
                key = JkUtilsString.ellipse(key, keyMaxLength-3);
            }
            columnsText.add(key, value, source);
        }
        return columnsText;
    }

    private JkProperties getSourceDefining(String key) {
        if (this.props.containsKey(key)) {
            return this;
        }
        if (this.fallback != null) {
            return fallback.getSourceDefining(key);
        }
        return null;
    }

    private JkProperties systemLess() {
        if (this.isSystem()) {
            return fallback == null ? null : fallback.systemLess();
        }
        return fallback == null ? this : new JkProperties(source, props, fallback.systemLess());
    }

    private boolean isSystem() {
        return SYS_PROPS_NAME == source || ENV_VARS_NAME == source;
    }
}
