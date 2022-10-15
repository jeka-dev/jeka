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

    public static final JkProperties EMPTY = JkProperties.of("", Collections.emptyMap());

    private final String source;

    private final Map<String, String> props;

    private final JkProperties fallback;

    private JkProperties(String source, Map<String, String> props, JkProperties fallback) {
        this.source = source;
        this.props = props;
        this.fallback = fallback;
    }

    public static JkProperties of(String soureName, Map<String, String> props) {
        return new JkProperties(soureName, Collections.unmodifiableMap(new HashMap<>(props)), null);
    }

    public static JkProperties ofEnvironmentVariables() {
        Map<String, String> props = new HashMap<>();
        for (String varName : System.getenv().keySet() ) {
            String value = System.getenv(varName);
            props.put(varName, value);
            props.put(lowerCase(varName), value);
        }
        return new JkProperties("Environment Variables", Collections.unmodifiableMap(props), null);
    }

    public static JkProperties ofSystemProperties() {
        Map<String, String> props = new HashMap<>();
        for (String propName : System.getProperties().stringPropertyNames() ) {
            props.put(propName, System.getProperty(propName));
        }
        return new JkProperties("System Properties", Collections.unmodifiableMap(props), null);
    }

    public static JkProperties of(Path propertyFile) {
        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(propertyFile.toFile())) {
            properties.load(is);
            return of(propertyFile.toString(), JkUtilsIterable.propertiesToMap(properties));
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

    public Map<String,String> getAllStartingWith(String prefix) {
        Map<String, String> result = new HashMap<>();
        for (String key : find(prefix)) {
            result.put(key, get(key));
        }
        return result;
    }

    public Set<String> find(String prefix) {
        Set<String> result = new HashSet<>();
        result.addAll(props.keySet().stream()
                .filter(name -> name.startsWith(prefix))
                .collect(Collectors.toSet())
        );
        if (fallback != null) {
            result.addAll(fallback.find(prefix));
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(source).append("\n");
        props.forEach( (key, val) -> result.append(key +  "=" + val + "\n"));
        if (fallback != null) {
            result.append(fallback);
        }
        return result.toString();
    }
}
