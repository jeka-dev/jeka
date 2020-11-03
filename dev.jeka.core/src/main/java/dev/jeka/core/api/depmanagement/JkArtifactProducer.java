package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


/**
 * Interface to be implemented by classes responsible of producing artifacts. <p>
 */
public interface JkArtifactProducer extends JkArtifactLocator {

    @FunctionalInterface
    interface JkSupplier {

        JkArtifactProducer getArtifactProducer();

    }

    /**
     * Produces the specified artifact file. This method is supposed to create it from scratch (should be working after
     * a clean) but implementations can caches already processed phase result as compilation result or so.
     */
    void makeArtifact(JkArtifactId jkArtifactId);


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
                JkLog.info("Making artifact file " + JkUtilsPath.relativizeFromWorkingDir(path)
                        + " ... Skip : already exist.");
            }
        }
    }

    default void makeAllMissingArtifacts() {
        makeArtifacts(this.getArtifactIds());
    }

}
