package dev.jeka.core.api.crypto.gpg;

import dev.jeka.core.api.depmanagement.JkModuleFileProxy;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;

public interface JkInternalGpgDoer {

    boolean verify(Path fileToVerify, Path pubringFile, Path signatureFile);

    void sign(Path fileToSign, Path secringFile, String keyName, Path signatureFile, char[] pass, boolean armor);

    static JkInternalGpgDoer of() {
        String IMPL_CLASS = "dev.jeka.core.api.crypto.gpg.embedded.bc.BcGpgDoer";
        Class<JkInternalGpgDoer> clazz = JkClassLoader.ofCurrent().loadIfExist(IMPL_CLASS);
        if (clazz != null) {
            return JkUtilsReflect.invokeStaticMethod(clazz, "of");
        }
        String bouncyCastleVersion = "1.70";
        JkModuleFileProxy bcProviderJar = JkModuleFileProxy.ofStandardRepos("org.bouncycastle:bcprov-jdk15on:"
                + bouncyCastleVersion);
        JkModuleFileProxy bcopenPgpApiJar = JkModuleFileProxy.ofStandardRepos("org.bouncycastle:bcpg-jdk15on:"
                + bouncyCastleVersion);
        return JkInternalClassloader.ofMainEmbeddedLibs(bcopenPgpApiJar.get(), bcProviderJar.get())
                .createCrossClassloaderProxy(JkInternalGpgDoer.class, IMPL_CLASS, "of");
    }

}
