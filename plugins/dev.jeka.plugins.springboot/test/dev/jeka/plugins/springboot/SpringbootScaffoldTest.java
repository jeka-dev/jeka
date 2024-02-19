package dev.jeka.plugins.springboot;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.base.JkBaseScaffold;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpringbootScaffoldTest {

    @Test
    public void scaffoldProject_withBuildClass_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        JkProjectScaffold projectScaffold = JkProjectScaffold.of(project);
        projectScaffold.compileDeps.add("toto:titi:0.0.1");
        projectScaffold.runtimeDeps.add("foo:bar");
        projectScaffold
                .setTemplate(JkProjectScaffold.Template.BUILD_CLASS)
                .setTemplate(JkProjectScaffold.Template.BUILD_CLASS)
                .addCustomizer(SpringbootScaffold::adapt)
                .run();

        // check .gitIgnore
        String gitIgnoreContent = JkPathFile.of(baseDir.resolve(".gitIgnore")).readAsString();
        assertTrue(gitIgnoreContent.contains("/.jeka-work"));
        assertTrue(gitIgnoreContent.contains("/jeka-output"));

        // Check Build class is present
        assertTrue(Files.exists(baseDir.resolve(JkConstants.JEKA_SRC_DIR).resolve("Build.java")));

        // Check project layout
        assertTrue(Files.exists(baseDir.resolve("src/main/java/app/Application.java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/main/resources")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/test/java")));
        assertTrue(Files.isDirectory(baseDir.resolve("src/test/resources")));

        // cleanup
        //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

    @Test
    public void scaffoldProject_PropsWithSimpleLayout_ok() throws IOException {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        project.flatFacade().setLayoutStyle(JkCompileLayout.Style.SIMPLE);
        JkProjectScaffold.of(project)
                .setTemplate(JkProjectScaffold.Template.PROPS)
                .setUseSimpleStyle(true)
                .addCustomizer(SpringbootScaffold::adapt)
                .run();

        // Check project layout
        assertFalse(Files.isDirectory(baseDir.resolve("src/main/java")));
        assertTrue(Files.exists(baseDir.resolve("src/app//Application.java")));
        assertFalse(Files.isDirectory(baseDir.resolve("res")));

        // Check default KBean is present
        String jekaContent = JkPathFile.of(baseDir.resolve(JkConstants.PROPERTIES_FILE)).readAsString();
        assertTrue(jekaContent.contains(JkConstants.DEFAULT_KBEAN_PROP + "=project"));

        // cleanup
        //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

    @Test
    public void scaffoldProject_buildClassWithSimpleLayout_ok() throws IOException {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        JkProject project = JkProject.of().setBaseDir(baseDir);
        project.flatFacade().setLayoutStyle(JkCompileLayout.Style.SIMPLE);

        JkProjectScaffold.of(project)
                .setTemplate(JkProjectScaffold.Template.BUILD_CLASS)
                .setUseSimpleStyle(true)
                .addCustomizer(SpringbootScaffold::adapt)
                .run();

        // Check project layout
        assertFalse(Files.isDirectory(baseDir.resolve("src/main/java")));
        assertTrue(Files.exists(baseDir.resolve("src/app//Application.java")));
        assertFalse(Files.isDirectory(baseDir.resolve("res")));

        // cleanup
        //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

    @Test
    public void scaffoldSelf_withBuildClass_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        BaseKBean.BaseScaffoldOptions options = new BaseKBean.BaseScaffoldOptions();
        JkBaseScaffold.of(baseDir, options)
                .addCustomizer(SpringbootScaffold::adapt)
                .run();

        // Should not include script class defined by default
        assertFalse(Files.exists(baseDir.resolve(JkBaseScaffold.SCRIPT_CLASS_PATH)));

        // cleanup
        //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

}
