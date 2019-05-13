package org.jerkar.api.crypto.pgp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.tool.JkConstants;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkPgpTest {

    @Test
    public void testSign() throws Exception {
        final Path pubFile = Paths.get(JkPgpTest.class.getResource("pubring.gpg").toURI());
        final Path secringFile = Paths.get(JkPgpTest.class.getResource("secring.gpg").toURI());
        final JkPgp pgp = JkPgp.of(pubFile, secringFile, "jerkar");
        final Path signatureFile = Paths.get(JkConstants.OUTPUT_PATH+ "/test-out/signature.asm");
        if (!Files.exists(signatureFile)) {
            Files.createDirectories(signatureFile.getParent());
            Files.createFile(signatureFile);
        }
        final Path sampleFile = Paths.get(JkPgpTest.class.getResource("sampleFileToSign.txt").toURI());
        Path signature = pgp.sign(sampleFile);
        System.out.println("Signature file : " + signature);
    }

    @Test(expected = RuntimeException.class)
    public void testSignWithBadPassword() throws Exception {
        final Path pubFile = Paths.get(JkPgpTest.class.getResource("pubring.gpg").toURI());
        final Path secringFile = Paths.get(JkPgpTest.class.getResource("secring.gpg").toURI());
        final JkPgp pgp = JkPgp.of(pubFile, secringFile, "badPassword");
        final Path signatureFile = Paths.get(JkConstants.OUTPUT_PATH + "/test-out/signature-fake.asm");
        if (!Files.exists(signatureFile)) {
            Files.createDirectories(signatureFile.getParent());
            Files.createFile(signatureFile);
        }
        final Path sampleFile = Paths.get(JkPgpTest.class.getResource("sampleFileToSign.txt").toURI());
        pgp.withSecretRingPassword("bad password").sign(sampleFile, signatureFile);
        Assert.assertTrue(pgp.verify(sampleFile, signatureFile));
    }

}
