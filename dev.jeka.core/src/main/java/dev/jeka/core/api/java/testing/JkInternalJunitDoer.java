package dev.jeka.core.api.java.testing;

import dev.jeka.core.api.java.JkInternalClassloader;

import java.nio.file.Path;
import java.util.List;

/**
 * Not part of the public API
 */
public interface JkInternalJunitDoer {

    static JkInternalJunitDoer instance(List<Path> extraPaths) {
        String IMPL_CLASS = "dev.jeka.core.api.java.testing.embedded.junitplatform.JunitPlatformDoer";
        return JkInternalClassloader.ofMainEmbeddedLibs(extraPaths)
                .createCrossClassloaderProxy(JkInternalJunitDoer.class, IMPL_CLASS, "of");
    }

    JkTestResult launch(JkTestProcessor.JkEngineBehavior engineBehavior, JkTestSelection testSelection);

}
