/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.api.tooling.intellij.JkImlReader;
import dev.jeka.core.api.tooling.intellij.JkIntellijJdk;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exported methods to integrate with external tools.
 */
public final class JkExternalToolApi {

    // The following fields and methods are part of the API used by external tools
    // Please don't remove.

    public static final String CMD_PREFIX_PROP = JkConstants.CMD_PREFIX_PROP;

    public static final String CMD_APPEND_SUFFIX_PROP =  JkConstants.CMD_APPEND_SUFFIX_PROP;

    public static final String CMD_SUBSTITUTE_SYMBOL = JkConstants.CMD_SUBSTITUTE_SYMBOL;

    private JkExternalToolApi(){
    }

    /**
     * Converts the given templated text to ANSI escape sequences.
     *
     * @param templatedText the input text to be converted to ANSI escape sequences
     * @return the converted text with ANSI escape sequences
     */
    public static String ansiText(String templatedText) {
        return CommandLine.Help.Ansi.AUTO.string(templatedText);
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
        JkProperties properties = PropertiesHandler.constructRunbaseProperties(baseDir);
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
        return Files.isRegularFile(candidate.resolve("jeka.ps1"));
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
        Map<String, String> result = PropertiesHandler.readJekaPropertiesRecursively(baseDir)
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
        return PropertiesHandler.constructRunbaseProperties(baseDir);
    }

