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
import java.util.LinkedList;
import java.util.List;
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

    protected JkRepoSet getDownloadRepos() {
        return downloadRepos;
    }

    /**
     * Runs the scaffolding, meaning folder structure, build class, props file and .gitignore
     */
    protected void run() {

        JkLog.startTask("Scaffolding");

        // Create 'jeka-src' dir
        final Path jekaSrc = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        JkLog.info("Create " + jekaSrc);

        JkUtilsPath.createDirectories(jekaSrc);
        createOrUpdateJekaProps();  // Create 'jeka.properties' file
        createOrUpdateGitIgnore();  // Create .gitignore
        createShellScripts();   // Shell scripts
        fileEntries.forEach(extraEntry -> extraEntry.write(baseDir));

        JkLog.endTask();;
    }

    protected final String lastJekaVersion() {
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

    protected static String readResource(Class<?> clazz, String resourceName) {
        return JkUtilsIO.read(clazz.getResource(resourceName));
    }

    protected final void addFileEntry(String relativePath, String content) {
        JkFileEntry fileEntry = JkFileEntry.of(Paths.get(relativePath), content);
        this.fileEntries.add(fileEntry);
    }

    private void createOrUpdateJekaProps() {
        JkPathFile jekaPropsFile  = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE));
        if (!jekaPropsFile.exists()) {
            jekaPropsFile.fetchContentFrom(JkScaffold.class.getResource(JkConstants.PROPERTIES_FILE));
        }

        if (!JkUtilsString.isBlank(jekaDistribLocation)) {
            String line = "jeka.distrib.location=" + jekaDistribLocation + "\n";
            jekaPropsFile.write(line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

            // -- we need to specify a version if distribution location is empty
        } else  {
            String effectiveJekaVersion = !JkUtilsString.isBlank(jekaVersion) ? jekaVersion : lastJekaVersion();
            String line = "jeka.version=" + effectiveJekaVersion + "\n";
            jekaPropsFile.write(line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }

        if (!JkUtilsString.isBlank(this.jekaDistribRepo)) {
            String line = "jeka.distrib.repo=" + jekaDistribRepo + "\n";
            jekaPropsFile.write(line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }
        if (!JkUtilsString.isBlank(this.jekaPropsExtraContent)) {
            String content = jekaPropsExtraContent.replace("\\n", "\n");
            jekaPropsFile.write(content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }
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
            JkLog.info("Create jeka.bat file");
            JkUtilsPath.copy(jekaBat, baseDir.resolve("jeka.bat"), StandardCopyOption.REPLACE_EXISTING);
        }
        Path jekaShell = JkLocator.getJekaHomeDir().resolve("jeka");
        if (Files.isDirectory(jekaShell)) {
            JkLog.warn("%s directory is still present. Cannot create jeka shell file in base directory.", jekaShell);
        } else if (Files.exists(jekaShell)) {
            JkLog.info("Create jeka shell file");
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

}
