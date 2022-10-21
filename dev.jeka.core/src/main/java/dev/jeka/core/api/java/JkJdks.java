package dev.jeka.core.api.java;

import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JkJdks {

    private final Map<JkJavaVersion, Path> explicitJdkHomes;

    private JkJdks(Map<JkJavaVersion, Path> explicitJdkHomes) {
        this.explicitJdkHomes = explicitJdkHomes;
    }

    public static JkJdks of() {
        return new JkJdks(Collections.emptyMap());
    }

    public static JkJdks ofJdkHomeProps(Map<String, String> homes) {
        Map<JkJavaVersion, Path> map = new HashMap<>();
        for (Map.Entry<String, String> entry : homes.entrySet()) {
            map.put(JkJavaVersion.of(entry.getKey().trim()), Paths.get(entry.getValue().trim()));
        }
        return new JkJdks(map);
    }

    public Path getHome(JkJavaVersion javaVersion) {
        Path result = explicitJdkHomes.get(javaVersion);
        if (result == null && javaVersion.equals(JkJavaVersion.ofCurrent())) {
            return Paths.get(System.getProperty("java.home"));
        }
        if (result != null && !Files.exists(result)) {
            JkLog.warn("Specified path for JDK %s does not exists", result);
            return null;
        }
        return result;
    }
}
