package dev.jeka.plugins.springboot;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.builtins.self.SelfKBean;
import org.junit.Test;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpringbootSelfScaffoldTest {

    @Test
    public void scaffold_withBuildClass_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        SelfKBean.SelfScaffoldOptions options = new SelfKBean.SelfScaffoldOptions();
        SpringbootSelfScaffold selfScaffold = new SpringbootSelfScaffold(baseDir, options);
        selfScaffold.run();


        // cleanup
        Desktop.getDesktop().open(baseDir.toFile());
        //JkPathTree.of(baseDir).deleteRoot();
    }



}
