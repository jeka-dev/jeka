package org.jerkar.api.crypto.pgp;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;

import org.jerkar.api.file.JkPathFile;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.tool.JkConstants;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class PgpUtilsTest {

    @Test
    public void testSignAndVerify() throws Exception {
        final JkPathFile path = JkPathFile.of(Paths.get(JkConstants.OUTPUT_PATH +
                "/test-out/signature.asm")).createIfNotExist();
        PgpUtils.sign(sample(), PgpUtilsTest.class.getResourceAsStream("secring.gpg"),
                JkUtilsIO.outputStream(path.get().toFile(), false), "jerkar".toCharArray(), true);

        final boolean result = PgpUtils.verify(sample(), JkUtilsIO.inputStream(path.get().toFile()),
                PgpUtilsTest.class.getResourceAsStream("pubring.gpg"));
        System.out.println(result);

    }

    static InputStream sample() {
        return PgpUtilsTest.class.getResourceAsStream("sampleFileToSign.txt");
    }

}
