package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final Path baseDir;

    private String commandClassCode;

    private String classFilename = "Commands.java";

    private boolean embed;

    private final JkRunnables extraActions = JkRunnables.noOp();

    JkScaffolder(Path baseDir) {
        super();
        this.commandClassCode = "";
        this.baseDir= baseDir;
        this.embed = embed;
    }

    public void setEmbbed(boolean embed) {
        this.embed = embed;
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
