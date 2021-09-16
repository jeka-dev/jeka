package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;
import org.apache.ivy.ant.IvyMakePom;
import org.apache.ivy.plugins.resolver.DependencyResolver;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final Path baseDir;

    private String jkClassCode;

    private String classFilename = "Commands.java";

    private String wrapperJekaVersion;

    private JkDependencyResolver dependencyResolver;

    private final JkRunnables extraActions = JkRunnables.of();

    private String cachedJekaVersion;

    JkScaffolder(Path baseDir) {
        super();
        this.jkClassCode = "";
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
        String finalCode = jkClassCode;
        if (jkClassCode.contains("${version}")) {
            final String version = JkUtilsString.isBlank(wrapperJekaVersion) ? jekaVersion() : wrapperJekaVersion;
            finalCode = jkClassCode.replace("${version}", version);
        }
        JkUtilsPath.write(buildClass, finalCode.getBytes(Charset.forName("UTF-8")));
        extraActions.run();
    }

    /**
     * Copies script an Jeka jar inside the project in order to be executable in embedded mode.
     */
    public void embed() {
        JkLog.info("Create shell files.");
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("jeka.bat");
        JkUtilsAssert.state(Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
                "to shell scripts");
        JkUtilsPath.copy(jekaBat, baseDir.resolve("jekaw.bat"), StandardCopyOption.REPLACE_EXISTING);
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("jeka"), baseDir.resolve("jekaw"),
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        final Path jekaJar = JkLocator.getJekaJarPath();
        final Path bootFolder = baseDir.resolve(JkConstants.JEKA_DIR + "/boot");
        JkUtilsPath.createDirectories(bootFolder);
        final Path target = bootFolder.resolve(jekaJar.getFileName());
        JkLog.info("Copy jeka jar to " + baseDir.relativize(target));
        JkUtilsPath.copy(jekaJar, target, StandardCopyOption.REPLACE_EXISTING);
        final String jarSourceName = "dev.jeka.jeka-core-sources.jar";
        final Path libSources = baseDir.resolve(JkConstants.JEKA_DIR + "/libs-sources");
        JkUtilsPath.createDirectories(libSources);
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("libs-sources/" + jarSourceName),
                libSources.resolve(jarSourceName), StandardCopyOption.REPLACE_EXISTING);
    }

    public void wrap() {
        JkLog.info("Create shell files.");
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("wrapper/jekaw.bat");
        JkUtilsAssert.state(Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
                "to shell scripts");
        JkUtilsPath.copy(jekaBat, baseDir.resolve("jekaw.bat"), StandardCopyOption.REPLACE_EXISTING);
        Path jekawPath = baseDir.resolve("jekaw");
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("wrapper/jekaw"), jekawPath,
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        JkPathFile.of(jekawPath).addExecPerm(true, true, true);
        final Path jekaWrapperJar = JkLocator.getJekaJarPath().getParent().resolve("dev.jeka.jeka-core-wrapper.jar");
        final Path wrapperFolder = baseDir.resolve(JkConstants.JEKA_DIR + "/wrapper");
        JkUtilsPath.createDirectories(wrapperFolder);
        final Path target = wrapperFolder.resolve(jekaWrapperJar.getFileName());
        JkLog.info("Copy jeka wrapper jar to " + baseDir.relativize(target));
        JkUtilsPath.copy(jekaWrapperJar, target, StandardCopyOption.REPLACE_EXISTING);
        final String version = JkUtilsString.isBlank(wrapperJekaVersion) ? jekaVersion() : wrapperJekaVersion;
        Path tempProps = JkUtilsPath.createTempFile("jeka-", ".properties");
        Path jekaPropertiesPath = wrapperFolder.resolve("jeka.properties");
        if (!Files.exists(jekaPropertiesPath)) {
            JkPathFile.of(tempProps)
                    .replaceContentBy(JkScaffolder.class.getResource("jeka.properties"))
                    .copyReplacingTokens(jekaPropertiesPath,
                            JkUtilsIterable.mapOf("${version}", version), Charset.forName("utf-8"))
                    .deleteIfExist();
        }
    }

    public void wrapDelegate(String delegateFolder) {
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
                    .addExecPerm(true, true, true);
        }
    }

    public void setJekaClassCode(String code) {
        this.jkClassCode = code;
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
        List<String> allVersions = dependencyResolver.searchVersions(JkModuleId.of(JkInfo.JEKA_MODULE_ID));
        final List<String> versions = allVersions.stream()
                .filter(version -> version.endsWith(".RELEASE"))
                .collect(Collectors.toList());
        if (versions.isEmpty()) {
            JkLog.warn("Didn't find any version of " + JkInfo.JEKA_MODULE_ID + " in " + dependencyResolver);
            JkLog.warn("Will use current one : " + JkInfo.getJekaVersion());
            return JkInfo.getJekaVersion();
        }
        cachedJekaVersion = versions.get(versions.size() -1);
        return cachedJekaVersion;
    }
}
