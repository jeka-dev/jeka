package dev.jeka.core.api.scaffold;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Object that process scaffolding.
 */
public final class JkScaffold {

    private final Path baseDir;

    private Supplier<String> jkClassCodeProvider;

    private String classFilename = "App.java";

    private JkDependencyResolver dependencyResolver;

    public final JkRunnables extraActions = JkRunnables.of();

    private String jekaVersion;

    private String jekaDistribRepo;

    private String jekaLocation;

    private String extraPropsExtraContent = "";

    private JkScaffold(Path baseDir) {
        super();
        this.jkClassCodeProvider = () -> "";
        this.baseDir= baseDir;
        dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());
    }

    public static JkScaffold of(Path baseDir) {
        return new JkScaffold(baseDir);
    }

    public JkScaffold setDependencyResolver(JkDependencyResolver jkDependencyResolver) {
        this.dependencyResolver = jkDependencyResolver;
        return this;
    }

    public JkScaffold addJekaPropsFileContent(String extraContent) {
        if (extraContent == null) {
            return this;
        }
        this.extraPropsExtraContent += extraContent;
        return this;
    }

    /**
     * Adds a property to scaffolded jeka.properties by specifying a string with format "prop.name=prop.value".
     */
    public JkScaffold addJekaPropValue(String propValue) {
        if (JkUtilsString.isBlank(propValue)) {
            return this;
        }
        extraPropsExtraContent += "\n" + propValue.trim();
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

    public JkScaffold setJekaLocation(String jekaLocation) {
        this.jekaLocation = jekaLocation;
        return this;
    }

    public JkScaffold setJekaClassCodeProvider(Supplier<String> codeProvider) {
        this.jkClassCodeProvider = codeProvider;
        return this;
    }

    public JkScaffold setClassFilename(String classFilename) {
        this.classFilename = classFilename;
        return this;
    }

    /**
     * Runs the scaffolding, meaning folder structure, build class, props file and .gitignore
     */
    public void run() {

        JkLog.startTask("Scaffolding");

        String effectiveJekaVersion = !JkUtilsString.isBlank(jekaVersion) ? jekaVersion : lastJekaVersion();

        // Create 'jeka-src' dir
        final Path jekaSrc = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        JkLog.info("Create " + jekaSrc);
        JkUtilsPath.createDirectories(jekaSrc);

        // Create build class if needed
        final Path appClass = jekaSrc.resolve(classFilename);
        if (!Files.exists(appClass)) {
            JkLog.info("Create " + appClass);
            String code = jkClassCodeProvider.get();
            if (!JkUtilsString.isBlank(code)) {
                if (code.contains("${jekaVersion}")) {
                    code = code.replace("${jekaVersion}", effectiveJekaVersion);
                }
                JkUtilsPath.write(appClass, code.getBytes(StandardCharsets.UTF_8));
            }
        }

        // Create 'jeka.properties' file
        JkPathFile jekaPropsFile = jekaPropsFile = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE));
        if (!jekaPropsFile.exists()) {
            jekaPropsFile.fetchContentFrom(JkScaffold.class.getResource(JkConstants.PROPERTIES_FILE));
        }

        if (!JkUtilsString.isBlank(jekaLocation)) {
            String line = "jeka.distrib.location=" + jekaLocation + "\n";
            jekaPropsFile.write(line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

        // we need to specify a version if distribution location is empty
        } else  {
            String line = "jeka.version=" + effectiveJekaVersion + "\n";
            jekaPropsFile.write(line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }

        if (!JkUtilsString.isBlank(this.jekaDistribRepo)) {
            String line = "jeka.distrib.repo=" + jekaDistribRepo + "\n";
            jekaPropsFile.write(line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }
        if (!JkUtilsString.isBlank(this.extraPropsExtraContent)) {
            String content = extraPropsExtraContent.replace("\\n", "\n");
            jekaPropsFile.write(content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }

        // Create .gitignore
        JkPathFile.of(baseDir.resolve(".gitignore"))
                        .fetchContentFrom(JkScaffold.class.getResource("gitignore.snippet"));

        /// Shell scripts
        createShellScripts();

        extraActions.run();
        JkLog.endTask();;
    }

    private String lastJekaVersion() {
        List<String> versions = dependencyResolver.searchVersions(JkInfo.JEKA_MODULE_ID).stream()
                .filter(version -> !JkVersion.of(version).isSnapshot())
                .collect(Collectors.toList());
        if (versions.isEmpty()) {
            JkLog.warn("Didn't find any version of " + JkInfo.JEKA_MODULE_ID + " in " + dependencyResolver);
            JkLog.warn("Will use current one : " + JkInfo.getJekaVersion());
            return JkInfo.getJekaVersion();
        }
        return versions.get(versions.size() -1);
    }

    private void createShellScripts() {
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("jeka.bat");
        JkUtilsAssert.state(Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
                "to generate shell scripts");
        JkLog.info("Create jeka.bat file");
        JkUtilsPath.copy(jekaBat, baseDir.resolve("jeka.bat"), StandardCopyOption.REPLACE_EXISTING);
        Path jekaShell = baseDir.resolve("jeka");
        if (Files.isDirectory(jekaShell)) {
            JkLog.warn("%s directory is still present. Cannot create jeka shell file in base directory.", jekaShell);
        } else  {
            JkLog.info("Create jeka shell file");
            JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("jeka"), jekaShell,
                    StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            JkPathFile.of(jekaShell).setPosixExecPermissions(true, true, true);
        }
    }

}
