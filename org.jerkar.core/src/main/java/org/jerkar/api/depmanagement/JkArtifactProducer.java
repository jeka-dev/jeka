package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.Arrays;

import org.jerkar.api.file.JkPathSequence;


/**
 * Defines methods for enumerating artifact files likely to be produced by an object along methods to produce them.
 */
public interface JkArtifactProducer extends JkArtifactLocator {

    /**
     * Produces the specified artifact file. This method is supposed to create it from scratch (should be working after
     * a clean) but implementations can caches already processed phase result as compilation result or so.
     */
    void makeArtifactFile(JkArtifactFileId jkArtifactId);

    /**
     * Returns the runtime dependencies of the specified artifact file. This is usefull to use the artifact file as
     * a transitive dependency.
     */
    JkPathSequence runtimeDependencies(JkArtifactFileId jkArtifactId);

    /**
     * Short hand to produce the main artifact file and returns the result.
     */
    default File makeMainJar() {
        this.makeArtifactFile(mainArtifactFileId());
        return artifactFile(mainArtifactFileId());
    }

    /**
     * Returns the main artifact file.
     */
    @Override
    default File mainArtifactFile() {
        return artifactFile(mainArtifactFileId());
    }

    /**
     * Produces all the artifact files for the specified artifact file ids.
     */
    default void makeArtifactFiles(Iterable<JkArtifactFileId> artifactFileIds) {
        for (final JkArtifactFileId artifactFileId : artifactFileIds) {
            makeArtifactFile(artifactFileId);
        }
    }

    /**
     * Produces all the artifact files for this producer.
     */
    default void makeAllArtifactFiles() {
        makeArtifactFiles(artifactFileIds());
    }

    /**
     * Produces specified artifact files. Only non existing files are created.
     */
    default void makeArtifactFilesIfNecessary(JkArtifactFileId ... artifactFileIds) {
        makeArtifactFilesIfNecessary(Arrays.asList(artifactFileIds));
    }

    /**
     * Same as {@link #makeArtifactFile(JkArtifactFileId)}
     */
    default void makeArtifactFilesIfNecessary(Iterable<JkArtifactFileId> artifactFileIds) {
        for (final JkArtifactFileId artifactFileId : artifactFileIds) {
            final File file = artifactFile(artifactFileId);
            if (!file.exists()) {
                makeArtifactFile(artifactFileId);
            }
        }
    }

}
