package org.jerkar.tool.builtins.scaffold;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;

import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.tool.JkConstants;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final JkPathTree baseTree;

    private String buildClassCode;

    private boolean embed;

    public final JkRunnables extraActions = JkRunnables.noOp();

    JkScaffolder(Path baseDir, boolean embed) {
        super();
        this.baseTree = JkPathTree.of(baseDir);
        this.buildClassCode = "";
        this.embed = embed;
    }

    public void setEmbbed(boolean embed) {
        this.embed = embed;
    }

    /**
     * Runs the scaffolding.
     */
    public void run() {
        final Path def = baseTree.root().resolve(JkConstants.DEF_DIR);
        JkUtilsPath.createDirectories(def);
        JkLog.info("Create " + def);
        final Path buildClass = def.resolve("Build.java");
        JkLog.info("Create " + buildClass);
        JkUtilsPath.write(buildClass, buildClassCode.getBytes(Charset.forName("UTF-8")));
        if (embed) {
            JkLog.info("Create shell files.");
            JkUtilsIO.copyUrlToFile(JkScaffolder.class.getClassLoader().getResource("META-INF/bin/jerkar.bat"), baseTree.root().resolve("jerkar.bat"));
            JkUtilsIO.copyUrlToFile(JkScaffolder.class.getClassLoader().getResource("META-INF/bin/jerkar"), baseTree.root().resolve("jerkar"));
            Path jerkarJar = JkLocator.jerkarJarPath();
            Path bootFolder = baseTree.root().resolve("build/boot");
            JkUtilsPath.createDirectories(bootFolder);
            Path target = bootFolder.resolve(jerkarJar.getFileName());
            JkLog.info("Copy jerkar jar to " + baseTree.root().relativize(target));
            JkUtilsPath.copy(jerkarJar, target, StandardCopyOption.REPLACE_EXISTING);
        }
        extraActions.run();
    }

    public void setBuildClassCode(String code) {
        this.buildClassCode = code;
    }



}
