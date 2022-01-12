package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkProperty;
import dev.jeka.core.api.utils.JkUtilsFile;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Set of Key/value strings used to parameter Jeka and builds.
 */
public final class JkProperties {

    public static final String GLOBAL_PROPERTY_FILE_NAME = "global.properties";

    public static final String PROJECT_PROPERTY_FILE_NAME = "project.properties";

    private static final JkProperties INSTANCE;

    static {
        Map<String, String> props = new TreeMap<>();
        props.putAll(readGlobalProperties());
        props.putAll(readProjectProperties(Paths.get("")));
        props.putAll(Environment.commandLine.getSystemProperties());
        INSTANCE = new JkProperties(props);
        props.forEach((k,v) -> System.setProperty(k, v));
    }

    private final Map<String, String> props;

    private JkProperties(Map<String, String> props) {
        this.props = props;
    }

    /**
     * Returns the value defined for the specified key. The override order is
     * <ul>
     *     <li>System Properties</li>
     *     <li>OS Environment Variables</li>
     *     <li>Properties defined in [USER DIR]/.jeka/global.properties</li>
     *     <li>Properties defined in [WORKING DIR]/jeka/project.properties</li>
     * </ul>
     */
    public static String get(String key) {
        return JkProperty.get(key);
    }

    public static boolean isDefined(String key) {
        if (System.getProperties().containsKey(key)) {
            return true;
        }
        return INSTANCE.props.containsKey(key);
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
                result.put(key, INSTANCE.get(key));
            }
        }
        return result;
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

    private static Map<String, String> readGlobalProperties() {
        final Path userPropFile = JkLocator.getJekaUserHomeDir().resolve(GLOBAL_PROPERTY_FILE_NAME);
        if (Files.exists(userPropFile)) {
            return JkUtilsFile.readPropertyFileAsMap(userPropFile);
        }
        return Collections.emptyMap();
    }

    private static Map<String, String> readProjectProperties(Path projectBaseDir) {
        Path presetCommandsFile = projectBaseDir.resolve("jeka/" + PROJECT_PROPERTY_FILE_NAME);
        if (Files.exists(presetCommandsFile)) {
            return JkUtilsFile.readPropertyFileAsMap(presetCommandsFile);
        }
        return Collections.emptyMap();
    }

}
