package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by angibaudj on 25-07-17.
 */
public class JkProcessTest {

    @Test
    public void dirOnWindows() throws Exception {
        //JkLog.setDecorator(JkLog.Style.BRACE);

        if (JkUtilsSystem.IS_WINDOWS) {
            Path toto = Paths.get(JkProcessTest.class.getResource( "toto").toURI());
            Path totoWithSpaces = Paths.get(JkProcessTest.class.getResource("toto with spaces").toURI());
            Path parent = toto.getParent();

            Assert.assertTrue(Files.exists(parent));
            JkProcess.of("find", "a string", totoWithSpaces.toAbsolutePath().toString())
                    //.setLogCommand(true)
                    .exec();
        }
    }

}
