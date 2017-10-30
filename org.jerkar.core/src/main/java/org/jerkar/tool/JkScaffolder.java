package org.jerkar.tool;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.tooling.JkCodeWriterForBuildClass;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsPath;

/**
 * Object that process scaffolding.
 */
public final class JkScaffolder {

    private final JkPathTree baseTree;

    private Supplier<String> mainBuildclassWriter;

    private final boolean embed;

    public final JkRunnables extraActions = JkRunnables.noOp();

    JkScaffolder(Path baseDir, boolean embed) {
        super();
        this.baseTree = JkPathTree.of(baseDir);
        this.mainBuildclassWriter = basicScaffoldedBuildClassCode();
        this.embed = embed;
    }

    /**
     * Runs the scaffolding.
     */
    public void run() {
        final Path def = baseTree.root().resolve(JkConstants.BUILD_DEF_DIR);
        JkUtilsPath.createDirectories(def);
        final Path buildClass = def.resolve("Build.java");
        JkUtilsPath.write(buildClass, mainBuildclassWriter.get().getBytes(Charset.forName("UTF-8")));
        if (embed) {
            JkUtilsIO.copyUrlToFile(JkScaffolder.class.getClassLoader().getResource("META-INF/bin/jerkar.bat"), baseTree.root().resolve("jerkar.bat").toFile());
            JkUtilsIO.copyUrlToFile(JkScaffolder.class.getClassLoader().getResource("META-INF/bin/jerkar"), baseTree.root().resolve("jerkar").toFile());
            JkUtilsPath.copy(JkLocator.jerkarJarPath(),
                    baseTree.root().resolve("build/boot").resolve(JkLocator.jerkarJarPath().getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
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
     * The #toString method ofMany the specified code writer will be used to
     * generate code.
     * Generally we use an instance ofMany {@link JkCodeWriterForBuildClass}
     */
    public JkScaffolder buildClassWriter(Supplier<String> codeWriter) {
        this.mainBuildclassWriter = codeWriter;
        return this;
    }

    /**
     * Returns the build class code writer ofMany this scaffolder.
     */
    @SuppressWarnings("unchecked")
    public <T> T buildClassCodeWriter() {
        return (T) this.mainBuildclassWriter;
    }


}
