package dev.jeka.core.api.testing;

import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;

import java.nio.file.Path;
import java.util.List;

/**
 * Not part of the public API
 */
public interface JkInternalJunitDoer {

    static JkInternalJunitDoer instance(List<Path> extraPaths) {
        String IMPL_CLASS = "dev.jeka.core.api.testing.embedded.junitplatform.JunitPlatformDoer";
        return JkInternalEmbeddedClassloader.ofMainEmbeddedLibs(extraPaths)
                .createCrossClassloaderProxy(JkInternalJunitDoer.class, IMPL_CLASS, "of");
    }

    JkTestResult launch(JkTestProcessor.JkEngineBehavior engineBehavior, JkTestSelection testSelection);

}
