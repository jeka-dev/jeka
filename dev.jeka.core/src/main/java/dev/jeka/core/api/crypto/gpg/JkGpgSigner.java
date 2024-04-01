/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.crypto.gpg;

import dev.jeka.core.api.crypto.JkSigner;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides methods for signing data with GnuPG.
 *
 * @author Jerome Angibaud
 */
public final class JkGpgSigner implements JkSigner {

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    private static final JkInternalGpgDoer INTERNAL_GPG_DOER =
            JkInternalGpgDoer.of(JkProperties.ofSysPropsThenEnvThenGlobalProperties());

    private final Path secretRingPath;

    private final String passphrase;

    private final String keyName;

    private JkGpgSigner(Path secretRingPath, String password, String keyName) {
        super();
        this.secretRingPath = secretRingPath;
        this.passphrase = password;
        this.keyName = keyName;
    }

    /**
     * Creates a new instance of JkGpg with the specified secret ring, password, and key name.
     *
     * @param secRing    the path to the secret ring file
     * @param password   the password for the secret ring file
     * @param keyName    the name of the key to use within the secret ring file.
     *                   Can be empty if secRing contains a single key.
     * @return a new instance of JkGpg
     */
    public static JkGpgSigner of(Path secRing, String password, String keyName) {
        return new JkGpgSigner(secRing, password, keyName);
    }

    /**
     * Creates a JkGpg with the specified secret key ring, assuming the secret ring
     * contains a single key.
     */
    public static JkGpgSigner of(Path secRing, String password) {
        return of(secRing, password, "");
    }

    /**
     * Creates a {@link JkGpgSigner} instance for a standard project.
     * <p>
     * This method creates a {@link JkGpgSigner} instance using the specified base directory to locate
     * the public and secret ring files. If the public or secret ring files are not found in the
     * base directory, the default files will be used.
     * </p>
     *
     * @param baseDir the base directory of the project
     * @return a {@link JkGpgSigner} instance for the standard project
     */
    public static JkGpgSigner ofStandardProject(Path baseDir) {
        Path localSec = baseDir.resolve("gpg/secring.gpg");
        Path sec = JkUtilsPath.firstExisting(localSec, JkGpgSigner.getDefaultGpgSecretRingPath());
        if (sec == null) {
            sec = JkGpgSigner.getDefaultGpgSecretRingPath();
        }
        String secretKeyPassword = JkUtilsObject.firstNonNull(System.getProperty("jeka.gpg.passphrase"),
                System.getenv("JEKA_GPG_PASSPHRASE"), "");
        return JkGpgSigner.of(sec, secretKeyPassword, "");
    }

    /**
     * Creates a {@link JkGpgSigner} with default GnuPgp file location.
     *
     * @param keyName Can be empty if secret ring contains a single key.
     */
    public static JkGpgSigner ofDefaultGnuPg(String password, String keyName) {
        return of(getDefaultGpgSecretRingPath(), password, keyName);
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
     * Returns the secret ring of this object.
     */
    public Path getSecretRingPath() {
        return secretRingPath;
    }

    @Override
    public void sign(InputStream streamToSign, OutputStream signatureStream) {
        assertSecretRingExist();
        INTERNAL_GPG_DOER.sign(streamToSign, signatureStream, secretRingPath, keyName, passwordAsCharArray(), true);
    }

    private char[] passwordAsCharArray() {
        return passphrase == null ? new char[0] : passphrase.toCharArray();
    }

    private void assertSecretRingExist() {
        JkUtilsAssert.state(secretRingPath != null, "You must supply a public ring file (as secring.gpg) " +
                "to verify file signatures");
        JkUtilsAssert.state(Files.exists(secretRingPath), "Specified public ring file %s not found.", secretRingPath);
    }

}