    /**
     * Returns the global properties.
     */
    public static JkProperties getGlobalProperties() {
        return JkProperties.ofStandardProperties();
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

    /**
     * Resolves and retrieves the path to the KBean class name cache file located in the specified base directory.
     *
     * @param baseDir the root directory from which the path will be resolved
     * @return the resolved path to the KBean class name cache file
     */
    public static Path getKBeanClassnameCacheFile(Path baseDir) {
        return baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(JkConstants.KBEAN_CLASS_NAMES_CACHE_FILE);
    }

    /**
     * Resolves the path to the KBean classpath cache file located in the specified base directory.
     *
     * @param baseDir the base directory from which the path to the KBean classpath cache file will be resolved
     * @return the resolved path to the KBean classpath cache file
     */
    public static Path getKBeanClasspathCacheFile(Path baseDir) {
        return baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve(JkConstants.KBEAN_CLASSPATH_CACHE_FILE);
    }

    /**
     * Retrieves a list of available command-line options for the annotated class.
     * Each option is represented as an array of strings, containing the following information:
     * - The option's longest name.
     * - The option's description.
     * - The type name of the option.
     * - The parameter label of the option.
     *
     * @return a list of string arrays, each representing an available option's details
     */
    public static List<String[]> availableOptions() {
        List<CommandLine.Model.OptionSpec> options =CommandLine.Model.CommandSpec
                .forAnnotatedObject(new PicocliMainCommand())
                .options();
        return options.stream()
                .map(optionSpec -> {
                    String name = optionSpec.longestName();
                    String desc = String.join(" ", optionSpec.description());
                    String typeName = optionSpec.type().getTypeName();
                    String paramLabel = optionSpec.paramLabel();
                    return new String[] {name, desc, typeName, paramLabel};
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the default and initialization KBean class names for the specified base directory.
     *
     * @param baseDir the base directory from which to read and determine the KBean class names
     * @return an instance of {@code EngineClasspathCache.DefafaulAndInitKBeans} containing the default
     *         and initialization KBean class names
     */
    public static InitKBeans getDefaultAndInitKBeans(Path baseDir) {
        KBeanInitStore store = KBeanInitStore.read(baseDir);
        return InitKBeans.of(store);
    }

    /**
     * Retrieves the JDK information including version, distribution name, and the JDK home directory
     * based on the provided base directory.
     *
     * @param baseDir the base directory used to resolve JDK properties and paths
     * @return an instance of JdkInfo containing the resolved JDK information
     */
    public static JdkInfo getJdkInfo(Path baseDir) {
        return JdkInfo.of(baseDir);
    }

    /**
     * Returns the jeka jdk names used into generated iml, so that Jeka IDE can add
     * it in its SDK table
     */
    public static List<SdkInfo> readJdkInfoFromIml(Path baseDir) {
        List<Path> imlFiles = JkUtilsPath.listDirectChildren(baseDir).stream()
                .filter(path -> path.toString().endsWith(".iml"))
                .collect(Collectors.toCollection(LinkedList::new));
        JkUtilsPath.listDirectChildren(baseDir.resolve(".idea")).stream()
                .filter(path -> path.toString().endsWith(".iml"))
                .forEach(imlFiles::add);
        return imlFiles.stream()
                .map(JkImlReader::getJdk)
                .filter(Objects::nonNull)
                .filter(JkIntellijJdk::isJekaManaged)
                .map(intellijJdk -> new SdkInfo(
                        intellijJdk.getJdkName(),
                        intellijJdk.getJdkVersion().toString(),
                        getJavaHome(baseDir, intellijJdk)
                ))
                .collect(Collectors.toList());
    }

    private static Path getJavaHome(Path baseDir, JkIntellijJdk intellijJdk) {
        if (intellijJdk.isCustom()) {
            JkProperties properties = getProperties(baseDir);
            String propName = "jeka.jdk." + intellijJdk.getJdkVersion();
            return Paths.get(properties.getTrimmedNonBlank(propName));
        }
        String distAndVersion = JkUtilsString.substringAfterFirst(intellijJdk.getJdkName(), "jeka-");
        return JkLocator.getCacheDir().resolve("jdks").resolve(distAndVersion);
    }

    /**
     * Retrieves the child paths based on the given base directory.
     *
     * @param baseDir the base directory from which to determine child paths
     * @return a list of paths representing the child paths of the specified base directory
     */
    public static List<Path> getChildPaths(Path baseDir) {
        JkProperties props = getProperties(baseDir);
        return RunbaseGraph.getChildBaseProps(baseDir, props);
    }

    public static List<String> getKbeansDeclaredInJekaProperties(Path baseDir) {
        JkProperties properties = getProperties(baseDir);
        return properties.getAllStartingWith(JkRunbase.PROP_KBEAN_PREFIX, false).keySet().stream()
                .filter(key -> !key.contains("."))
                .filter(key -> {
                    String propName = JkRunbase.PROP_KBEAN_PREFIX + key;
                    String value = JkUtilsString.nullToEmpty(properties.get(propName)).trim();
                    return !JkRunbase.PROP_KBEAN_OFF.equals(value);
                })
                .collect(Collectors.toList());
    }

    public static boolean KbeanNameMatches(String className, String candidate) {
        return KBean.nameMatches(className, candidate);
    }

    public static class InitKBeans {

        public final String defaultClassName;

        public final List<String> involvedClassNames;

        InitKBeans(String defaultClassName, List<String> involvedClassNames) {
            this.defaultClassName = defaultClassName;
            this.involvedClassNames = involvedClassNames;
        }

        static InitKBeans of(KBeanInitStore store) {
            return new InitKBeans(store.defaultKBeanClassName,
                    store.involvedKBeanClassNames);
        }
    }

    public static class SdkInfo {

        public final String sdkName;

        public final String sdkVersion;

        public final Path javaHome;

        SdkInfo(String sdkName, String sdkVersion, Path javaHome) {
            this.sdkName = sdkName;
            this.sdkVersion = sdkVersion;
            this.javaHome = javaHome;
        }
    }

    public static class JdkInfo {

        public static final String LOCAL = "local";

        public final String version;

        // values 'local' when the path is specified in 'jeka.jdk.xxx='
        public final String distribName;

        // 'java' bin lies in jkdHome/bin
        public final Path jdkHome;

        public JdkInfo(String version, String distribName, Path jdkHome) {
            this.version = version;
            this.distribName = distribName;
            this.jdkHome = jdkHome;
        }

        static JdkInfo of(Path moduleDir) {
            JkProperties props = JkExternalToolApi.getProperties(moduleDir);
            String version = props.getTrimmedNonBlank(JkConstants.JEKA_JAVA_VERSION_PROP);
            if (JkUtilsString.isBlank(version)) {
                version = JkJavaVersion.LAST_LTS.toString();
            }
            String specificJdkPathProp = "jeka.jdk." + version.trim();
            String specificLocation = props.getTrimmedNonBlank(specificJdkPathProp);
            String specifiedDistrib = props.getTrimmedNonBlank(JkConstants.JEKA_JAVA_DISTRIB_PROP);

            final String effectiveDistrib;
            if (specifiedDistrib != null) {
                effectiveDistrib = specifiedDistrib;
            } else if (specificLocation != null) {
                effectiveDistrib = LOCAL;
            } else {
                effectiveDistrib = "temurin";
            }

            final Path distribPath;
            if (LOCAL.equals(effectiveDistrib)) {
                distribPath = Paths.get(specificLocation);
            } else {
                distribPath = JkLocator.getCacheDir().resolve("jdks").resolve(effectiveDistrib + "-" + version);
            }
            return new JdkInfo(version, effectiveDistrib, distribPath);
        }
    }

    private static class EngineClasspathCache {

        private static final String RESOLVED_CLASSPATH_FILE = "resolved-classpath.txt";

        private static final String KBEAN_CLASSES_CACHE_FILE_NAME = "jeka-kbean-classes.txt";

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
