package org.jerkar.crypto.pgp;

import java.io.File;

import org.jerkar.utils.JkUtilsFile;
import org.junit.Assert;
import org.junit.Test;

public class JkPgpTest {

	@Test
	public void testSign() {
		final File pubFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("pubring.gpg"));
		final File secringFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("secring.gpg"));
		final JkPgp pgp = JkPgp.of(pubFile, secringFile);
		final File signatureFile = JkUtilsFile.createFileIfNotExist(new File(
				"build/output/test-out/signature.asm"));
		final File sampleFile = JkUtilsFile.fromUrl(JkPgpTest.class.getResource("sampleFileToSign.txt"));
		pgp.sign(sampleFile, signatureFile, "jerkar");
		final boolean result = pgp.verify(sampleFile, signatureFile);
		Assert.assertTrue(result);
	}


}
