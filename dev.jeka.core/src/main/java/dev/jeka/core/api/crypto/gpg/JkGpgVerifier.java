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

import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides methods for verifying signature with GnuPG.
 *
 * @author Jerome Angibaud
 */
public final class JkGpgVerifier {

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    private static final JkInternalGpgDoer INTERNAL_GPG_DOER =
            JkInternalGpgDoer.of(JkProperties.ofSysPropsThenEnvThenGlobalProperties());

    private final Path publicRingPath;

    private JkGpgVerifier(Path publicRingPath) {
        super();
        this.publicRingPath = publicRingPath;
    }

    /**
     * Creates a new instance of JkGpg with the specified public ring.
     */
    public static JkGpgVerifier of(Path pubRing) {
        return new JkGpgVerifier(pubRing);
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

    private void assertPublicRingExist() {
        JkUtilsAssert.state(publicRingPath != null, "You must supply a public ring file (as pubring.gpg) " +
                "to verify file signatures");
        JkUtilsAssert.state(Files.exists(publicRingPath), "Specified public ring file %s not found.", publicRingPath);
    }

}
