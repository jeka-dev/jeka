package org.jerkar.api.system;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsSystem;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Created by angibaudj on 25-07-17.
 */
public class JkProcessTest {

    @Test
    public void dirOnWindows() throws Exception {

        if (JkUtilsSystem.IS_WINDOWS) {
            File toto = JkUtilsFile.resourceAsFile(JkProcessTest.class, "toto");
            File totoWithSpaces = JkUtilsFile.resourceAsFile(JkProcessTest.class, "toto with spaces");
            File parent = toto.getParentFile();

            Assert.assertTrue(parent.exists());
            //new ProcessBuilder().command("explorer", parent.getAbsolutePath()).start().waitFor();
            JkProcess.of("find", "a string", totoWithSpaces.getAbsolutePath()).runSync();
        }
    }

}
