package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by angibaudj on 25-07-17.
 */
class JkProcessTest {

    @Test
    void dirOnWindows() throws Exception {
        //JkLog.setDecorator(JkLog.Style.BRACE);

        if (JkUtilsSystem.IS_WINDOWS) {
            Path toto = Paths.get(JkProcessTest.class.getResource( "toto").toURI());
            Path totoWithSpaces = Paths.get(JkProcessTest.class.getResource("toto with spaces").toURI());
            Path parent = toto.getParent();

            Assertions.assertTrue(Files.exists(parent));
            JkProcess.of("find", "a string", totoWithSpaces.toAbsolutePath().toString())
                    //.setLogCommand(true)
                    .exec();
        }
    }

}
