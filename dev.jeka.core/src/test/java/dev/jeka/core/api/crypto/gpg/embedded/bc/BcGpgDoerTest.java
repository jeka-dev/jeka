package dev.jeka.core.api.crypto.gpg.embedded.bc;

import dev.jeka.core.api.crypto.gpg.JkGpgTest;
import dev.jeka.core.api.utils.JkUtilsIO;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("javadoc")
public class BcGpgDoerTest {

    @Test
    public void testSignAndVerify() throws Exception {
        Path asmSignature = Files.createTempFile("jeka", ".asm");
        InputStream secring = JkGpgTest.class.getResourceAsStream("secring.gpg");
        BcGpgDoer.sign(sample(), secring, "",
                JkUtilsIO.outputStream(asmSignature.toFile(), false), "jeka".toCharArray(), true);
        final boolean result = BcGpgDoer.verify(sample(), JkUtilsIO.inputStream(asmSignature.toFile()),
                JkGpgTest.class.getResourceAsStream("pubring.gpg"));
        System.out.println(result);
    }

    private static InputStream sample() {
        return JkGpgTest.class.getResourceAsStream("sampleFileToSign.txt");
    }

}
