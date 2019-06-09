package dev.jeka.core.api.crypto.pgp;

import java.io.InputStream;
import java.nio.file.Paths;

import org.jerkar.api.file.JkPathFile;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkConstants;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class PgpUtilsTest {

    @Test
    public void testSignAndVerify() throws Exception {
        final JkPathFile path = JkPathFile.of(Paths.get(JkConstants.OUTPUT_PATH +
                "/test-out/signature.asm")).createIfNotExist();
        PgpUtils.sign(sample(), PgpUtilsTest.class.getResourceAsStream("secring.gpg"), "",
                JkUtilsIO.outputStream(path.get().toFile(), false), "jerkar".toCharArray(), true);

        final boolean result = PgpUtils.verify(sample(), JkUtilsIO.inputStream(path.get().toFile()),
                PgpUtilsTest.class.getResourceAsStream("pubring.gpg"));
        System.out.println(result);

    }

    static InputStream sample() {
        return PgpUtilsTest.class.getResourceAsStream("sampleFileToSign.txt");
    }

}
