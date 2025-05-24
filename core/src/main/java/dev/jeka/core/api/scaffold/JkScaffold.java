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

package dev.jeka.core.api.scaffold;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Object that process scaffolding.
 */
public abstract class JkScaffold {

    public static final String UNSPECIFIED_JEKA_VERSION = "NO";

    private static final String JEKA_WORK_IGNORE = "/.jeka-work";

    private static final String JEKA_OUTPUT_IGNORE = "/jeka-output";

    private static final String LAST_VERSION_OF_TOKEN = "${lastVersionOf:";

    @JkDepSuggest(versionOnly = true, hint = "org.junit:junit-bom")
    public static final String JUNIT_VERSION = "5.12.2";

    protected final Path baseDir;

    private final JkRepoSet downloadRepos;

    public final List<JkFileEntry> fileEntries = new LinkedList<>();

    private String jekaVersion;

    private String jekaDistribRepo;

    private String jekaDistribLocation;

    private String jekaPropsContent = "";

    // If not null, we take this file content as is and ignore other settings
    private Path rawJekaPropsPath;

    private UnaryOperator<String> jekaPropCustomizer = s -> s;

    protected JkScaffold(Path baseDir, JkRepoSet downloadRepos) {
        super();
        this.baseDir= baseDir;
        this.downloadRepos = downloadRepos;
    }

    protected JkScaffold(Path baseDir) {
        this(baseDir, JkRepoProperties.ofGlobalProperties().getDownloadRepos());
    }

    public JkScaffold addJekaPropsContent(String extraContent) {
        if (JkUtilsString.isBlank(extraContent)) {
            return this;
        }
        this.jekaPropsContent += extraContent;
        return this;
    }

    /**
     * Adds a property to scaffolded jeka.properties by specifying a string with format "prop.name=prop.value".
     */
    public JkScaffold addJekaPropValue(String propValue) {
        return addJekaPropsContent(propValue.trim() + "\n");
    }

    public JkScaffold setJekaVersion(String jekaVersion) {
        this.jekaVersion = jekaVersion;
        return this;
    }

    /**
     * Sets the path of a file that will be copied to jeka.properties.
     * <p>
     * If such a path is set, all other settings related to jeka.properties generation
     * are ignored and the content of jeka.properties will be exactly the same as
     * the specified file.
     *
     */
    public JkScaffold setRawJekaPropsPath(Path rawJekaPropsPath) {
        this.rawJekaPropsPath = rawJekaPropsPath;
        return this;
    }

    public JkScaffold setJekaDistribRepo(String jekaDistribRepo) {
        this.jekaDistribRepo = jekaDistribRepo;
        return this;
    }

    public JkScaffold setJekaDistribLocation(String jekaDistribLocation) {
        this.jekaDistribLocation = jekaDistribLocation;
        return this;
    }

    /**
     * Finds the latest version for a given module coordinate. If the latest version cannot be found or an exception occurs,
     * the method returns the default version.
     *
     * @param moduleCoordinate the coordinate of the module for which to find the latest version
     * @param defaultVersion the default version to be returned if the latest version cannot be found
     */
    public String findLatestStableVersion(String moduleCoordinate, String defaultVersion) {
        try {
            List<String> versions = JkDependencyResolver.of(downloadRepos)
                    .searchVersions(moduleCoordinate);
            return versions.stream()
                    .filter(version -> !version.contains("-"))
                    .sorted(JkVersion.VERSION_COMPARATOR.reversed())
                    .findFirst().get();
        } catch (Exception e) {
            JkLog.warn(e.getMessage());
            JkLog.warn("Cannot find latest version for '%s, choose default : %s ", moduleCoordinate, defaultVersion);
            return defaultVersion;
        }
    }

    protected List<String> getJUnitDeps() {
        String jupiterModuleId = "org.junit.jupiter:junit-jupiter";
        String launcherModuleId = "org.junit.platform:junit-platform-launcher";
        return Arrays.asList(jupiterModuleId, launcherModuleId);
    }

    protected String getJekaVersion() {
        return jekaVersion;
    }

    /**
     * Runs the scaffolding, meaning folder structure, build class, props file and .gitignore
     */
    protected void run() {
        // Create 'jeka-src' dir
        final Path jekaSrc = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        JkLog.verbose("Create %s", jekaSrc);

        JkUtilsPath.createDirectories(jekaSrc);
        createOrUpdateJekaProps();  // Create 'jeka.properties' file
        createOrUpdateGitIgnore();  // Create .gitignore
        createShellScripts();   // Shell scripts
        fileEntries.forEach(extraEntry -> extraEntry.write(baseDir));
    }

