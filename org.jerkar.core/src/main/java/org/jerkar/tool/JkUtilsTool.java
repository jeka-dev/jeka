package org.jerkar.tool;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.JkUtilsFile;

/**
 * Utilities related to running piece of code outside of the tool itself.
 */
public final class JkUtilsTool {

    static Map<String, String> userSystemProperties() {
        final Map<String, String> result = new HashMap<String, String>();
        final File userPropFile = new File(JkLocator.jerkarUserHome(), "system.properties");
        if (userPropFile.exists()) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
        }
        return result;
    }

    static void setSystemProperties(Map<String, String> props) {
        for (final Map.Entry<String, String> entry : props.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    public static void loadUserSystemProperties() {
        setSystemProperties(userSystemProperties());
    }

    private JkUtilsTool() {
        // Can't instantiate
    }

}
