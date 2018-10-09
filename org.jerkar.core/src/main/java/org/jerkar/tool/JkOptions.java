package org.jerkar.tool;


import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Key/value string storage for run parameters. Both keys and values are java
 * {@link String}. Parameters are stored in a static field so they are available
 * to everywhere.<br/>
 * To define these values see <a
 * href="http://jerkar.github.io/documentation/latest/reference.html">Jerkar
 * Reference Guide section 3.3</a>
 */
public final class JkOptions {

    // Default populated instance without needing to invoke #init method.
    private static JkOptions INSTANCE = new JkOptions(readSystemAndUserOptions());

    private final Map<String, String> props = new HashMap<>();

    static synchronized void init(Map<String, String> options) {
        final Map<String, String> map = new HashMap<>();
        map.putAll(options);
        INSTANCE.props.putAll(options);
    }

    private JkOptions(Map<String, String> options) {
        props.putAll(options);
    }

    @SuppressWarnings("unchecked")
    private JkOptions() {
        this(Collections.EMPTY_MAP);
    }

    /**
     * Returns <code>true</code> if a value has been defined for the specified
     * key.
     */
    public static boolean containsKey(String key) {
        return INSTANCE.props.containsKey(key);
    }

    /**
     * Returns the value defined for the specified key.
     */
    public static String get(String key) {
        return INSTANCE.props.get(key);
    }

    /**
     * Returns the complete store.
     */
    public static Map<String, String> getAll() {
        return Collections.unmodifiableMap(INSTANCE.props);
    }

    /**
     * Returns all defined key/values pair where the key start with the
     * specified prefix.
     */
    public static Map<String, String> getAllStartingWith(String prefix) {
        final Map<String, String> result = new HashMap<>();
        for (final String key : INSTANCE.props.keySet()) {
            if (key.startsWith(prefix)) {
                result.put(key, INSTANCE.props.get(key));
            }
        }
        return result;
    }

    /**
     * Set the field values according to the target object according the string
     * found in props arguments.
     */
    static void populateFields(Object target, Map<String, String> props) {
        OptionInjector.inject(target, props);
    }


    public static Map<String, String> toDisplayedMap(Map<String, String> props) {
        final Map<String, String> result = new TreeMap<>();
        for (final Map.Entry<String, String> entry : props.entrySet()) {
            final String value;
            if (JkUtilsString.firstMatching(entry.getKey().toLowerCase(), "password", "pwd") != null
                    && entry.getValue() != null) {
                value = "*****";
            } else {
                value = entry.getValue();
            }
            result.put(entry.getKey(), value);

        }
        return result;
    }

    static Map<String, String> readSystemAndUserOptions() {
        final Path propFile = JkLocator.jerkarHomeDir().resolve("options.properties");
        final Map<String, String> result = new HashMap<>();
        if (Files.exists(propFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
        }
        final Path userPropFile = JkLocator.jerkarUserHomeDir().resolve("options.properties");
        if (Files.exists(userPropFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
        }
        return result;
    }

}
