package dev.jeka.core.api.crypto.gpg.embedded.bc;

import dev.jeka.core.api.crypto.gpg.JkGpgTest;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkConstants;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Paths;

@SuppressWarnings("javadoc")
public class BcGpgDoerTest {

    @Test
    public void testSignAndVerify() throws Exception {
        final JkPathFile path = JkPathFile.of(Paths.get(JkConstants.OUTPUT_PATH +
                "/test-out/signature.asm")).createIfNotExist();
        BcGpgDoer.sign(sample(), JkGpgTest.class.getResourceAsStream("secring.gpg"), "",
                JkUtilsIO.outputStream(path.get().toFile(), false), "jerkar".toCharArray(), true);

        final boolean result = BcGpgDoer.verify(sample(), JkUtilsIO.inputStream(path.get().toFile()),
                JkGpgTest.class.getResourceAsStream("pubring.gpg"));
        System.out.println(result);

    }

    static InputStream sample() {
        return JkGpgTest.class.getResourceAsStream("sampleFileToSign.txt");
    }

}
