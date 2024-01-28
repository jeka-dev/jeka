package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
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
        return KBean.name(fullyQualifiedClassName);
    }

    public static List<String> getCachedBeanClassNames(Path projectRoot) {
        return new EngineKBeanClassResolver(projectRoot).readKbeanClasses();
    }

    public static JkRepoSet getDownloadRepos(Path projectDir) {
        JkProperties properties = JkRunbase.constructProperties(projectDir);
        JkRepoSet result = JkRepoProperties.of(properties).getDownloadRepos();
        return result;
    }

    /**
     * Returns <code>true</code> if the specified path is the root directory of a Jeka project.
     */
    public static boolean isJekaProject(Path candidate) {
        Path jekaSrc = candidate.resolve(JkConstants.JEKA_SRC_DIR);
        if (Files.isDirectory(jekaSrc)) {
            return true;
        }
        if (Files.isRegularFile(candidate.resolve(JkConstants.PROPERTIES_FILE))) {
            return true;
        }
        if (Files.isRegularFile(candidate.resolve(JkProject.DEPENDENCIES_TXT_FILE))) {
            return true;
        }
        if (Files.isDirectory(candidate.resolve(JkProject.PROJECT_LIBS_DIR))) {
            return true;
        }
        if (Files.isRegularFile(candidate.resolve("jeka"))) {
            return true;
        }
        return Files.isRegularFile(candidate.resolve("jeka.bat"));
    }

    public static Path getImlFile(Path moduleDir) {
        return getImlFile(moduleDir, false);
    }

    public static Path getImlFile(Path moduleDir, boolean inJekaSubDir) {
        return JkImlGenerator.getImlFilePath(moduleDir);
    }

    public static Map<String, String> getCmdShortcutsProperties(Path projectDir) {
        Map<String, String> result = JkRunbase.readProjectPropertiesRecursively(projectDir)
                .getAllStartingWith(JkConstants.CMD_PREFIX_PROP, false);
        List<String> keys = new LinkedList<>(result.keySet());
        keys.stream().filter(key -> key.startsWith(JkConstants.CMD_APPEND_SUFFIX_PROP))
                        .forEach(key -> result.remove(key));
        return new TreeMap(result);
    }

    public static JkProperties getProperties(Path projectDir) {
        return JkRunbase.readProjectPropertiesRecursively(projectDir);
    }

    public static JkProperties getGlobalProperties() {
        return JkProperties.ofSysPropsThenEnvThenGlobalProperties();
    }

    public static List<Path> getDefDependenciesClasspath(Path projectDir) {
        return new EngineClasspathCache(projectDir, null).readCachedResolvedClasspath(EngineClasspathCache.Scope.ALL)
                .getEntries();
    }

    public static boolean kbeanNameMatches(String className, String candidate) {
        return KBean.nameMatches(className, candidate);
    }
}
