package dev.jeka.core.api.crypto.pgp;

import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;

/**
 * Provides method for signing and verify signature with PGP.
 * <p>
 * When constructing JkPgp, you can provide a secret key ring, a public key ring
 * or both.
 *
 * @author Jerome Angibaud
 */
public final class JkPgp implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PGPUTILS_CLASS_NAME = "dev.jeka.core.api.crypto.pgp.PgpUtils";

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    private final File pubRing;

    private final File secRing;

    private final String password;

    private JkPgp(Path pubRing, Path secRing, String password) {
        super();
        this.pubRing = pubRing.toFile();
        this.secRing = secRing.toFile();
        this.password = password;
    }

    // We don't want to add Bouncycastle in the Jeka classpath, so we create a
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

    public Path sign(Path fileToSign, String keyname) {
        final Path signatureFile = getSignatureFile(fileToSign);
        sign(fileToSign, keyname, signatureFile);
        return signatureFile;
    }

    /**
     * Signs the specified file and write the signature in the specified signature file.
     */
    public void sign(Path fileToSign, String keyName, Path signatureFile) {
        final char[] pass;
        if (password == null) {
            pass = new char[0];
        } else {
            pass = password.toCharArray();
        }
        JkUtilsAssert.isTrue(secRing != null,
                "You must supply a secret ring file (as secring.gpg) to sign files");
        if (!Files.exists(getSecretRing())) {
            throw new IllegalStateException("Specified secret ring file " + secRing + " not found.");
        }
        Method signMethod = JkUtilsReflect.getMethod(PGPUTILS_CLASS, "sign", Path.class, Path.class, String.class, Path.class,
                char[].class, boolean.class);
        JkUtilsReflect.invoke(null, signMethod, fileToSign,
                secRing.toPath(), keyName, signatureFile, pass, true);
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
        return JkUtilsReflect.invokeStaticMethod(PGPUTILS_CLASS, "verify", fileToVerify,
                pubRing.toPath(), signature);
    }

    /**
     * Creates a identical {@link JkPgp} but with the specified secret ring key file.
     */
    public JkPgp withSecretRing(Path file, String password) {
        return new JkPgp(pubRing.toPath(), file, password);
    }

    /**
     * Creates a identical {@link JkPgp} but with the specified public ring key file.
     */
    public JkPgp withPublicRing(Path file) {
        return new JkPgp(file, secRing.toPath(), password);
    }

    /**
     * Creates a identical {@link JkPgp} but with the specified password for secret ring.
     */
    public JkPgp withSecretRingPassword(String pwd) {
        return new JkPgp(pubRing.toPath(), secRing.toPath(), pwd);
    }

    /**
     * Returns the secret ring of this object.
     */
    public Path getSecretRing() {
        return secRing.toPath();
    }

    /**
     * Returns the public ring of this object.
     */
    public Path getPublicRing() {
        return pubRing.toPath();
    }

    public UnaryOperator<Path> getSigner(String keyName) {
        return new Signer(keyName);
    }

    private class Signer implements UnaryOperator<Path>, Serializable {

        private final String keyName;

        private Signer(String keyName) {
            this.keyName = keyName;
        }

        @Override
        public Path apply(Path file) {
            if (!Files.exists(file)) {
                return null;
            }
            final Path signatureFile = file.getParent().resolve(file.getFileName().toString() + ".asc");
            sign(file, keyName, signatureFile);
            return signatureFile;
        }
    }

}
