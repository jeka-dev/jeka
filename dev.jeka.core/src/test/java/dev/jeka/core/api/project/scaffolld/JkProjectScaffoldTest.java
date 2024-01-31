package dev.jeka.core.api.project.scaffolld;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import org.junit.Test;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JkProjectScaffoldTest {


    @Test
    public void scaffold_withBuildClass_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        JkProjectScaffold projectScaffold = JkProjectScaffold.of(project);
        projectScaffold.compileDeps.add("toto:titi:0.0.1");
        projectScaffold.runtimeDeps.add("foo:bar");
        projectScaffold
                .setTemplate(JkProjectScaffold.Template.BUILD_CLASS)
                .run();

        // check .gitIgnore
        String gitIgnoreContent = JkPathFile.of(baseDir.resolve(".gitIgnore")).readAsString();
        assertTrue(gitIgnoreContent.contains("/.jeka-work"));
        assertTrue(gitIgnoreContent.contains("/jeka-output"));

        // Check Build class is present
        assertTrue(Files.exists(baseDir.resolve(JkConstants.JEKA_SRC_DIR).resolve("Build.java")));

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
    public void scaffold_withProps_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        JkProjectScaffold.of(project)
            .setTemplate(JkProjectScaffold.Template.PROPS)
            .run();

        // Check build class is absent
        assertFalse(Files.exists(baseDir.resolve(JkConstants.JEKA_SRC_DIR).resolve("Build.java")));

        // Check default KBean is present
        String jekaContent = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE)).readAsString();
        assertTrue(jekaContent.contains(JkConstants.DEFAULT_KBEAN_PROP + "=project"));

        // cleanup
        // Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

    @Test
    public void scaffold_withSimpleLayout_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        project.flatFacade().setLayoutStyle(JkCompileLayout.Style.SIMPLE);

        JkProjectScaffold.of(project).setUseSimpleStyle(true).run();

        // Check project layout
        assertFalse(Files.isDirectory(baseDir.resolve("src/main/java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src")));
        assertTrue(Files.isDirectory(baseDir.resolve("test")));
        assertFalse(Files.isDirectory(baseDir.resolve("res")));

        // Check property is here
        String jekaContent = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE)).readAsString();
        assertTrue(jekaContent.contains("project#layout.style=SIMPLE"));

        // cleanup
        //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

}
