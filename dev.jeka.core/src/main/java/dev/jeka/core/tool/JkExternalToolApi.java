package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Exported methods to integrate with external tools.
 *
 */
public final class JkExternalToolApi {

    private JkExternalToolApi(){
    }

    /**
     * Retrieves the bean name for the given fully qualified class name.
     */
    public static String getBeanName(String fullyQualifiedClassName) {
        return KBean.name(fullyQualifiedClassName);
    }

    /**
     * Returns the cached bean class names for the given base dir.
     */
    public static List<String> getCachedBeanClassNames(Path baseDir) {
        return new EngineClasspathCache(baseDir).readKbeanClasses();
    }

    /**
     * Returns download repositories for the specified base directory.
     */
    public static JkRepoSet getDownloadRepos(Path baseDir) {
        JkProperties properties = JkRunbase.constructProperties(baseDir);
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

    /**
     * Retrieves the path of the iml file for the given module directory.
     */
    public static Path getImlFile(Path moduleDir) {
        return getImlFile(moduleDir, false);
    }


    private static Path getImlFile(Path moduleDir, boolean inJekaSubDir) {
        return JkImlGenerator.getImlFilePath(moduleDir);
    }

    /**
     * Returns the command shorthand properties defined in properties files, from
     * the specified base dir context.
     */
    public static Map<String, String> getCmdShorthandsProperties(Path baseDir) {
        Map<String, String> result = JkRunbase.readProjectPropertiesRecursively(baseDir)
                .getAllStartingWith(JkConstants.CMD_PREFIX_PROP, false);
        List<String> keys = new LinkedList<>(result.keySet());
        keys.stream().filter(key -> key.startsWith(JkConstants.CMD_APPEND_SUFFIX_PROP))
                        .forEach(key -> result.remove(key));
        return new TreeMap(result);
    }

    /**
     * Returns the properties defined from the specifies base dir..
     */
    public static JkProperties getProperties(Path baseDir) {
        return JkRunbase.readProjectPropertiesRecursively(baseDir);
    }

    /**
     * Returns the global properties.
     */
    public static JkProperties getGlobalProperties() {
        return JkProperties.ofSysPropsThenEnvThenGlobalProperties();
    }

    /**
     * Returns the default dependencies classpath for the specified project directory..
     */
    public static List<Path> getDefDependenciesClasspath(Path projectDir) {
        return new EngineClasspathCache(projectDir).readCachedResolvedClasspath(EngineClasspathCache.Scope.ALL)
                .getEntries();
    }

    /**
     * Checks if the specified className matches the given candidate name.
     *
     * @param className The fully qualified class name to compare.
     * @param candidate The name candidate to match against the class name.
     * @return True if the candidate matches the class name, false otherwise.
     */
    public static boolean kbeanNameMatches(String className, String candidate) {
        return KBean.nameMatches(className, candidate);
    }


    private static class EngineClasspathCache {

        private static final String RESOLVED_CLASSPATH_FILE = "resolved-classpath.txt";

        private static final String KBEAN_CLASSES_CACHE_FILE_NAME = "kbean-classes.txt";

        private final Path baseDir;

        enum Scope {
            ALL, EXPORTED;
            String prefix() {
                return this == ALL ?  "" : "exported-";
            }
        }

        EngineClasspathCache(Path baseDir) {
            this.baseDir = baseDir;
        }

        JkPathSequence readCachedResolvedClasspath(Scope scope) {
            return JkPathSequence.ofPathString(JkPathFile.of(resolvedClasspathCache(scope)).readAsString());
        }

        List<String> readKbeanClasses() {
            Path store = baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(KBEAN_CLASSES_CACHE_FILE_NAME);
            if (!Files.exists(store)) {
                return Collections.emptyList();
            }
            return JkUtilsPath.readAllLines(store);
        }

        private Path resolvedClasspathCache(Scope scope) {
            return baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(scope.prefix() + RESOLVED_CLASSPATH_FILE);
        }

    }
}
