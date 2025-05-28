package dev.jeka.core.api.crypto.gpg.embedded.bc;

import dev.jeka.core.api.crypto.gpg.PgpRunner;
import dev.jeka.core.api.utils.JkUtilsIO;
import org.junit.jupiter.api.Test;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("javadoc")
class BcGpgDoerTest {

    @Test
    void testSignAndVerify() throws Exception {
        Path asmSignature = Files.createTempFile("jeka", ".asm");
        InputStream secring = PgpRunner.class.getResourceAsStream("secring.gpg");
        BcGpgDoer.sign(sample(), secring, "",
                JkUtilsIO.outputStream(asmSignature.toFile(), false), "jerkar".toCharArray());
        final boolean result = BcGpgDoer.verify(sample(), JkUtilsIO.inputStream(asmSignature.toFile()),
                PgpRunner.class.getResourceAsStream("pubring.gpg"));
        System.out.println(result);
    }

    @Test
    void createKey() {
        String asciiKey = JkUtilsIO.readAsString(PgpRunner.class.getResourceAsStream("jeka-unit-test-key.asc"));
        BcGpgDoer.createSecretKey(asciiKey);
    }

    private static InputStream sample() {
        return PgpRunner.class.getResourceAsStream("sampleFileToSign.txt");
    }

}
