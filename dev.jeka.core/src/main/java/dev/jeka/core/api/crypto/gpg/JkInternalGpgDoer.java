package dev.jeka.core.api.crypto.gpg;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;

public interface JkInternalGpgDoer {

    boolean verify(Path fileToVerify, Path pubringFile, Path signatureFile);

    void sign(Path fileToSign, Path secringFile, String keyName, Path signatureFile, char[] pass, boolean armor);

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
            JkCoordinateFileProxy bcProviderJar = JkCoordinateFileProxy.ofStandardRepos(properties, "org.bouncycastle:bcprov-jdk15on:"
                    + bouncyCastleVersion);
            JkCoordinateFileProxy bcopenPgpApiJar = JkCoordinateFileProxy.ofStandardRepos(properties, "org.bouncycastle:bcpg-jdk15on:"
                    + bouncyCastleVersion);
            CACHED_INSTANCE =  JkInternalEmbeddedClassloader.ofMainEmbeddedLibs(bcopenPgpApiJar.get(), bcProviderJar.get())
                    .createCrossClassloaderProxy(JkInternalGpgDoer.class, IMPL_CLASS, "of");
            return CACHED_INSTANCE;
        }

    }

}
