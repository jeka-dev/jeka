package dev.jeka.core.api.system;

/**
 * Abstraction over system property and environment variable.
 * This class provides a single method to get a value located in System Properties or Environment variables.
 */
public final class JkProperty {

    private JkProperty() {
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
        String result = System.getProperty(key);
        if (result != null) {
            return result;
        }
        result = System.getenv(key);
        if (result != null) {
            return key;
        }
        String upperCaseKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        return System.getenv(upperCaseKey);
    }


}
