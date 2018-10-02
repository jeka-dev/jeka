package org.jerkar.api.crypto.pgp;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.JkConstants;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkPgpTest {

    @Test
    public void testSignAndVerify() throws Exception {
        final Path pubFile = Paths.get(JkPgpTest.class.getResource("pubring.gpg").toURI());
        final Path secringFile = Paths.get(JkPgpTest.class.getResource("secring.gpg").toURI());
        final JkPgp pgp = JkPgp.of(pubFile, secringFile, "jerkar");
        final Path signatureFile = Paths.get(JkConstants.OUTPUT_PATH+ "/test-out/signature.asm");
        if (!Files.exists(signatureFile)) {
            Files.createDirectories(signatureFile.getParent());
            Files.createFile(signatureFile);
        }
        final Path sampleFile = Paths.get(JkPgpTest.class.getResource("sampleFileToSign.txt").toURI());
        pgp.sign(sampleFile, signatureFile);
    }

    @Test(expected = RuntimeException.class)
    public void testSignWithBadSignature() throws Exception {
        final Path pubFile = Paths.get(JkPgpTest.class.getResource("pubring.gpg").toURI());
        final Path secringFile = Paths.get(JkPgpTest.class.getResource("secring.gpg").toURI());
        final JkPgp pgp = JkPgp.of(pubFile, secringFile, "badPassword");
        final Path signatureFile = Paths.get(JkConstants.OUTPUT_PATH + "/test-out/signature-fake.asm");
        if (!Files.exists(signatureFile)) {
            Files.createDirectories(signatureFile.getParent());
            Files.createFile(signatureFile);
        }
        final Path sampleFile = Paths.get(JkPgpTest.class.getResource("sampleFileToSign.txt").toURI());
        pgp.sign(sampleFile, signatureFile, "badPassword");
    }

}
