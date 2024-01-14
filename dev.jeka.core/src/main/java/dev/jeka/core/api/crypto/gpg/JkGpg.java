package dev.jeka.core.api.crypto.gpg;

import dev.jeka.core.api.crypto.JkSigner;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkConstants;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides methods for signing and verify signature with GnuPG.
 *
 * @author Jerome Angibaud
 */
public final class JkGpg implements JkSigner {

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    private static final JkInternalGpgDoer INTERNAL_GPG_DOER =
            JkInternalGpgDoer.of(JkProperties.ofSysPropsThenEnvThenGlobalProperties());

    private final Path publicRingPath;

    private final Path secretRingPath;

    private final String passphrase;

    private final String keyName;

    private JkGpg(Path pubRing, Path secretRingPath, String password, String keyName) {
        super();
        this.publicRingPath = pubRing;
        this.secretRingPath = secretRingPath;
        this.passphrase = password;
        this.keyName = keyName;
    }

    /**
     * Creates a new instance of JkGpg with the specified public ring, secret ring, password, and key name.
     *
     * @param pubRing    the path to the public ring file
     * @param secRing    the path to the secret ring file
     * @param password   the password for the secret ring file
     * @param keyName    the name of the key to use within the secret ring file.
     *                   Can be empty if secRing contains a single key.
     * @return a new instance of JkGpg
     */
    public static JkGpg of(Path pubRing, Path secRing, String password, String keyName) {
        return new JkGpg(pubRing, secRing, password, keyName);
    }

    /**
     * Creates a {@link JkGpg} instance for a standard project.
     * <p>
     * This method creates a {@link JkGpg} instance using the specified base directory to locate
     * the public and secret ring files. If the public or secret ring files are not found in the
     * base directory, the default files will be used.
     * </p>
     *
     * @param baseDir the base directory of the project
     * @return a {@link JkGpg} instance for the standard project
     */
    public static JkGpg ofStandardProject(Path baseDir) {
        Path localPub = baseDir.resolve(JkConstants.JEKA_DIR).resolve("gpg/pubring.gpg");
        Path pub = JkUtilsPath.firstExisting(localPub, JkGpg.getDefaultGpgPublicRingPath());
        if (pub == null) {
            pub = JkGpg.getDefaultGpgPublicRingPath();
        }
        Path localSec = baseDir.resolve(JkConstants.JEKA_DIR).resolve("gpg/secring.gpg");
        Path sec = JkUtilsPath.firstExisting(localSec, JkGpg.getDefaultGpgSecretRingPath());
        if (sec == null) {
            sec = JkGpg.getDefaultGpgSecretRingPath();
        }
        String secretKeyPassword = JkUtilsObject.firstNonNull(System.getProperty("jeka.gpg.passphrase"),
                System.getenv("JEKA_GPG_PASSPHRASE"), "");
        return JkGpg.of(pub, sec, secretKeyPassword, "");
    }

    /**
     * Returns the default path for the pubring.gpg file.
     */
    public static Path getDefaultGpgPublicRingPath() {
        if (JkUtilsSystem.IS_WINDOWS) {
            return USER_HOME.resolve("AppData/Roaming/gnupg/pubring.gpg");
        }
        return USER_HOME.resolve(".gnupg/pubring.gpg");
    }

    /**
     * Returns the default path for the secret ring file.
     */
    public static Path getDefaultGpgSecretRingPath() {
        if (JkUtilsSystem.IS_WINDOWS) {
            return USER_HOME.resolve("AppData/Roaming/gnupg/secring.gpg");
        }
        return USER_HOME.resolve(".gnupg/secring.gpg");
    }

    /**
     * Creates a {@link JkGpg} with default GnuPgp file location.
     *
     * @param keyName Can be empty if secret ring contains a single key.
     */
    public static JkGpg ofDefaultGnuPg(String password, String keyName) {
        return of(getDefaultGpgPublicRingPath(), getDefaultGpgSecretRingPath(), password, keyName);
    }

    /**
     * Creates a JkGpg with the specified secret key ring.
     *
     * @param keyName Can be empty if secret ring contains a single key.
     */
    public static JkGpg ofSecretRing(Path secRing, String password, String keyName) {
        return of(null, secRing, password, keyName);
    }

    /**
     * Returns the secret ring of this object.
     */
    public Path getSecretRingPath() {
        return secretRingPath;
    }

    /**
     * Returns the public ring of this object.
     */
    public Path getPublicRingPath() {
        return publicRingPath;
    }

    /**
     * Verifies the specified file against the specified signature.
     */
    public boolean verify(Path fileToVerify, Path signature) {
        assertPublicRingExist();
        return INTERNAL_GPG_DOER.verify(fileToVerify, signature, publicRingPath);
    }

    @Override
    public void sign(InputStream streamToSign, OutputStream signatureStream) {
        assertSecretRingExist();
        INTERNAL_GPG_DOER.sign(streamToSign, signatureStream, secretRingPath, keyName, passwordAsCharArray(), true);
    }

    private char[] passwordAsCharArray() {
        return passphrase == null ? new char[0] : passphrase.toCharArray();
    }

    private void assertPublicRingExist() {
        JkUtilsAssert.state(publicRingPath != null, "You must supply a public ring file (as pubring.gpg) " +
                "to verify file signatures");
        JkUtilsAssert.state(Files.exists(publicRingPath), "Specified public ring file %s not found.", publicRingPath);
    }

    private void assertSecretRingExist() {
        JkUtilsAssert.state(secretRingPath != null, "You must supply a public ring file (as secring.gpg) " +
                "to verify file signatures");
        JkUtilsAssert.state(Files.exists(secretRingPath), "Specified public ring file %s not found.", secretRingPath);
    }

}
