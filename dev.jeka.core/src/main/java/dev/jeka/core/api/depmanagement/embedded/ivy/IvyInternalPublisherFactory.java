package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkInternalPublisher;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.java.JkClassLoader;

import java.io.File;
import java.nio.file.Path;

class IvyInternalPublisherFactory {

    static JkInternalPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        File arg = artifactDir == null ? null : artifactDir.toFile();
        final JkInternalPublisher ivyPublisher;
        if (JkClassLoader.ofCurrent().isDefined(IvyClassloader.IVY_CLASS_NAME)) {
            ivyPublisher = IvyInternalPublisher.of(publishRepos, arg);

            // Temporary while embeddedClassloader is not implemented
        } else {
            ivyPublisher =IvyClassloader.CLASSLOADER.createCrossClassloaderProxy(
                    JkInternalPublisher.class, IvyInternalPublisher.class.getName(), "of", publishRepos,
                    artifactDir == null ? null : artifactDir.toFile());
        }
        return ivyPublisher;
    }

}
