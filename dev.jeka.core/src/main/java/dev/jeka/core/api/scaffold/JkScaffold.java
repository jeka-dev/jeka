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

/**
 * Object that process scaffolding.
 */
public final class JkScaffold {

    private final Path baseDir;

    private Supplier<String> jkClassCodeProvider;

    private String classFilename = "Build.java";

    private String wrapperJekaVersion;

    private JkDependencyResolver dependencyResolver;

    public final JkRunnables extraActions = JkRunnables.of();

    private String cachedJekaVersion;

    private String localPropsExtraContent = "";

    private JkScaffold(Path baseDir) {
        super();
        this.jkClassCodeProvider = () -> "";
        this.baseDir= baseDir;
        dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());
    }

    public static JkScaffold of(Path baseDir) {
        return new JkScaffold(baseDir);
    }

    public JkScaffold setWrapperJekaVersion(String wrapperJekaVersion) {
        this.wrapperJekaVersion = wrapperJekaVersion;
        return this;
    }

    public JkScaffold setDependencyResolver(JkDependencyResolver jkDependencyResolver) {
        this.dependencyResolver = jkDependencyResolver;
        return this;
    }

    public JkScaffold addLocalPropsFileContent(String extraContent) {
        this.localPropsExtraContent += extraContent;
        return this;
    }

    /**
     * Runs the scaffolding, meaning folder structure, build class, props file and .gitignore
     */
    public void run() {

        // Create 'def' dir
        final Path def = baseDir.resolve(JkConstants.DEF_DIR);
        JkLog.info("Create " + def);
        JkUtilsPath.createDirectories(def);

        // Create build class if needed
        final Path buildClass = def.resolve(classFilename);
        JkLog.info("Create " + buildClass);
        String code = jkClassCodeProvider.get();
        if (!JkUtilsString.isBlank(code)) {
            if (code.contains("${jekaVersion}")) {
                final String version = JkUtilsString.isBlank(wrapperJekaVersion) ? jekaVersion() : wrapperJekaVersion;
                code = code.replace("${jekaVersion}", version);
            }
            JkUtilsPath.write(buildClass, code.getBytes(StandardCharsets.UTF_8));
        }

        // Create 'local.properties' file
        JkPathFile localPropsFile = JkPathFile.of(baseDir.resolve(JkConstants.JEKA_DIR).resolve(JkConstants.PROPERTIES_FILE))
                .fetchContentFrom(JkScaffold.class.getResource(JkConstants.PROPERTIES_FILE));
        if (!JkUtilsString.isBlank(this.localPropsExtraContent)) {
            String content = localPropsExtraContent.replace("\\n", "\n");
            localPropsFile.write(content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }

        // Create .gitignore
        JkPathFile.of(baseDir.resolve(JkConstants.JEKA_DIR).resolve(".gitignore"))
                        .fetchContentFrom(JkScaffold.class.getResource("gitignore.snippet"));
        extraActions.run();
    }

    /**
     * Creates wrapper files, meaning shell scripts, jar, and configuration file.
     */
    public void createStandardWrapperStructure() {

        // shell scripts
        JkLog.info("Create shell files.");
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("wrapper/jekaw.bat");
        JkUtilsAssert.state(Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
                "to generate shell scripts");
        JkUtilsPath.copy(jekaBat, baseDir.resolve("jekaw.bat"), StandardCopyOption.REPLACE_EXISTING);
        Path jekawPath = baseDir.resolve("jekaw");
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("jeka"), jekawPath,
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        JkPathFile.of(jekawPath).setPosixExecPermissions(true, true, true);

        // jar file
        final Path jekaWrapperJar = JkLocator.getJekaJarPath().getParent().resolve("dev.jeka.jeka-core-wrapper.jar");
        final Path wrapperFolder = baseDir.resolve(JkConstants.JEKA_DIR + "/wrapper");
        JkUtilsPath.createDirectories(wrapperFolder);
        final Path target = wrapperFolder.resolve(jekaWrapperJar.getFileName());
        JkLog.info("Copy jeka wrapper jar to " + baseDir.relativize(target));
        JkUtilsPath.copy(jekaWrapperJar, target, StandardCopyOption.REPLACE_EXISTING);
        final String version = JkUtilsString.isBlank(wrapperJekaVersion) ? jekaVersion() : wrapperJekaVersion;

        // wrapper.properties file
        Path tempProps = JkUtilsPath.createTempFile("jeka-", ".properties");
        Path jekaPropertiesPath = wrapperFolder.resolve("wrapper.properties");
        if (!Files.exists(jekaPropertiesPath)) {
            JkPathFile.of(tempProps)
                    .fetchContentFrom(JkScaffold.class.getResource("wrapper.properties"))
                    .copyReplacingTokens(jekaPropertiesPath,
                            JkUtilsIterable.mapOf("${version}", version), StandardCharsets.UTF_8)
                    .deleteIfExist();
        }
    }

    /**
     * Creates the files needed to delegate the wrapper to another module.
     * This is generally used in multi-modules projects to have a single
     * shared wrapper (and version) to manage.
     * It consists in creating shell files that simply invoke the wrapper from
     * another directory.
     */
    public void createWrapperStructureWithDelegate(String delegateFolder) {
        JkPathFile newBatFile = JkPathFile.of(baseDir.resolve("jekaw.bat"));
        JkPathFile newShellFile = JkPathFile.of(baseDir.resolve("jekaw"));
        Path batDelegate = baseDir.resolve(delegateFolder).resolve("jekaw.bat");
        if (!Files.exists(batDelegate) && JkUtilsSystem.IS_WINDOWS) {
            throw new IllegalArgumentException("Cannot find file " + batDelegate);
        } else {
            String content = delegateFolder + "\\jekaw %*";
            content = content.replace('/', '\\');
            newBatFile.deleteIfExist().createIfNotExist().write(content.getBytes(StandardCharsets.UTF_8));
        }
        Path shellDelegate = baseDir.resolve(delegateFolder).resolve("jekaw");
        if (!Files.exists(shellDelegate) && !JkUtilsSystem.IS_WINDOWS) {
            throw new IllegalStateException("Cannot find file " + batDelegate);
        } else {
            String content ="#!/bin/sh\n\n" + delegateFolder.replace('\\', '/') + "/jekaw $@";
            newShellFile.deleteIfExist().createIfNotExist().write(content.getBytes(StandardCharsets.UTF_8))
                    .setPosixExecPermissions(true, true, true);
        }
        JkLog.info("Shell scripts generated.");
    }

    public void setJekaClassCodeProvider(Supplier<String> codeProvider) {
        this.jkClassCodeProvider = codeProvider;
    }

    public void setClassFilename(String classFilename) {
        this.classFilename = classFilename;
    }

    private String jekaVersion() {
        if (cachedJekaVersion != null) {
            return cachedJekaVersion;
        }
        if (!JkVersion.of(JkInfo.getJekaVersion()).isSnapshot()) {
            cachedJekaVersion = JkInfo.getJekaVersion();
            return cachedJekaVersion;
        }
        List<String> versions = dependencyResolver.searchVersions(JkInfo.JEKA_MODULE_ID);
        if (versions.isEmpty()) {
            JkLog.warn("Didn't find any version of " + JkInfo.JEKA_MODULE_ID + " in " + dependencyResolver);
            JkLog.warn("Will use current one : " + JkInfo.getJekaVersion());
            return JkInfo.getJekaVersion();
        }
        cachedJekaVersion = versions.get(versions.size() -1);
        return cachedJekaVersion;
    }
}
