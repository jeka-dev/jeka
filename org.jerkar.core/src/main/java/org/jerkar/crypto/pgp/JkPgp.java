package org.jerkar.crypto.pgp;

import java.io.File;

import org.jerkar.JkClassLoader;
import org.jerkar.utils.JkUtilsAssert;
import org.jerkar.utils.JkUtilsReflect;

/**
 * Provides method for signing and verify signature with PGP.<p>
 * When constructing JkPgp, you can provide a secret key ring, a public key ring or both.
 * <br/>Note that you need the secret ring for signing and the public ring for verifying.
 * 
 * @author Jerome Angibaud
 */
public final class JkPgp {

	private final File pubRing;

	private final File secRing;

	/**
	 * Creates a JkPgp with the specified public and secret ring.
	 */
	public static JkPgp of(File pubRing, File secRing) {
		return new JkPgp(pubRing, secRing);
	}

	/**
	 * Creates a JkPgp with the specified public key ring.
	 */
	public static JkPgp ofPublicRing(File pubRing) {
		return of(pubRing, null);
	}

	/**
	 * Creates a JkPgp with the specified secret key ring.
	 */
	public static JkPgp ofSecretRing(File secRing) {
		return of(null, secRing);
	}

	// We don't want to add Bouncycastle in the Jerkar classpath, so we create one
	// just for launching the Bouncy castle methods.
	private static Class<?> PGPUTILS_CLASS = JkClassLoader.current().childWithUrl(
			JkPgp.class.getResource("bouncycastle-all-152.jar")).load(PgpUtils.class);


	private JkPgp(File pubRing, File secRing) {
		super();
		this.pubRing = pubRing;
		this.secRing = secRing;
	}

	/**
	 * Signs the specified file with the first secret key of this key ring. The password is necessary
	 * if the secret key is protected.
	 * @param fileToSign The file to sign
	 * @param output The file where will be written the signature (no need to exist priorly)
	 * @param password password of the secret key
	 */
	public void sign(File fileToSign, File output, String password) {
		JkUtilsAssert.isTrue(secRing != null, "You must supply a secret ring file (as secring.gpg) to sign files");
		JkUtilsReflect.invokeStaticMethod(PGPUTILS_CLASS, "sign",
				fileToSign, secRing, output, password.toCharArray(), true);
	}

	/**
	 * Signs the specified files in a detached signature file which will have the same name of
	 * the signed file plus ".asc" suffix.
	 */
	public void sign(String password, File ...filesToSign) {
		for (final File file : filesToSign) {
			if (!file.exists()) {
				continue;
			}
			final File signatureFile = new File(file.getParent(), file.getName() + ".asc");
			sign(file, signatureFile, password);
		}
	}

	/**
	 * Verifies the specified file against the specified signature.
	 */
	public boolean verify(File fileToVerify, File signature) {
		JkUtilsAssert.isTrue(pubRing != null, "You must supply a public ring file (as pubring.gpg) to verify file signatures");
		return JkUtilsReflect.invokeStaticMethod(PGPUTILS_CLASS, "verify",
				fileToVerify, pubRing, signature);
	}


}
