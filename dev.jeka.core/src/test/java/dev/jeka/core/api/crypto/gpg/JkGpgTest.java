package dev.jeka.core.api.crypto.gpg;

import dev.jeka.core.tool.JkConstants;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("javadoc")
public class JkGpgTest {

    @Test
    public void testSign() throws Exception {
        final Path secringFile = Paths.get(JkGpgTest.class.getResource("secring.gpg").toURI());
        final JkGpgSigner pgp = JkGpgSigner.of(secringFile, "jerkar", "");
        final Path signatureFile = Paths.get(JkConstants.OUTPUT_PATH+ "/test-out/signature.asm");
        if (!Files.exists(signatureFile)) {
            Files.createDirectories(signatureFile.getParent());
            Files.createFile(signatureFile);
        }
        final Path sampleFile = Paths.get(JkGpgTest.class.getResource("sampleFileToSign.txt").toURI());
        Path signature = pgp.sign(sampleFile);
        System.out.println("Signature file : " + signature);
    }

    @Test(expected = RuntimeException.class)
    public void testSignWithBadPassword() throws Exception {
        final Path pubFile = Paths.get(JkGpgTest.class.getResource("pubring.gpg").toURI());
        final Path secringFile = Paths.get(JkGpgTest.class.getResource("secring.gpg").toURI());
        final JkGpgSigner signer = JkGpgSigner.of(secringFile, "badPassword", "");
        final Path signatureFile = Paths.get(JkConstants.OUTPUT_PATH + "/test-out/signature-fake.asm");
        if (!Files.exists(signatureFile)) {
            Files.createDirectories(signatureFile.getParent());
            Files.createFile(signatureFile);
        }
        final Path sampleFile = Paths.get(JkGpgTest.class.getResource("sampleFileToSign.txt").toURI());
        signer.sign(sampleFile, signatureFile);
        JkGpgVerifier verifier = JkGpgVerifier.of(pubFile);
        Assert.assertTrue(verifier.verify(sampleFile, signatureFile));
    }

}
