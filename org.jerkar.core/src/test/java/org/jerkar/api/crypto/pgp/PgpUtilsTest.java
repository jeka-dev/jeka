package org.jerkar.api.crypto.pgp;

import java.io.File;
import java.io.InputStream;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.junit.Test;

public class PgpUtilsTest {

    @Test
    public void testSignAndVerify() throws Exception {
        final File signatureFile = JkUtilsFile.createFileIfNotExist(new File(
                "build/output/test-out/signature.asm"));
        PgpUtils.sign(sample(), PgpUtilsTest.class.getResourceAsStream("secring.gpg"),
                JkUtilsIO.outputStream(signatureFile, false), "jerkar".toCharArray(), true);

        final boolean result = PgpUtils.verify(sample(), JkUtilsIO.inputStream(signatureFile),
                PgpUtilsTest.class.getResourceAsStream("pubring.gpg"));
        System.out.println(result);

    }

    static InputStream sample() {
        return PgpUtilsTest.class.getResourceAsStream("sampleFileToSign.txt");
    }

}
