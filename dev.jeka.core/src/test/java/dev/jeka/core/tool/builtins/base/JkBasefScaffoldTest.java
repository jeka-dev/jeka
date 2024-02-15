package dev.jeka.core.tool.builtins.base;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.Test;

import java.nio.file.Path;

public class JkBasefScaffoldTest {

    @Test
    public void scaffold_app_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        BaseKBean.BaseScaffoldOptions options = new BaseKBean.BaseScaffoldOptions();
        options.kind = JkBasefScaffold.Kind.APP;
        JkBasefScaffold selfScaffold = JkBasefScaffold.of(baseDir, options);
        selfScaffold.run();

        // cleanup
         //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

    @Test
    public void scaffold_script_ok() throws Exception {
        Path baseDir = JkUtilsPath.createTempDirectory("jk-test-");
        BaseKBean.BaseScaffoldOptions options = new BaseKBean.BaseScaffoldOptions();
        options.kind = JkBasefScaffold.Kind.JEKA_SCRIPT;
        JkBasefScaffold selfScaffold = JkBasefScaffold.of(baseDir, options);
        selfScaffold.run();

        // cleanup
        //Desktop.getDesktop().open(baseDir.toFile());
        JkPathTree.of(baseDir).deleteRoot();
    }

}
