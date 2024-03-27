package dev.jeka.core.api.scaffold;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Object that process scaffolding.
 */
public abstract class JkScaffold {

    private static final String JEKA_WORK_IGNORE = "/.jeka-work";

    private static final String JEKA_OUTPUT_IGNORE = "/jeka-output";

    private final Path baseDir;

    private final JkRepoSet downloadRepos;

    public final List<JkFileEntry> fileEntries = new LinkedList<>();

    private String jekaVersion;

    private String jekaDistribRepo;

    private String jekaDistribLocation;

    private String jekaPropsExtraContent = "";

    protected JkScaffold(Path baseDir, JkRepoSet downloadRepos) {
        super();
        this.baseDir= baseDir;
        this.downloadRepos = downloadRepos;
    }

    protected JkScaffold(Path baseDir) {
        this(baseDir, JkRepoProperties.ofGlobalProperties().getDownloadRepos());
    }

    public JkScaffold addJekaPropsFileContent(String extraContent) {
        if (extraContent == null) {
            return this;
        }
        this.jekaPropsExtraContent += extraContent;
        return this;
    }

    /**
     * Adds a property to scaffolded jeka.properties by specifying a string with format "prop.name=prop.value".
     */
    public JkScaffold addJekaPropValue(String propValue) {
        if (JkUtilsString.isBlank(propValue)) {
            return this;
        }
        jekaPropsExtraContent += "\n" + propValue.trim();
        return this;
    }

    public JkScaffold setJekaVersion(String jekaVersion) {
        this.jekaVersion = jekaVersion;
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
    public String findLatestVersion(String moduleCoordinate, String defaultVersion) {
        try {
            List<String> springbootVersions = JkDependencyResolver.of(downloadRepos)
                    .searchVersions(moduleCoordinate);
            return springbootVersions.stream()
                    .sorted(JkVersion.VERSION_COMPARATOR.reversed())
                    .findFirst().get();
        } catch (Exception e) {
            JkLog.warn(e.getMessage());
            JkLog.warn("Cannot find latest version for '%s, choose default : %s ", moduleCoordinate, defaultVersion);
            return defaultVersion;
        }
    }

    /**
     * Runs the scaffolding, meaning folder structure, build class, props file and .gitignore
     */
    protected void run() {

        JkLog.startTask("scaffold");

        // Create 'jeka-src' dir
        final Path jekaSrc = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        JkLog.verbose("Create %s", jekaSrc);

        JkUtilsPath.createDirectories(jekaSrc);
        createOrUpdateJekaProps();  // Create 'jeka.properties' file
        createOrUpdateGitIgnore();  // Create .gitignore
        createShellScripts();   // Shell scripts
        fileEntries.forEach(extraEntry -> extraEntry.write(baseDir));
        JkLog.endTask();;
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
    public final void removeFileEntriesStaringBy(Path pathPrefix) {
        for (ListIterator<JkFileEntry> it = fileEntries.listIterator(); it.hasNext();) {
            JkFileEntry entry = it.next();
            if (entry.relativePath.startsWith(pathPrefix)) {
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

    private void createOrUpdateJekaProps() {
        StringBuilder sb = new StringBuilder();
        if (!JkUtilsString.isBlank(jekaDistribLocation)) {
            sb.append("jeka.distrib.location=" + jekaDistribLocation + "\n");

            // -- we need to specify a version if distribution location is empty
        } else  {
            String effectiveJekaVersion = !JkUtilsString.isBlank(jekaVersion) ? jekaVersion : lastJekaVersion();

            // TODO remove this check when 0.11.x will be released.
            if (!effectiveJekaVersion.startsWith("0.10.")) {
                sb.append("jeka.version=" + effectiveJekaVersion + "\n");
            }
        }

        if (!JkUtilsString.isBlank(this.jekaDistribRepo)) {
            sb.append("jeka.distrib.repo=" + jekaDistribRepo + "\n");
        }
        if (!JkUtilsString.isBlank(this.jekaPropsExtraContent)) {
            String content = jekaPropsExtraContent.replace("\\n", "\n");
            sb.append(content + "\n");
        }
        String sorted = sortJekaPropContent(sb.toString());

        JkPathFile jekaPropsFile  = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE));
        if (!jekaPropsFile.exists()) {
            jekaPropsFile.fetchContentFrom(JkScaffold.class.getResource(JkConstants.PROPERTIES_FILE));
        }
        jekaPropsFile.write(sorted, StandardOpenOption.APPEND);
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
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("jeka.bat");
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

    private String sortJekaPropContent(String originalContent) {
        List<String> sorted = Arrays.stream(originalContent.split("\n"))
                .filter(line -> !line.trim().startsWith("#"))
                .filter(line -> line.contains("="))
                .sorted(new PropComparator())
                .collect(Collectors.toCollection(LinkedList::new));
        String lastPrefix = "";
        for (ListIterator<String> it = sorted.listIterator(); it.hasNext();) {
            String line = it.next();
            String currentPrefix = extractPrefix(line);
            if (!currentPrefix.equals(lastPrefix)) {
               it.set("\n" + line);
            }
            lastPrefix = currentPrefix;
        }
        return String.join("\n", sorted);
    }

    private static String extractPrefix(String line) {
        String key = JkUtilsString.substringBeforeFirst(line, "=");
        String prefix = JkUtilsString.substringBeforeFirst(key, ".");
        if (!prefix.equals(key)) {
            return prefix + ".";
        }
        prefix = JkUtilsString.substringBeforeFirst(key, "#");
        if (!prefix.equals(key)) {
            return prefix + "#";
        }
        return key;
    }

    // Comparator that prioritize string starting with 'jeka.'
    private static class PropComparator implements Comparator<String> {

        public static final String JEKA_DOT = "jeka.";

        @Override
        public int compare(String o1, String o2) {
            int compareJekaDot = compareJakeDotPrefix(o1, o2);
            if (compareJekaDot != 0) {
                return compareJekaDot;
            }
            return o1.compareTo(o2);
        }

        private static int compareJakeDotPrefix(String o1, String o2) {
            if (o1.startsWith(JEKA_DOT + "version")) {
                return -1;
            }
            if (o2.startsWith(JEKA_DOT + "version")) {
                return 1;
            }
            if (o1.startsWith(JEKA_DOT)) {
                return o2.startsWith(JEKA_DOT) ? 0 : -1;
            }
            return o2.startsWith(JEKA_DOT) ? 1 : 0;
        }
    }

}