    /**
     * Returns the last version of the Jeka module.
     * <p>
     * This method searches for versions of the Jeka module in the specified download repositories.
     * It filters out any snapshot versions and returns the last non-snapshot version found.
     * If no versions are found, it logs a warning message and returns the current Jeka version.
     *
     * @return the last version of the Jeka module
     */
    public final String lastJekaVersion() {
        List<String> versions = JkDependencyResolver.of(downloadRepos)
                .searchVersions(JkInfo.JEKA_MODULE_ID).stream()
                    .filter(version -> !JkVersion.of(version).isSnapshot())
                    .collect(Collectors.toList());
        if (versions.isEmpty()) {
            JkLog.warn("Didn't find any version of " + JkInfo.JEKA_MODULE_ID + " in " + downloadRepos);
            JkLog.warn("Will use current one : " + JkInfo.getJekaVersion());
            return JkInfo.getJekaVersion();
        }
        return versions.get(versions.size() -1);
    }

    /**
     * Gives a chance to override the final jeka.properties file
     */
    public JkScaffold setJekaPropsCustomizer(UnaryOperator<String> customizer) {
        this.jekaPropCustomizer = customizer;
        return this;
    }

    /**
     * Adds a file entry to the list of file entries in the scaffold.
     * The file entry consists of a relative path and its content.
     * The file entry will be created or updated when the scaffold is run.
     */
    public final void addFileEntry(String relativePath, String content) {
        JkFileEntry fileEntry = JkFileEntry.of(Paths.get(relativePath), content);
        this.fileEntries.add(fileEntry);
    }

    /**
     * Removes the file entries that start with the specified path prefix.
     */
    public final void removeFileEntriesStartingWith(Path pathPrefix) {
        for (ListIterator<JkFileEntry> it = fileEntries.listIterator(); it.hasNext();) {
            JkFileEntry entry = it.next();
            if (entry.relativePath.startsWith(pathPrefix)) {
                it.remove();
            }
        }
    }

    /**
     * Removes a file entry from the list of file entries. This gives a chance to plugins
     * to remove the non-necessary files.
     *
     * @param relativePath the path, relative to base dir.
     */
    public void removeFileEntry(String relativePath) {
        ListIterator<JkFileEntry> it = fileEntries.listIterator();
        while (it.hasNext()) {
            JkFileEntry fileEntry = it.next();
            if (relativePath.equals(fileEntry.relativePath.toString())) {
                it.remove();
            }
        }
    }

    /**
     * Returns the content of a resource file as a string.
     */
    public static String readResource(Class<?> clazz, String resourceName) {
        return JkUtilsIO.read(clazz.getResource(resourceName));
    }

    /**
     * Creates or replaces shell scripts in the specified base directory, meaning jeka.ps1 and jeka bash scripts.
     * @param baseDir the base directory where the shell scripts will be created or replaced.
     */
    public static void createShellScripts(Path baseDir) {
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("jeka.ps1");
        if (Files.exists(jekaBat)) {
            JkLog.verbose("Create jeka.ps1 file");
            JkUtilsPath.copy(jekaBat, baseDir.resolve("jeka.ps1"), StandardCopyOption.REPLACE_EXISTING);
        }
        Path jekaShell = JkLocator.getJekaHomeDir().resolve("jeka");
        if (Files.isDirectory(jekaShell)) {
            JkLog.warn("%s directory is still present. Cannot create jeka shell file in base directory.", jekaShell);
        } else if (Files.exists(jekaShell)) {
            JkLog.verbose("Create jeka shell file");
            JkUtilsPath.copy(jekaShell, baseDir.resolve("jeka"),
                    StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            JkPathFile.of(jekaShell).setPosixExecPermissions(true, true, true);
        }
    }

    private void createOrUpdateJekaProps() {
        final String fileContent;

        if (rawJekaPropsPath != null) {
            fileContent = JkPathFile.of(rawJekaPropsPath).readAsString();
        } else {
            StringBuilder sb = new StringBuilder();
            String propJekaVersion = this.jekaVersion;
            if (JkUtilsString.isBlank(propJekaVersion)) {
                propJekaVersion = this.getLastJekaVersionSafely();
            }
            if (!UNSPECIFIED_JEKA_VERSION.equals(propJekaVersion) && !JkUtilsString.isBlank(propJekaVersion)) {
                sb.append("jeka.version=").append(propJekaVersion).append("\n");
            }
            if (!JkUtilsString.isBlank(jekaDistribLocation)) {
                sb.append("jeka.distrib.location=").append(jekaDistribLocation).append("\n");
            }

            if (!JkUtilsString.isBlank(this.jekaDistribRepo)) {
                sb.append("jeka.distrib.repo=").append(jekaDistribRepo).append("\n");
            }
            sb.append("jeka.java.version=").append(JkJavaVersion.LAST_LTS).append("\n");

            if (!JkUtilsString.isBlank(this.jekaPropsContent)) {
                String content = jekaPropsContent.replace("\\n", "\n");
                sb.append(content).append("\n");
            }
            String partialContent = sb.toString();
            fileContent = jekaPropCustomizer.apply(partialContent);
        }
        String interpolated = interpolateLastVersionOf(fileContent, 0, this.downloadRepos);

        JkPathFile jekaPropsFile  = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE));
        if (!jekaPropsFile.exists()) {
            jekaPropsFile.fetchContentFrom(JkScaffold.class.getResource(JkConstants.PROPERTIES_FILE));
        }

