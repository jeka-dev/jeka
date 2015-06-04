package org.jerkar.crypto.pgp;

import java.io.File;
import java.util.Map;

import org.jerkar.JkClassLoader;
import org.jerkar.JkOptions;
import org.jerkar.utils.JkUtilsAssert;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsSystem;

/**
 * Provides method for signing and verify signature with PGP.<p>
 * When constructing JkPgp, you can provide a secret key ring, a public key ring or both.
 * <br/>Note that you need the secret ring for signing and the public ring for verifying.
 * 
 * @author Jerome Angibaud
 */
public final class JkPgp {

	private static final String PUB_KEYRING = "pgp.pubring";

	private static final String SECRET_KEYRING = "pgp.secring";

	private static final String PGPUTILS_CLASS_NAME = "org.jerkar.crypto.pgp.PgpUtils";

	// We don't want to add Bouncycastle in the Jerkar classpath, so we create a specific classloader
	// just for launching the Bouncy castle methods.
	private static Class<?> PGPUTILS_CLASS = JkClassLoader.current().sibling(
			JkPgp.class.getResource("bouncycastle-pgp-152.jar")
			).load(PGPUTILS_CLASS_NAME);

	private final File pubRing;

	private final File secRing;

	/**
	 * Creates a JkPgp with the specified public and secret ring.
	 */
	public static JkPgp of(File pubRing, File secRing) {
		return new JkPgp(pubRing, secRing);
	}

	public static JkPgp ofDefaultGnuPg() {
		final File pub;
		final File sec;
		if (JkUtilsSystem.IS_WINDOWS) {
			pub = new File(JkUtilsFile.userHome(), "AppData/Roaming/gnupg/pubring.gpg");
			sec = new File(JkUtilsFile.userHome(), "AppData/Roaming/gnupg/secring.gpg");
		} else {
			pub = new File(JkUtilsFile.userHome(), ".gnupg/pubring.gpg");
			sec = new File(JkUtilsFile.userHome(), ".gnupg/secring.gpg");
		}
		return new JkPgp(pub, sec);
	}



	/**
	 * Creates a JkPgp with values found in {@link JkOptions}
	 */
	public static JkPgp of(Map<String, String> option) {
		JkPgp result = ofDefaultGnuPg();
		final String pub = option.get(PUB_KEYRING);
		if (pub != null) {
			result = result.publicRing(new File(pub));
		}
		final String sec = option.get(SECRET_KEYRING);
		if (sec != null) {
			result = result.secretRing(new File(sec));
		}
		return result;
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
		final char[] pass;
		if (password == null) {
			pass = new char[0];
		} else {
			pass = password.toCharArray();
		}
		JkUtilsAssert.isTrue(secRing != null, "You must supply a secret ring file (as secring.gpg) to sign files");
		JkUtilsReflect.invokeStaticMethod(PGPUTILS_CLASS, "sign",
				fileToSign, secRing, output, pass, true);
	}

	/**
	 * Signs the specified files in a detached signature file which will have the same name of
	 * the signed file plus ".asc" suffix.
	 */
	public File[] sign(String password, File ...filesToSign) {
		final File[] result = new File[filesToSign.length];
		int i = 0;
		for (final File file : filesToSign) {
			if (!file.exists()) {
				continue;
			}
			final File signatureFile = new File(file.getParent(), file.getName() + ".asc");
			result[i] = signatureFile;
			sign(file, signatureFile, password);
			i++;
		}
		return result;
	}

	/**
	 * Verifies the specified file against the specified signature.
	 */
	public boolean verify(File fileToVerify, File signature) {
		JkUtilsAssert.isTrue(pubRing != null, "You must supply a public ring file (as pubring.gpg) to verify file signatures");
		final Boolean result = JkUtilsReflect.invokeStaticMethod(PGPUTILS_CLASS, "verify",
				fileToVerify, pubRing, signature);
		return result;
	}

	/**
	 * Creates a identical {@link JkPgp} but with the specified secret ring key file.
	 */
	public JkPgp secretRing(File file) {
		JkUtilsFile.assertAllExist(file);
		return new JkPgp(pubRing, file);
	}

	/**
	 * Creates a identical {@link JkPgp} but with the specified public ring key file.
	 */
	public JkPgp publicRing(File file) {
		JkUtilsFile.assertAllExist(file);
		return new JkPgp(file, secRing);
	}

	/**
	 * Returns the secret ring of this object.
	 */
	public File secretRing() {
		return secRing;
	}

	/**
	 * Returns the public ring of this object.
	 */
	public File publicRing() {
		return pubRing;
	}


}
