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

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkInternalChildFirstClassLoader;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface JkInternalGpgDoer {

    boolean verify(Path fileToVerify, Path signatureFile,
                   Path publicRingPath);

    void sign(InputStream streamToSign, OutputStream signatureStream,
              Path secretRingPath, String keyName, char[] pass, boolean armor);

    static JkInternalGpgDoer of(JkProperties properties) {
        return Cache.get(properties);
    }

    class Cache {

        private static JkInternalGpgDoer CACHED_INSTANCE;

        private static JkInternalGpgDoer get(JkProperties properties) {
            if (CACHED_INSTANCE != null) {
                return CACHED_INSTANCE;
            }
            String IMPL_CLASS = "dev.jeka.core.api.crypto.gpg.embedded.bc.BcGpgDoer";
            Class<JkInternalGpgDoer> clazz = JkClassLoader.ofCurrent().loadIfExist(IMPL_CLASS);
            if (clazz != null) {
                return JkUtilsReflect.invokeStaticMethod(clazz, "of");
            }
            String bouncyCastleVersion = "1.70";
            JkCoordinateFileProxy bcProviderJar = JkCoordinateFileProxy.ofStandardRepos(properties,
                    "org.bouncycastle:bcprov-jdk15on:" + bouncyCastleVersion);
            JkCoordinateFileProxy bcOpenPgpApiJar = JkCoordinateFileProxy.ofStandardRepos(properties,
                    "org.bouncycastle:bcpg-jdk15on:" + bouncyCastleVersion);

            JkPathSequence paths = JkPathSequence.of(bcProviderJar.get(), bcOpenPgpApiJar.get());

            ClassLoader classLoader = JkInternalChildFirstClassLoader.of(paths, JkInternalGpgDoer.class.getClassLoader());
            clazz = JkClassLoader.of(classLoader).load(IMPL_CLASS);
            CACHED_INSTANCE = JkUtilsReflect.invokeStaticMethod(clazz, "of");
            return CACHED_INSTANCE;
        }

    }

}
