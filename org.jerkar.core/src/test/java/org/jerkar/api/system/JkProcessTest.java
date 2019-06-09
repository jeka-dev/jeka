package org.jerkar.api.system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by angibaudj on 25-07-17.
 */
public class JkProcessTest {

    @Test
    public void dirOnWindows() throws Exception {

        if (JkUtilsSystem.IS_WINDOWS) {
            Path toto = Paths.get(JkProcessTest.class.getResource( "toto").toURI());
            Path totoWithSpaces = Paths.get(JkProcessTest.class.getResource("toto with spaces").toURI());
            Path parent = toto.getParent();

            Assert.assertTrue(Files.exists(parent));
            //new ProcessBuilder().command("explorer", parent.getAbsolutePath()).start().waitFor();
            JkProcess.of("find", "a string", totoWithSpaces.toAbsolutePath().toString()).runSync();
        }
    }

}
