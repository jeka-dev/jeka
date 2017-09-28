package org.jerkar.tool;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.tooling.JkCodeWriterForBuildClass;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsPath;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final JkFileTree baseTree;

    private Supplier<String> mainBuildclassWriter;

    private final boolean embed;

    public final JkRunnables extraActions = JkRunnables.noOp();

    JkScaffolder(Path baseDir, boolean embed) {
        super();
        this.baseTree = JkFileTree.of(baseDir);
        this.mainBuildclassWriter = basicScaffoldedBuildClassCode();
        this.embed = embed;
    }

    /**
     * Runs the scaffolding.
     */
    public void run() {
        final Path def = baseTree.rootPath().resolve(JkConstants.BUILD_DEF_DIR);
        JkUtilsPath.createDirectories(def);
        final Path buildClass = def.resolve("Build.java");
        JkUtilsPath.write(buildClass, mainBuildclassWriter.get().getBytes(Charset.forName("UTF-8")));
        if (embed) {
            JkUtilsIO.copyUrlToFile(JkScaffolder.class.getClassLoader().getResource("META-INF/bin/jerkar.bat"), baseTree.file("jerkar.bat"));
            JkUtilsIO.copyUrlToFile(JkScaffolder.class.getClassLoader().getResource("META-INF/bin/jerkar"), baseTree.file("jerkar"));
            JkUtilsFile.copyFileToDir(JkLocator.jerkarJarFile(), baseTree.file("build/boot"));
        }
        extraActions.run();
    }

    private static JkCodeWriterForBuildClass basicScaffoldedBuildClassCode() {
        final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
        codeWriter.extendedClass = "JkBuild";
        return codeWriter;
    }

    /**
     * Sets the the code writer to use to write build class code.
     * The #toString method of the specified code writer will be used to
     * generate code.
     * Generally we use an instance of {@link JkCodeWriterForBuildClass}
     */
    public JkScaffolder buildClassWriter(Supplier<String> codeWriter) {
        this.mainBuildclassWriter = codeWriter;
        return this;
    }

    /**
     * Returns the build class code writer of this scaffolder.
     */
    @SuppressWarnings("unchecked")
    public <T> T buildClassCodeWriter() {
        return (T) this.mainBuildclassWriter;
    }


}
