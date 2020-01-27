package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.depmanagement.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final Path baseDir;

    private String commandClassCode;

    private String classFilename = "Commands.java";


    private final JkRunnables extraActions = JkRunnables.noOp();

    JkScaffolder(Path baseDir) {
        super();
        this.commandClassCode = "";
        this.baseDir= baseDir;
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
        JkUtilsPath.write(buildClass, commandClassCode.getBytes(Charset.forName("UTF-8")));
        extraActions.run();
    }

    /**
     * Copies script an Jeka jar inside the project in order to be executable in embedded mode.
     */
    public void embed() {
        JkLog.info("Create shell files.");
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("jeka.bat");
        JkException.throwIf(!Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
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

    public void wrap(JkDependencyResolver dependencyResolver) {
        JkLog.info("Create shell files.");
        final Path jekaBat = JkLocator.getJekaHomeDir().resolve("wrapper/jekaw.bat");
        JkException.throwIf(!Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
                "to shell scripts");
        JkUtilsPath.copy(jekaBat, baseDir.resolve("jekaw.bat"), StandardCopyOption.REPLACE_EXISTING);
        Path jekawPath = baseDir.resolve("jekaw");
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("wrapper/jekaw"), jekawPath,
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        Set<PosixFilePermission> perms = null;
        try {
            perms = Files.getPosixFilePermissions(jekawPath);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(jekawPath, perms);
        } catch (UnsupportedOperationException e) {
            // Windows system
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final Path jekaWrapperJar = JkLocator.getJekaJarPath().getParent().resolve("dev.jeka.jeka-core-wrapper.jar");
        final Path wrapperFolder = baseDir.resolve(JkConstants.JEKA_DIR + "/wrapper");
        JkUtilsPath.createDirectories(wrapperFolder);
        final Path target = wrapperFolder.resolve(jekaWrapperJar.getFileName());
        JkLog.info("Copy jeka wrapper jar to " + baseDir.relativize(target));
        JkUtilsPath.copy(jekaWrapperJar, target, StandardCopyOption.REPLACE_EXISTING);
        final Properties properties = new Properties();
        final String version = jekaVersion(dependencyResolver);
        Path tempProps = JkUtilsPath.createTempFile("jeka-", ".properties");
        JkPathFile.of(tempProps)
                .replaceContentBy(JkScaffolder.class.getResource("jeka.properties"))
                .copyReplacingTokens(wrapperFolder.resolve("jeka.properties"),
                        JkUtilsIterable.mapOf("${version}", version), Charset.forName("utf-8"))
                .deleteIfExist();
    }

    public void setCommandClassCode(String code) {
        this.commandClassCode = code;
    }

    public void setClassFilename(String classFilename) {
        this.classFilename = classFilename;
    }

    public JkRunnables getExtraActions() {
        return extraActions;
    }

    private String jekaVersion(JkDependencyResolver dependencyResolver) {
        final List<String> versions = dependencyResolver.searchVersions(JkModuleId.of(JkInfo.JEKA_MODULE_ID)).stream()
                .filter(version -> version.endsWith(".RELEASE"))
                .collect(Collectors.toList());
        if (versions.isEmpty()) {
            JkLog.warn("Didn't find any version of " + JkInfo.JEKA_MODULE_ID + " in " + dependencyResolver);
            JkLog.warn("Will use current one : " + JkInfo.getJekaVersion());
            return JkInfo.getJekaVersion();
        }
        return versions.get(versions.size() -1);
    }
}
