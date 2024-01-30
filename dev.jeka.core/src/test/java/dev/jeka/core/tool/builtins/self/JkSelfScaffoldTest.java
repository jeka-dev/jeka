package dev.jeka.core.tool.builtins.self;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.Test;

import java.awt.*;
import java.nio.file.Path;

public class JkSelfScaffoldTest {

    @Test
    public void scaffold_app_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        SelfKBean.SelfScaffoldOptions options = new SelfKBean.SelfScaffoldOptions();
        options.kind = JkSelfScaffold.Kind.APP;
        JkSelfScaffold selfScaffold = new JkSelfScaffold(baseDir, options);
        selfScaffold.run();

        // cleanup
         //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

    @Test
    public void scaffold_script_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        SelfKBean.SelfScaffoldOptions options = new SelfKBean.SelfScaffoldOptions();
        options.kind = JkSelfScaffold.Kind.JEKA_SCRIPT;
        JkSelfScaffold selfScaffold = new JkSelfScaffold(baseDir, options);
        selfScaffold.run();

        // cleanup
        Desktop.getDesktop().open(baseDir.toFile());
        //JkPathTree.of(baseDir).deleteRoot();
    }

}
