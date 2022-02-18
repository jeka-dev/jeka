package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.JkUtilsFile;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class in charge of loading properties from different sources (property files) and set it as System properties.
 */
final class PropertyLoader {

    private PropertyLoader() {
    }

    static void load() {
        Map<String, String> props = new TreeMap<>();
        props.putAll(readGlobalProperties());
        props.putAll(readProjectPropertiesRecursively(Paths.get("")));
        props.putAll(Environment.commandLine.getSystemProperties());
        props.forEach((k,v) -> System.setProperty(k, v));
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

    private static Map<String, String> readGlobalProperties() {
        final Path userPropFile = JkLocator.getJekaUserHomeDir().resolve(JkConstants.GLOBAL_PROPERTY_FILE_NAME);
        if (Files.exists(userPropFile)) {
            return JkUtilsFile.readPropertyFileAsMap(userPropFile);
        }
        return Collections.emptyMap();
    }

    static Map<String, String> readProjectPropertiesRecursively(Path projectBaseDir) {
        Path parentProject = projectBaseDir.toAbsolutePath().normalize().getParent();
        Map<String, String> result = new HashMap<>();
        if (parentProject != null && Files.exists(parentProject.resolve(JkConstants.JEKA_DIR))
                & Files.isDirectory(parentProject.resolve(JkConstants.JEKA_DIR))) {
            result.putAll(readProjectPropertiesRecursively(parentProject));
        }
        Path presetCommandsFile = projectBaseDir.resolve("jeka/" + JkConstants.PROJECT_PROPERTY_FILE_NAME);
        if (Files.exists(presetCommandsFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(presetCommandsFile));
        }
        return result;
    }

}
