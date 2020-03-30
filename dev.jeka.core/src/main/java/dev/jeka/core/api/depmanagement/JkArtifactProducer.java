package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


/**
 * Interface to be implemented by classes responsible of producing artifacts. <p>
 */
public interface JkArtifactProducer extends JkArtifactLocator {

    /**
     * Produces the specified artifact file. This method is supposed to create it from scratch (should be working after
     * a clean) but implementations can caches already processed phase result as compilation result or so.
     */
    void makeArtifact(JkArtifactId jkArtifactId);

    /**
     * Returns the runtime dependencies of the specified artifact file. This is useful to use the artifact file as
     * a transitive dependency.
     */
    JkPathSequence fetchRuntimeDependencies(JkArtifactId jkArtifactId);

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
        makeArtifacts(getArtifactIds());
    }

    default void makeMainArtifact() {
        makeArtifact(getMainArtifactId());
    }

    /**
     * Produces specified artifact files. Only non existing files are created.
     */
    default void makeMissingArtifacts(JkArtifactId... artifactFileIds) {
        makeMissingArtifacts(Arrays.asList(artifactFileIds));
    }

    /**
     * Same as {@link #makeArtifact(JkArtifactId)}
     */
    default void makeMissingArtifacts(Iterable<JkArtifactId> artifactFileIds) {
        for (final JkArtifactId artifactFileId : artifactFileIds) {
            final Path path = getArtifactPath(artifactFileId);
            if (!Files.exists(path)) {
                makeArtifact(artifactFileId);
            } else {
                Path resultFile = Paths.get("").toAbsolutePath().relativize(path);
                JkLog.info("Making artifact file " + resultFile + " ... Skip : already exist.");
            }
        }
    }

    default void makeAllMissingArtifacts() {
        makeArtifacts(this.getArtifactIds());
    }

}
