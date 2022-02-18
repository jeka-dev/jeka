package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsString;

import java.util.*;

/**
 * Abstraction over system property and environment variable.
 * This class provides a single method to get a value located in System Properties or Environment variables.
 */
public final class JkProperties {

    private static final Map<String, String> OVERRIDE_PROPS = new HashMap<>();

    private JkProperties() {
    }

    /**
     * Returns the value for the specified fey according rule :
     * <ul>
     *    <li> if {@link System#getProperty(String)} exists then the value is returned </li>
     *  * <li> if {@link System#getenv()} contains the specified key, the value is returned </li>
     *  * <li> the key is normalized to upper case then if {@link System#getenv()} contains the normalized key, the value is returned. </li>
     * </ul>
     *
     *  See <a href="https://google.github.io/styleguide/shellguide.html#s7.3-constants-and-environment-variable-names">Environment variable conventions</a>
     */
    public static String get(String key) {
        String result = OVERRIDE_PROPS.get(key);
        if (result != null) {
            return result;
        }
        result = System.getProperty(key);
        if (result != null) {
            return interpolate(result);
        }
        result = System.getenv(key);
        if (result != null) {
            return interpolate(result);
        }
        String upperCaseKey = upperCase(key);
        result = System.getenv(upperCaseKey);
        return result == null ? null : interpolate(result);
    }

    /**
     * Override the current properties with the specified ones.
     * Use it with caution !!!
     */
    public static void override(Map<String, String> overrideProps) {
        OVERRIDE_PROPS.putAll(overrideProps);
    }

    /**
     * Remove properties that has been ovveridded using {@link #override(Map)}
     */
    public static void removeOverride() {
        OVERRIDE_PROPS.clear();;
    }

    private static String interpolate(String string) {
        return JkUtilsString.interpolate(string, JkProperties::get);
    }

    private static String upperCase(String value) {
        return value.toUpperCase().replace('.', '_').replace('-', '_');
    }

    private static String lowerCase(String value) {
        return value.toLowerCase().replace('_', '.');
    }

    public static Map<String,String> getAllStartingWith(String prefix) {
        Map<String, String> result = new HashMap<>();
        for (String key : find(prefix)) {
            result.put(key, get(key));
        }
        return result;
    }

    public static Set<String> find(String prefix) {
        Set<String> result = new HashSet<>();
        for (String extraKey : OVERRIDE_PROPS.keySet()) {
            if (extraKey.startsWith(prefix)) {
                result.add(extraKey);
            }
        }
        for (Enumeration propNameEnum = System.getProperties().propertyNames(); propNameEnum.hasMoreElements();) {
            String name = (String) propNameEnum.nextElement();
            if ( name.startsWith(prefix)) {
                result.add(name);
            }
        }
        String upperCaseKey = upperCase(prefix);
        for (String propName : System.getenv().keySet()) {
            if ( propName.startsWith(prefix) || propName.startsWith(upperCaseKey)) {
                result.add(lowerCase(propName));
            }
        }
        return result;
    }


}
