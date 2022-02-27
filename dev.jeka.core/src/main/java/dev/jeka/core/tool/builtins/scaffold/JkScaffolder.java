package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.depmanagement.JkModuleId;
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

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Supplier;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final Path baseDir;

    private Supplier<String> jkClassCodeProvider;

    private String classFilename = "Build.java";

    private String wrapperJekaVersion;

    private JkDependencyResolver dependencyResolver;

    private final JkRunnables extraActions = JkRunnables.of();

    private String cachedJekaVersion;

    JkScaffolder(Path baseDir) {
        super();
        this.jkClassCodeProvider = () -> "";
        this.baseDir= baseDir;
        dependencyResolver = JkDependencyResolver.of().addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral());
    }


    public JkScaffolder setWrapperJekaVersion(String wrapperJekaVersion) {
        this.wrapperJekaVersion = wrapperJekaVersion;
        return this;
    }

    public JkScaffolder setDependencyResolver(JkDependencyResolver jkDependencyResolver) {
        this.dependencyResolver = jkDependencyResolver;
        return this;
    }

    /**
     * Runs the scaffolding.
     */
    public void run() {
        final Path def = baseDir.resolve(JkConstants.DEF_DIR);
        JkUtilsPath.createDirectories(def);
        JkLog.info("Create " + def);
        final Path buildClass = def.resolve(classFilename);
        JkLog.info("Create " + buildClass);
        String code = jkClassCodeProvider.get();
        if (code.contains("${jekaVersion}")) {
            final String version = JkUtilsString.isBlank(wrapperJekaVersion) ? jekaVersion() : wrapperJekaVersion;
            code = code.replace("${jekaVersion}", version);
        }
        JkUtilsPath.write(buildClass, code.getBytes(Charset.forName("UTF-8")));
        JkPathFile.of(baseDir.resolve(JkConstants.JEKA_DIR).resolve(JkConstants.PROJECT_PROPERTIES))
                .fetchContentFrom(JkScaffolder.class.getResource(JkConstants.PROJECT_PROPERTIES));
        JkPathFile.of(baseDir.resolve(JkConstants.JEKA_DIR).resolve(JkConstants.CMD_PROPERTIES))
                .fetchContentFrom(JkScaffolder.class.getResource(JkConstants.CMD_PROPERTIES));
        Path manualHtml = JkLocator.getJekaHomeDir().resolve("doc/reference-guide.html");
        if (Files.exists(manualHtml)) {
            JkPathFile.of(manualHtml).copyToDir(baseDir.resolve("jeka"));
        }
        JkUtilsPath.createDirectories(baseDir.resolve(JkConstants.JEKA_DIR).resolve("boot"));
        extraActions.run();
    }

    public void createStandardWrapperStructure() {

        JkLog.info("Create shell files.");
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("wrapper/jekaw.bat");
        JkUtilsAssert.state(Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
                "to generate shell scripts");
        JkUtilsPath.copy(jekaBat, baseDir.resolve("jekaw.bat"), StandardCopyOption.REPLACE_EXISTING);
        Path jekawPath = baseDir.resolve("jekaw");
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("wrapper/jekaw"), jekawPath,
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        JkPathFile.of(jekawPath).setPosixExecPermissions(true, true, true);

        final Path jekaWrapperJar = JkLocator.getJekaJarPath().getParent().resolve("dev.jeka.jeka-core-wrapper.jar");
        final Path wrapperFolder = baseDir.resolve(JkConstants.JEKA_DIR + "/wrapper");
        JkUtilsPath.createDirectories(wrapperFolder);
        final Path target = wrapperFolder.resolve(jekaWrapperJar.getFileName());
        JkLog.info("Copy jeka wrapper jar to " + baseDir.relativize(target));
        JkUtilsPath.copy(jekaWrapperJar, target, StandardCopyOption.REPLACE_EXISTING);
        final String version = JkUtilsString.isBlank(wrapperJekaVersion) ? jekaVersion() : wrapperJekaVersion;
        Path tempProps = JkUtilsPath.createTempFile("jeka-", ".properties");
        Path jekaPropertiesPath = wrapperFolder.resolve("wrapper.properties");
        if (!Files.exists(jekaPropertiesPath)) {
            JkPathFile.of(tempProps)
                    .fetchContentFrom(JkScaffolder.class.getResource("wrapper.properties"))
                    .copyReplacingTokens(jekaPropertiesPath,
                            JkUtilsIterable.mapOf("${version}", version), Charset.forName("utf-8"))
                    .deleteIfExist();
        }
    }

    public void createWrapperStructureWithDelagation(String delegateFolder) {
        JkPathFile newBatFile = JkPathFile.of(baseDir.resolve("jekaw.bat"));
        JkPathFile newShellFile = JkPathFile.of(baseDir.resolve("jekaw"));
        Path batDelegate = baseDir.resolve(delegateFolder).resolve("jekaw.bat");
        if (!Files.exists(batDelegate) && JkUtilsSystem.IS_WINDOWS) {
            throw new IllegalArgumentException("Cannot find file " + batDelegate);
        } else {
            String content = delegateFolder + "\\jekaw %*";
            content = content.replace('/', '\\');
            newBatFile.deleteIfExist().createIfNotExist().write(content.getBytes(Charset.forName("utf8")));
        }
        Path shellDelegate = baseDir.resolve(delegateFolder).resolve("jekaw");
        if (!Files.exists(shellDelegate) && !JkUtilsSystem.IS_WINDOWS) {
            throw new IllegalStateException("Cannot find file " + batDelegate);
        } else {
            String content ="#!/bin/sh\n\n" + delegateFolder.replace('\\', '/') + "/jekaw $@";
            newShellFile.deleteIfExist().createIfNotExist().write(content.getBytes(Charset.forName("utf8")))
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

    public JkRunnables getExtraActions() {
        return extraActions;
    }

    private String jekaVersion() {
        if (cachedJekaVersion != null) {
            return cachedJekaVersion;
        }
        if (!JkVersion.of(JkInfo.getJekaVersion()).isSnapshot()) {
            cachedJekaVersion = JkInfo.getJekaVersion();
            return cachedJekaVersion;
        }
        List<String> versions = dependencyResolver.searchVersions(JkModuleId.of(JkInfo.JEKA_MODULE_ID));
        if (versions.isEmpty()) {
            JkLog.warn("Didn't find any version of " + JkInfo.JEKA_MODULE_ID + " in " + dependencyResolver);
            JkLog.warn("Will use current one : " + JkInfo.getJekaVersion());
            return JkInfo.getJekaVersion();
        }
        cachedJekaVersion = versions.get(versions.size() -1);
        return cachedJekaVersion;
    }
}
