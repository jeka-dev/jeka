package org.jerkar.tool;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.tooling.JkCodeWriterForBuildClass;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsPath;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final JkPathTree baseTree;

    private String buildClassCode;

    private final boolean embed;

    public final JkRunnables extraActions = JkRunnables.noOp();

    JkScaffolder(Path baseDir, boolean embed) {
        super();
        this.baseTree = JkPathTree.of(baseDir);
        this.buildClassCode = basicScaffoldedBuildClassCode();
        this.embed = embed;
    }

    /**
     * Runs the scaffolding.
     */
    public void run() {
        final Path def = baseTree.root().resolve(JkConstants.BUILD_DEF_DIR);
        JkUtilsPath.createDirectories(def);
        final Path buildClass = def.resolve("Build.java");
        JkUtilsPath.write(buildClass, buildClassCode.getBytes(Charset.forName("UTF-8")));
        if (embed) {
            JkUtilsIO.copyUrlToFile(JkScaffolder.class.getClassLoader().getResource("META-INF/bin/jerkar.bat"), baseTree.root().resolve("jerkar.bat"));
            JkUtilsIO.copyUrlToFile(JkScaffolder.class.getClassLoader().getResource("META-INF/bin/jerkar"), baseTree.root().resolve("jerkar"));
            JkUtilsPath.copy(JkLocator.jerkarJarPath(),
                    baseTree.root().resolve("build/boot").resolve(JkLocator.jerkarJarPath().getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        extraActions.run();
    }

    public void setBuildClassCode(String code) {
        this.buildClassCode = code;
    }

    private static String basicScaffoldedBuildClassCode() {
        final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
        codeWriter.extendedClass = "JkBuild";
        return codeWriter.get();
    }

}
