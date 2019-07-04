package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

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
        Path jekaBat = JkLocator.getJekaHomeDir().resolve("jeka.bat");
        JkException.throwIf(!Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
                "to shell scripts");
        JkUtilsPath.copy(jekaBat, baseDir.resolve("jekaw.bat"), StandardCopyOption.REPLACE_EXISTING);
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("jeka"), baseDir.resolve("jekaw"),
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        Path jekaJar = JkLocator.getJekaJarPath();
        Path bootFolder = baseDir.resolve(JkConstants.JEKA_DIR + "/boot");
        JkUtilsPath.createDirectories(bootFolder);
        Path target = bootFolder.resolve(jekaJar.getFileName());
        JkLog.info("Copy jeka jar to " + baseDir.relativize(target));
        JkUtilsPath.copy(jekaJar, target, StandardCopyOption.REPLACE_EXISTING);
        String jarSourceName = "dev.jeka.jeka-core-sources.jar";
        Path libSources = baseDir.resolve(JkConstants.JEKA_DIR + "/libs-sources");
        JkUtilsPath.createDirectories(libSources);
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("libs-sources/" + jarSourceName),
                libSources.resolve(jarSourceName), StandardCopyOption.REPLACE_EXISTING);
    }

    public void wrap() {
        JkLog.info("Create shell files.");
        Path jekaBat = JkLocator.getJekaHomeDir().resolve("wrapper/jekaw.bat");
        JkException.throwIf(!Files.exists(jekaBat), "Jeka should be run from an installed version in order " +
                "to shell scripts");
        JkUtilsPath.copy(jekaBat, baseDir.resolve("jekaw.bat"), StandardCopyOption.REPLACE_EXISTING);
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("wrapper/jekaw"), baseDir.resolve("jekaw"),
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        Path jekaWrapperJar = JkLocator.getJekaJarPath().getParent().resolve("dev.jeka.jeka-core-wrapper.jar");
        Path wrapperFolder = baseDir.resolve(JkConstants.JEKA_DIR + "/wrapper");
        JkUtilsPath.createDirectories(wrapperFolder);
        Path target = wrapperFolder.resolve(jekaWrapperJar.getFileName());
        JkLog.info("Copy jeka wrapper jar to " + baseDir.relativize(target));
        JkUtilsPath.copy(jekaWrapperJar, target, StandardCopyOption.REPLACE_EXISTING);
        String jarSourceName = "dev.jeka.jeka-core-sources.jar";
        Path libSources = baseDir.resolve(JkConstants.JEKA_DIR + "/libs-sources");
        JkUtilsPath.createDirectories(libSources);
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("libs-sources/" + jarSourceName),
                libSources.resolve(jarSourceName), StandardCopyOption.REPLACE_EXISTING);
        Properties properties = new Properties();
        properties.setProperty("jeka.version", JkInfo.getJekaVersion());
        try {
            properties.store(JkUtilsIO.outputStream(wrapperFolder.resolve("jeka.properties").toFile(), false), "");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
}
