package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkInternalPublisher;
import dev.jeka.core.api.depmanagement.JkRepoSet;

import java.nio.file.Path;

/*
 * This class is only used with Refection. Please do not remove.
 */
final class IvyInternalPublisherFactory {

    /*
     * This method is only invoked by reflection. Please do not remove.
     */
    static JkInternalPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        Path arg = artifactDir == null ? null : artifactDir;
        return IvyInternalPublisher.of(publishRepos, arg);
    }

}
