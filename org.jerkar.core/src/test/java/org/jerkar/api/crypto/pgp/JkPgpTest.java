package org.jerkar.api.crypto.pgp;

import java.io.File;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Test;

public class JkPgpTest {

    @Test
    public void testSignAndVerify() {
	final File pubFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("pubring.gpg"));
	final File secringFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("secring.gpg"));
	final JkPgp pgp = JkPgp.of(pubFile, secringFile, "jerkar");
	final File signatureFile = JkUtilsFile.createFileIfNotExist(new File("build/output/test-out/signature.asm"));
	final File sampleFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("sampleFileToSign.txt"));
	pgp.sign(sampleFile, signatureFile);
	// final boolean result = pgp.verify(sampleFile, signatureFile);
	// Assert.assertTrue(result);
    }

    @Test(expected = RuntimeException.class)
    public void testSignWithBadSignature() {
	final File pubFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("pubring.gpg"));
	final File secringFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("secring.gpg"));
	final JkPgp pgp = JkPgp.of(pubFile, secringFile, "badPassword");
	final File signatureFile = JkUtilsFile
		.createFileIfNotExist(new File("build/output/test-out/signature-fake.asm"));
	final File sampleFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("sampleFileToSign.txt"));
	pgp.sign(sampleFile, signatureFile, "badPassword");
    }

}
