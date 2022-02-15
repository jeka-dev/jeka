package dev.jeka.core.api.crypto.gpg;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;

/**
 * Provides method for signing and verify signature with PGP.
 * <p>
 * When constructing JkGpg, you can provide a secret key ring, a public key ring
 * or both.
 *
 * @author Jerome Angibaud
 */
public final class JkGpg {

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    private static final JkInternalGpgDoer INTERNAL_GPG_DOER = JkInternalGpgDoer.of();

    private final Path pubRing;

    private final Path secRing;

    private final String passphrase;

    private JkGpg(Path pubRing, Path secRing, String password) {
        super();
        this.pubRing = pubRing;
        this.secRing = secRing;
        this.passphrase = password;
    }

    /**
     * Creates a {@link JkGpg} with the specified public and secret ring.
     */
    public static JkGpg of(Path pubRing, Path secRing, String password) {
        return new JkGpg(pubRing, secRing, password);
    }

    public static JkGpg ofStandardProject(Path baseDir) {
        Path localPub = baseDir.resolve(JkConstants.JEKA_DIR).resolve("gpg/pubring.gpg");
        Path pub = JkUtilsPath.firstExisting(localPub, JkGpg.getDefaultPubring());
        if (pub == null) {
            pub = JkGpg.getDefaultPubring();
        }
        Path localSec = baseDir.resolve(JkConstants.JEKA_DIR).resolve("gpg/secring.gpg");
        Path sec = JkUtilsPath.firstExisting(localSec, JkGpg.getDefaultSecring());
        if (sec == null) {
            sec = JkGpg.getDefaultSecring();
        }
        String secretKeyPassword = JkUtilsObject.firstNonNull(System.getProperty("jeka.gpg.passphrase"),
                System.getenv("JEKA_GPG_PASSPHRASE"), "");
        return JkGpg.of(pub, sec, secretKeyPassword);
    }

    public boolean isPublicAndSecretRingExist() {
        return Files.exists(pubRing) && Files.exists(secRing);
    }

    public static Path getDefaultPubring() {
        if (JkUtilsSystem.IS_WINDOWS) {
            return USER_HOME.resolve("AppData/Roaming/gnupg/pubring.gpg");
        }
        return USER_HOME.resolve(".gnupg/pubring.gpg");
    }

    public static Path getDefaultSecring() {
        if (JkUtilsSystem.IS_WINDOWS) {
            return USER_HOME.resolve("AppData/Roaming/gnupg/secring.gpg");
        }
        return USER_HOME.resolve(".gnupg/secring.gpg");
    }

    /**
     * Creates a {@link JkGpg} with default GnuPgp file location.
     */
    public static JkGpg ofDefaultGnuPg() {
        return new JkGpg(getDefaultPubring(), getDefaultSecring(), null);
    }

    /**
     * Creates a JkGpg with the specified public key ring.
     */
    public static JkGpg ofPublicRing(Path pubRing) {
        return of(pubRing, null, null);
    }

    /**
     * Creates a JkGpg with the specified secret key ring.
     */
    public static JkGpg ofSecretRing(Path secRing, String password) {
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
        if (passphrase == null) {
            pass = new char[0];
        } else {
            pass = passphrase.toCharArray();
        }
        JkUtilsAssert.state(secRing != null, "You must supply a secret ring file (as secring.gpg) to sign files");
        JkUtilsAssert.state(Files.exists(secRing), "Specified secret ring file " + secRing + " not found.");
        INTERNAL_GPG_DOER.sign(fileToSign, secRing, keyName, signatureFile, pass, true);
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
        JkUtilsAssert.state(pubRing != null,
                "You must supply a public ring file (as pubring.gpg) to verify file signatures");
        if (!Files.exists(getPublicRing())) {
            throw new IllegalStateException("Specified public ring file " + pubRing+ " not found.");
        }
        return INTERNAL_GPG_DOER.verify(fileToVerify, pubRing, signature);
    }

    /**
     * Creates an identical {@link JkGpg} but with the specified secret ring key file.
     */
    public JkGpg withSecretRing(Path file, String password) {
        return new JkGpg(pubRing, file, password);
    }

    /**
     * Creates an identical {@link JkGpg} but with the specified public ring key file.
     */
    public JkGpg withPublicRing(Path file) {
        return new JkGpg(file, secRing, passphrase);
    }

    /**
     * Creates an identical {@link JkGpg} but with the specified password for secret ring.
     */
    public JkGpg withSecretRingPassword(String pwd) {
        return new JkGpg(pubRing, secRing, pwd);
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

    /**
     * Creates a signer based on this secret ring and passphrase and using the specified key.
     * @param keyName The secret key to use within the t-scrfet ring. If empty string, the first key
     *                of the secret ring is selected.
     */
    public UnaryOperator<Path> getSigner(String keyName) {
        JkUtilsAssert.argument(keyName != null, "Key name cannot be null. Use \"\" to select the first " +
                "key present in " + this.secRing);
        return new Signer(keyName);
    }

    private class Signer implements UnaryOperator<Path> {

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
