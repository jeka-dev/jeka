package dev.jeka.core.api.crypto.gpg;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;
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
            CACHED_INSTANCE =  JkInternalEmbeddedClassloader.ofMainEmbeddedLibs(bcOpenPgpApiJar.get(), bcProviderJar.get())
                    .createCrossClassloaderProxy(JkInternalGpgDoer.class, IMPL_CLASS, "of");
            return CACHED_INSTANCE;
        }

    }

}
