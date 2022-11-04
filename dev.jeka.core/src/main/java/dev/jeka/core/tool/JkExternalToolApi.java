package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exported methods to integrate with external tool.
 */
public final class JkExternalToolApi {

    private JkExternalToolApi(){
    }

    public static String getBeanName(String fullyQualifiedClassName) {
        return JkBean.name(fullyQualifiedClassName);
    }

    public static List<String> getCachedBeanClassNames(Path projectRoot) {
        return new EngineBeanClassResolver(projectRoot).readKbeanClasses();
    }

    public static JkRepoSet getDownloadRepos(Path projectDir) {
        JkProperties properties = JkRuntime.constructProperties(projectDir);
        JkRepoSet result = JkRepoProperties.of(properties).getDownloadRepos();
        return result;
    }

    /**
     * Returns <code>true</code> if the specified path is the root directory of a Jeka project.
     */
    public static boolean isJekaProject(Path candidate) {
        Path jekaDir = candidate.resolve(JkConstants.JEKA_DIR);
        System.out.println(Arrays.asList(jekaDir.toFile().listFiles()));
        if (!Files.isDirectory(jekaDir)) {
            System.out.println("jeka dir ?" + Files.isDirectory(jekaDir));
            return false;
        }
        Path defDir = jekaDir.resolve(JkConstants.DEF_DIR);
        if (Files.isDirectory(defDir)) {
            return true;
        }
        if (Files.isRegularFile(jekaDir.resolve(JkConstants.PROPERTIES_FILE))) {
            return true;
        }
        if (Files.isRegularFile(jekaDir.resolve(JkConstants.PROJECT_DEPENDENCIES_TXT_FILE))) {
            return true;
        }
        if (Files.isDirectory(jekaDir.resolve(JkConstants.PROJECT_LIBS_DIR))) {
            return true;
        }
        Path wrapperDir = jekaDir.resolve("wrapper");
        if (Files.isDirectory(wrapperDir)) {
            return true;
        }
        if (Files.isRegularFile(candidate.resolve("jekaw"))) {
            return true;
        }
        if ((Files.isRegularFile(candidate.resolve("jekaw.bat")))) {
            return true;
        }
        return false;
    }

    public static Path getImlFile(Path moduleDir) {
        return JkImlGenerator.getImlFilePath(moduleDir);
    }

    public static Map<String, String> getCmdShortcutsProperties(Path projectDir) {
        Map<String, String> result = JkRuntime.readProjectPropertiesRecursively(projectDir)
                .getAllStartingWith(JkConstants.CMD_PROP_PREFIX, false);
        result.remove(JkConstants.CMD_APPEND_PROP.substring(JkConstants.CMD_PROP_PREFIX.length()));
        return new TreeMap(result);
    }

    public static JkProperties getProperties(Path projectDir) {
        return JkRuntime.readProjectPropertiesRecursively(projectDir);
    }

    public static JkProperties getGlobalProperties() {
        JkProperties result = JkProperties.SYSTEM_PROPERTIES
                .withFallback(JkProperties.ENVIRONMENT_VARIABLES);
        Path globalPropertiesFile = JkLocator.getJekaUserHomeDir().resolve(JkConstants.GLOBAL_PROPERTIES);
        if (Files.exists(globalPropertiesFile)) {
            result = result.withFallback(JkProperties.ofFile(globalPropertiesFile));
        }
        return result;
    }

}
