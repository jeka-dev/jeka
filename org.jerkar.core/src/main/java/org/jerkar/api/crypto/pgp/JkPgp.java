package org.jerkar.api.crypto.pgp;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsSystem;

/**
 * Provides method for signing and verify signature with PGP.
 * <p>
 * When constructing JkPgp, you can provide a secret key ring, a public key ring
 * or both. <br/>
 * Note that you need the secret ring for signing and the public ring for
 * verifying.
 *
 * @author Jerome Angibaud
 */
public final class JkPgp implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PUB_KEYRING = "pgp.pubring";

    private static final String SECRET_KEYRING = "pgp.secring";

    private static final String SECRET_KEY_PASSWORD = "pgp.secretKeyPassword";

    private static final String PGPUTILS_CLASS_NAME = "org.jerkar.api.crypto.pgp.PgpUtils";

    // We don't want to add Bouncycastle in the Jerkar classpath, so we create a
    // specific classloader
    // just for launching the Bouncy castle methods.
    private static Class<?> PGPUTILS_CLASS = JkClassLoader.current()
            .siblingWithOptional(JkPgp.class.getResource("bouncycastle-pgp-152.jar"))
            .load(PGPUTILS_CLASS_NAME);

    private final File pubRing;

    private final File secRing;

    private final String password;

    /**
     * Creates a {@link JkPgp} with the specified public and secret ring.
     */
    public static JkPgp of(File pubRing, File secRing, String password) {
        return new JkPgp(pubRing, secRing, password);
    }

    /**
     * Creates a {@link JkPgp} with default GnuPgp file location.
     */
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
        return new JkPgp(pub, sec, null);
    }

    /**
     * Creates a JkPgp with key/values map
     */
    public static JkPgp of(Map<String, String> options) {
        JkPgp result = ofDefaultGnuPg();
        final String pub = options.get(PUB_KEYRING);
        if (pub != null) {
            result = result.publicRing(new File(pub));
        }
        final String sec = options.get(SECRET_KEYRING);
        final String password = options.get(SECRET_KEY_PASSWORD);
        if (sec != null) {
            result = result.secretRing(new File(sec), password);
        } else {
            result = result.secretRing(result.secRing, password);
        }
        return result;
    }

    /**
     * Creates a JkPgp with the specified public key ring.
     */
    public static JkPgp ofPublicRing(File pubRing) {
        return of(pubRing, null, null);
    }

    /**
     * Creates a JkPgp with the specified secret key ring.
     */
    public static JkPgp ofSecretRing(File secRing, String password) {
        return of(null, secRing, password);
    }

    private JkPgp(File pubRing, File secRing, String password) {
        super();
        this.pubRing = pubRing;
        this.secRing = secRing;
        this.password = password;
    }

    void sign(File fileToSign, File output, String password) {
        final char[] pass;
        if (password == null) {
            pass = new char[0];
        } else {
            pass = password.toCharArray();
        }
        JkUtilsAssert.isTrue(secRing != null,
                "You must supply a secret ring file (as secring.gpg) to sign files");
        JkUtilsReflect.invokeStaticMethod(PGPUTILS_CLASS, "sign", fileToSign, secRing, output,
                pass, true);
    }

    /**
     * Signs the specified files in a detached signature file which will have
     * the same name of the signed file plus ".asc" suffix.
     */
    public File[] sign(File... filesToSign) {
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
     * Returns file that are created if a signature occurs on specified files.
     */
    public static File[] drySignatureFiles(File... filesToSign) {
        final File[] result = new File[filesToSign.length];
        int i = 0;
        for (final File file : filesToSign) {
            final File signatureFile = new File(file.getParent(), file.getName() + ".asc");
            result[i] = signatureFile;
            i++;
        }
        return result;
    }

    /**
     * Verifies the specified file against the specified signature.
     */
    public boolean verify(File fileToVerify, File signature) {
        JkUtilsAssert.isTrue(pubRing != null,
                "You must supply a public ring file (as pubring.gpg) to verify file signatures");
        final Boolean result = JkUtilsReflect.invokeStaticMethod(PGPUTILS_CLASS, "verify",
                fileToVerify, pubRing, signature);
        return result;
    }

    /**
     * Creates a identical {@link JkPgp} but with the specified secret ring key
     * file.
     */
    public JkPgp secretRing(File file, String password) {
        JkUtilsFile.assertAllExist(file);
        return new JkPgp(pubRing, file, password);
    }

    /**
     * Creates a identical {@link JkPgp} but with the specified public ring key
     * file.
     */
    public JkPgp publicRing(File file) {
        JkUtilsFile.assertAllExist(file);
        return new JkPgp(file, secRing, password);
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