        jekaPropsFile.write(interpolated, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void createOrUpdateGitIgnore() {
        JkPathFile file = JkPathFile.of(baseDir.resolve(".gitignore"));
        file.createIfNotExist();
        List<String> lines = file.readAsLines();
        if (lines.stream().noneMatch(line -> JEKA_OUTPUT_IGNORE.equals(line.trim()))) {
            file.write("\n" + JEKA_OUTPUT_IGNORE, StandardOpenOption.APPEND);
        }
        if (lines.stream().noneMatch(line -> JEKA_WORK_IGNORE.equals(line.trim()))) {
            file.write("\n" + JEKA_WORK_IGNORE, StandardOpenOption.APPEND);
        }
    }

    private void createShellScripts() {
        createShellScripts(baseDir);
    }

    public static class JkFileEntry {

        public final Path relativePath;

        public final String content;

        private JkFileEntry(Path relativePath, String content) {
            JkUtilsAssert.argument(!relativePath.isAbsolute(), "%s should be relative.", relativePath);
            JkUtilsAssert.argument(!relativePath.toString().isEmpty(), "Relative path %s should not be empty", relativePath);
            this.relativePath = relativePath;
            this.content = content;
        }

        private static JkFileEntry of(Path relativePath, String content) {
            return new JkFileEntry(relativePath, content);
        }

        void write(Path baseDir) {
            JkPathFile.of(baseDir.resolve(relativePath)).createIfNotExist();
            JkUtilsPath.write(baseDir.resolve(relativePath), content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE);
        }
    }

    static String interpolateLastVersionOf(String originalContent, int from, JkRepoSet repos) {
        int index = originalContent.indexOf(LAST_VERSION_OF_TOKEN);
        if (index == -1) {
            return originalContent;
        }
        if (from >= originalContent.length()) {
            return originalContent;
        }
        int indexTo = originalContent.indexOf("}", index);
        if (indexTo == -1) {
            return originalContent;
        }
        String coordinate = originalContent.substring(index + LAST_VERSION_OF_TOKEN.length(), indexTo);
        System.out.println(coordinate);
        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(repos);
        Optional<JkVersion> latest = dependencyResolver.searchVersions(coordinate).stream()
                .map(JkVersion::of)
                .max(Comparator.naturalOrder());
        if (!latest.isPresent()) {
            throw new IllegalArgumentException("Cannot find versions for " + coordinate);
        }
        String wholeToken = LAST_VERSION_OF_TOKEN + coordinate + "}";
        String newContent = originalContent.replace(wholeToken, latest.get().toString());

        return interpolateLastVersionOf(newContent, 0, repos);
    }

    private String getLastJekaVersionSafely() {
        JkVersion version = getLastJekaVersionSafely(this.downloadRepos);
        return version != null ? version.toString() : null;
    }

    private static JkVersion getLastJekaVersionSafely(JkRepoSet repoSet) {
        JkVersion result = null;
        for (JkRepo repo : repoSet.getRepos()) {
            List<String> artifacts = Collections.emptyList();
            try {
                JkLog.info("Searching last Jeka version in repo %s ... ", repo.getUrl());
                artifacts =JkCoordinateSearch.of(JkRepo.ofMavenCentral())
                        .setTimeout(10000)
                        .setGroupOrNameCriteria("dev.jeka:jeka-core:")
                        .search();
            } catch (RuntimeException e) {
                JkLog.warn("Failed to get Jeka versions from repo %s : %s", repo.getUrl(), e.getMessage());
                if (JkLog.isDebug()) {
                    JkUtilsThrowable.printStackTrace(JkLog.getErrPrintStream(), e, 100);
                }
            }
            Optional<JkVersion> candidate = artifacts.stream()
                    .map(s -> JkUtilsString.substringAfterLast(s, ":"))
                    .map(JkVersion::of)
                    .filter(JkVersion::isDigitsOnly)
                    .max(Comparator.naturalOrder());
            if (candidate.isPresent()) {
                if (result == null) {
                    result = candidate.get();
                } else if (result.compareTo(candidate.get()) < 0) {
                    result = candidate.get();
                }
            }

        }
        return result;
    }


}
