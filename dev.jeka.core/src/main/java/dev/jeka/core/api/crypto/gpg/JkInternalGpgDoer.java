package dev.jeka.core.api.crypto.gpg;

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
        return JkInternalClassloader.ofMainEmbeddedLibs().createCrossClassloaderProxy(JkInternalGpgDoer.class, IMPL_CLASS, "of");
    }

}
