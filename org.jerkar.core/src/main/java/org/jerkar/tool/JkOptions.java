package org.jerkar.tool;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Key/value string storage for build parameters. Both keys and values are java
 * {@link String}. Parameters are stored in a static field so they are available
 * to everywhere. These parameters are consumed by the build definitions but
 * not set here.<br/>
 * To define these values see <a
 * href="http://jerkar.github.io/documentation/latest/reference.html">Jerkar
 * Reference Guide section 3.3</a>
 */
public final class JkOptions {

    private static JkOptions INSTANCE = new JkOptions(loadSystemAndUserOptions());

    private final Map<String, String> props = new HashMap<>();

    private static boolean populated;

    static JkOptions instance() {
        return INSTANCE;
    }

    static synchronized void init(Map<String, String> options) {
        if (populated) {
            throw new IllegalStateException("The init method can be called only once.");
        }
        final Map<String, String> map = new HashMap<>();
        map.putAll(options);
        INSTANCE = new JkOptions(map);
        populated = true;
    }

    static boolean isPopulated() {
        return populated;
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

    static void populateFields(Object build) {
        populateFields(build, INSTANCE.props);
    }

    static Map<String, String> toDisplayedMap(Map<String, String> props) {
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

    private static Map<String, String> loadSystemAndUserOptions() {
        final File propFile = new File(JkLocator.jerkarHome(), "options.properties");
        final Map<String, String> result = new HashMap<>();
        if (propFile.exists()) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
        }
        final File userPropFile = new File(JkLocator.jerkarUserHome(), "options.properties");
        if (userPropFile.exists()) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
        }
        return result;
    }

    static String fieldOptionsToString(Object object) {
        final Map<String, String> map = JkOptions.toDisplayedMap(OptionInjector
                .injectedFields(object));
        return JkUtilsIterable.toString(map);
    }

}
