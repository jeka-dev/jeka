package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
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

    private static JkProperties ofEnvironmentVariables() {
        Map<String, String> props = new HashMap<>();
        for (String varName : System.getenv().keySet() ) {
            String value = System.getenv(varName);
            props.put(varName, value);
            props.put(lowerCase(varName), value);
        }
        return new JkProperties(ENV_VARS_NAME, Collections.unmodifiableMap(props), null);
    }

    // The system properties are likely to change suring the run, so we cannot cache it.
    private static JkProperties ofSystemProperties() {
        Map<String, String> props = new HashMap<>();
        for (String propName : System.getProperties().stringPropertyNames() ) {
            props.put(propName, System.getProperty(propName));
        }
        return new JkProperties(SYS_PROPS_NAME, Collections.unmodifiableMap(props), null);
    }

    public static JkProperties ofFile(Path propertyFile) {
        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(propertyFile.toFile())) {
            properties.load(is);
            return ofMap(propertyFile.toString(), JkUtilsIterable.propertiesToMap(properties));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(propertyFile + " not found");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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
     *
     *  See <a href="https://google.github.io/styleguide/shellguide.html#s7.3-constants-and-environment-variable-names">Environment variable conventions</a>
     */
    public String get(String propertyName) {
        String rawValue = getRawValue(propertyName);
        if (rawValue == null) {
            return null;
        }
        return interpolate(rawValue);
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

    public String toKeyValueString(String margin) {
        Set<String> keys = find("");
        StringBuilder sb = new StringBuilder();
        JkProperties systemLess = systemLess();
        if (systemLess == null) {
            return "";
        }
        int maxKeyLength = keys.stream()
                .max(Comparator.comparingInt(String::length))
                .orElse("").length();
        int maxValueLength = keys.stream()
                .filter(key -> systemLess.get(key) != null)
                .map(this::get)
                .max(Comparator.comparingInt(String::length))
                .orElse("").length();
        for (String key : keys) {
            if (systemLess.get(key) == null) {
                continue;
            }
            String value = get(key);
            String keyLabel = JkUtilsString.padEnd(key, maxKeyLength + 1, ' ');
            String valueLabel = JkUtilsString.padEnd(value, maxValueLength + 2, ' ');
            JkProperties declaringProps = getSourceDefining(key);
            sb.append(margin + keyLabel + ":" + valueLabel + "[from " + declaringProps.source + "]\n");
        }
        return sb.toString();
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
