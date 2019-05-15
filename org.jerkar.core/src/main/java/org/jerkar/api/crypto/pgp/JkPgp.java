package org.jerkar.api.crypto.pgp;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;

import org.jerkar.api.java.JkUrlClassLoader;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsSystem;

/**
 * Provides method for signing and verify signature with PGP.
 * <p>
 * When constructing JkPgp, you can provide a secret key ring, a public key ring
 * or both.
 *
 * @author Jerome Angibaud
 */
public final class JkPgp implements UnaryOperator<Path>,  Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PGPUTILS_CLASS_NAME = "org.jerkar.api.crypto.pgp.PgpUtils";

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    private final Path pubRing;

    private final Path secRing;

    private final String password;

    private JkPgp(Path pubRing, Path secRing, String password) {
        super();
        this.pubRing = pubRing;
        this.secRing = secRing;
        this.password = password;
    }

    // We don't want to add Bouncycastle in the Jerkar classpath, so we create a
    // specific classloader just for launching the Bouncy castle methods.
    private static final Class<?> PGPUTILS_CLASS = JkUrlClassLoader.ofCurrent()
            .getSiblingWithOptional(JkPgp.class.getResource("bouncycastle-pgp-152.jar"))
            .toJkClassLoader().load(PGPUTILS_CLASS_NAME);

    /**
     * Creates a {@link JkPgp} with the specified public and secret ring.
     */
    public static JkPgp of(Path pubRing, Path secRing, String password) {
        return new JkPgp(pubRing, secRing, password);
    }

    /**
     * Creates a {@link JkPgp} with default GnuPgp file location.
     */
    public static JkPgp ofDefaultGnuPg() {
        final Path pub;
        final Path sec;
        if (JkUtilsSystem.IS_WINDOWS) {
            pub = USER_HOME.resolve("AppData/Roaming/gnupg/pubring.gpg");
            sec = USER_HOME.resolve("AppData/Roaming/gnupg/secring.gpg");
        } else {
            pub = USER_HOME.resolve(".gnupg/pubring.gpg");
            sec = USER_HOME.resolve(".gnupg/secring.gpg");
        }
        return new JkPgp(pub, sec, null);
    }

    /**
     * Creates a JkPgp with the specified public key ring.
     */
    public static JkPgp ofPublicRing(Path pubRing) {
        return of(pubRing, null, null);
    }

    /**
     * Creates a JkPgp with the specified secret key ring.
     */
    public static JkPgp ofSecretRing(Path secRing, String password) {
        return of(null, secRing, password);
    }

    public Path sign(Path fileToSign) {
        final Path signatureFile = getSignatureFile(fileToSign);
        sign(fileToSign, signatureFile);
        return signatureFile;
    }

    /**
     * Signs the specified file and write the signature in the specified signature file.
     */
    public void sign(Path fileToSign, Path signatureFile) {
        final char[] pass;
        if (password == null) {
            pass = new char[0];
        } else {
            pass = password.toCharArray();
        }
        JkUtilsAssert.isTrue(secRing != null,
                "You must supply a secret ring file (as secring.gpg) to sign files");
        if (!Files.exists(getSecretRing())) {
            throw new IllegalStateException("Specified secret ring file not found.");
        }
        Method signMethod = JkUtilsReflect.getMethod(PGPUTILS_CLASS, "sign", Path.class, Path.class, Path.class,
                char[].class, boolean.class);
        JkUtilsReflect.invoke(null, signMethod, fileToSign,
                secRing, signatureFile, pass, true);
    }

    /**
     * Returns file that are created if a signature occurs on specified files.
     */
    public static Path getSignatureFile(Path fileToSign) {
        return fileToSign.getParent().resolve(fileToSign.getFileName().toString() + ".asc");
    }

    /**
     * Verifies the specified file against the specified signature.
     */
    public boolean verify(Path fileToVerify, Path signature) {
        JkUtilsAssert.isTrue(pubRing != null,
                "You must supply a public ring file (as pubring.gpg) to verify file signatures");
        if (!Files.exists(getPublicRing())) {
            throw new IllegalStateException("Specified public ring file " + getPublicRing() + " not found.");
        }
        return JkUtilsReflect.invokeStaticMethod(PGPUTILS_CLASS, "verify", fileToVerify, pubRing, signature);
    }

    /**
     * Creates a identical {@link JkPgp} but with the specified secret ring key file.
     */
    public JkPgp withSecretRing(Path file, String password) {
        return new JkPgp(pubRing, file, password);
    }

    /**
     * Creates a identical {@link JkPgp} but with the specified public ring key file.
     */
    public JkPgp withPublicRing(Path file) {
        return new JkPgp(file, secRing, password);
    }

    /**
     * Creates a identical {@link JkPgp} but with the specified password for secret ring.
     */
    public JkPgp withSecretRingPassword(String pwd) {
        return new JkPgp(pubRing, secRing, pwd);
    }

    /**
     * Returns the secret ring of this object.
     */
    public Path getSecretRing() {
        return secRing;
    }

    /**
     * Returns the public ring of this object.
     */
    public Path getPublicRing() {
        return pubRing;
    }

    @Override
    public Path apply(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        final Path signatureFile = file.getParent().resolve(file.getFileName().toString() + ".asc");
        sign(file, signatureFile);
        return signatureFile;
    }

}
