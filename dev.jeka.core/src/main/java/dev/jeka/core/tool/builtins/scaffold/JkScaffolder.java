package dev.jeka.core.tool.builtins.scaffold;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final JkPathTree baseTree;

    private String commandClassCode;

    private String classFilename = "Commands.java";

    private final JkRunnables extraActions = JkRunnables.noOp();

    JkScaffolder(Path baseDir) {
        super();
        this.baseTree = JkPathTree.of(baseDir);
        this.commandClassCode = "";
    }

    /**
     * Runs the scaffolding.
     */
    public void run() {
        final Path def = baseTree.getRoot().resolve(JkConstants.DEF_DIR);
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
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("jeka.bat"), baseTree.getRoot().resolve("jekaw.bat"),
                StandardCopyOption.REPLACE_EXISTING);
        JkUtilsPath.copy(JkLocator.getJekaHomeDir().resolve("jeka"), baseTree.getRoot().resolve("jekaw"),
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        Path jekaJar = JkLocator.getJekaJarPath();
        Path bootFolder = baseTree.getRoot().resolve(JkConstants.JEKA_DIR + "/boot");
        JkUtilsPath.createDirectories(bootFolder);
        Path target = bootFolder.resolve(jekaJar.getFileName());
        JkLog.info("Copy jeka jar to " + baseTree.getRoot().relativize(target));
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
