package org.jerkar.api.depmanagement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.system.JkLog;


/**
 * Defines methods for enumerating artifact files likely to be produced by an object along methods to produce them.
 */
public interface JkArtifactProducer extends JkArtifactLocator {

    /**
     * Produces the specified artifact file. This method is supposed to create it from scratch (should be working after
     * a clean) but implementations can caches already processed phase result as compilation result or so.
     */
    void makeArtifact(JkArtifactId jkArtifactId);

    /**
     * Returns the runtime dependencies of the specified artifact file. This is usefull to use the artifact file as
     * a transitive dependency.
     */
    JkPathSequence runtimeDependencies(JkArtifactId jkArtifactId);

    /**
     * Produces all the artifact files for the specified artifact file ids.
     */
    default void makeArtifacts(Iterable<JkArtifactId> artifactFileIds) {
        for (final JkArtifactId artifactFileId : artifactFileIds) {
            makeArtifact(artifactFileId);
        }
    }

    /**
     * Produces all the artifact files for the specified artifact file ids.
     */
    default void makeArtifacts(JkArtifactId ... artifactFileIds) {
        makeArtifacts(Arrays.asList(artifactFileIds));
    }

    /**
     * Produces all the artifact files for this producer.
     */
    default void makeAllArtifacts() {
        makeArtifacts(artifactIds());
    }

    /**
     * Produces specified artifact files. Only non existing files are created.
     */
    default void makeArtifactsIfAbsent(JkArtifactId... artifactFileIds) {
        makeArtifactsIfAbsent(Arrays.asList(artifactFileIds));
    }

    /**
     * Same as {@link #makeArtifact(JkArtifactId)}
     */
    default void makeArtifactsIfAbsent(Iterable<JkArtifactId> artifactFileIds) {
        for (final JkArtifactId artifactFileId : artifactFileIds) {
            final Path path = artifactPath(artifactFileId);
            if (!Files.exists(path)) {
                makeArtifact(artifactFileId);
            } else {
                JkLog.info("Artifact " + path + " already exist ... won't process.");
            }
        }
    }

}
