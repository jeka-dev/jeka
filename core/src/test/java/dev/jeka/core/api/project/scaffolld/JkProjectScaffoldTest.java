package dev.jeka.core.api.project.scaffolld;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JkProjectScaffoldTest {

    @Test
    void scaffold_regular_ok()  {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        JkProjectScaffold projectScaffold = JkProjectScaffold.of(project);
        projectScaffold.compileDeps.add("toto:titi:0.0.1");
        projectScaffold.runtimeDeps.add("foo:bar");
        projectScaffold
                .setKind(JkProjectScaffold.Kind.REGULAR)
                .run();

        // check .gitignore
        String gitIgnoreContent = JkPathFile.of(baseDir.resolve(".gitignore")).readAsString();
        assertTrue(gitIgnoreContent.contains("/.jeka-work"));
        assertTrue(gitIgnoreContent.contains("/jeka-output"));

        // Check Build class is present
        assertTrue(Files.exists(baseDir.resolve(JkConstants.JEKA_SRC_DIR).resolve("Custom.java")));

        // Check project layout
        assertTrue(Files.isDirectory(baseDir.resolve("src/main/java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/main/resources")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/test/java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/test/resources")));

        // cleanup
        // Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

    @Test
    void scaffold_withSimpleLayout_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        project.flatFacade.setLayoutStyle(JkCompileLayout.Style.SIMPLE);

        JkProjectScaffold.of(project).setUseSimpleStyle(true).run();

        // Check project layout
        assertFalse(Files.isDirectory(baseDir.resolve("src/main/java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src")));
        assertTrue(Files.isDirectory(baseDir.resolve("test")));
        assertTrue(Files.isDirectory(baseDir.resolve("res")));

        // Check property is here
        String jekaContent = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE)).readAsString();
        assertTrue(jekaContent.contains("@project.layout.style=SIMPLE"));

        // cleanup
        //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

}
