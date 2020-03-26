package dev.jeka.core.api.java.junit;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;

/**
 * Not part of the public API
 */
public interface JkInternalJunitDoer {

    static JkInternalJunitDoer instance(List<Path> extraPaths) {
        String IMPL_CLASS = "dev.jeka.core.api.java.junit.embedded.junit.JunitPlatformDoer";
        Class<JkInternalJunitDoer> clazz = JkClassLoader.ofCurrent().loadIfExist(IMPL_CLASS);
        if (clazz != null) {
            return JkUtilsReflect.invokeStaticMethod(clazz, "of");
        }
        return JkInternalClassloader.ofMainEmbeddedLibs(extraPaths)
                .createCrossClassloaderProxy(JkInternalJunitDoer.class, IMPL_CLASS, "of");
    }

    // testRequest is either a dev.jeka.core.api.java.junit.JkTestSelection, either a
    // dev.jeka.core.api.function.JkUnaryOperator<LauncherDiscoveryRequestBuilder>
    JkTestResult launch(JkTestProcessor.JkEngineBehavior engineBehavior, Serializable testRequest);

}
