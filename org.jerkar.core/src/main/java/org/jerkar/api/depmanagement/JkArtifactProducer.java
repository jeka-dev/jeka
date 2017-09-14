package org.jerkar.api.depmanagement;

import org.jerkar.api.file.JkPath;
import org.jerkar.api.utils.JkUtilsIterable;

import java.io.File;
import java.util.Arrays;


/**
 * Defines methods for enumerating artifact files likely to be produced by an object along methods to produce them.
 */
public interface JkArtifactProducer extends JkArtifactLocator {

    /**
     * Produces the specified artifact file. This method is supposed to create it from scratch (should be working after
     * a clean) but implementations can caches already processed phase result as compilation result or so.
     */
    void doArtifactFile(JkArtifactFileId jkArtifactId);

    /**
     * Returns the runtime dependencies of the specified artifact file. This is usefull to use the artifact file as
     * a transitive dependency.
     */
    JkPath runtimeDependencies(JkArtifactFileId jkArtifactId);

    /**
     * Returns the main artifact file.
     */
    default File mainArtifactFile() {
        return artifactFile(mainArtifactFileId());
    }

    /**
     * Produces all the artifact files for the specified artifact file ids.
     */
    default void doArtifactFiles(Iterable<JkArtifactFileId> artifactFileIds) {
        for (JkArtifactFileId artifactFileId : artifactFileIds) {
            doArtifactFile(artifactFileId);
        }
    }

    /**
     * Same as {@link #doArtifactFile(JkArtifactFileId)}.
     */
    default void doArtifactFiles(JkArtifactFileId ... artifactFileIds) {
        doArtifactFiles(Arrays.asList(artifactFileIds));
    }

    /**
     * Produces all the artifact files for this producer.
     */
    default void doAllArtifactFiles() {
        doArtifactFiles(artifactFileIds());
    }

    /**
     * Produces specified artifact files. Only non existing files are created.
     */
    default void doArtifactFilesIfNecessary(JkArtifactFileId ... artifactFileIds) {
        doArtifactFilesIfNecessary(Arrays.asList(artifactFileIds));
    }

    /**
     * Same as {@link #doArtifactFile(JkArtifactFileId)}
     */
    default void doArtifactFilesIfNecessary(Iterable<JkArtifactFileId> artifactFileIds) {
        for (JkArtifactFileId artifactFileId : artifactFileIds) {
            File file = artifactFile(artifactFileId);
            if (!file.exists()) {
                doArtifactFile(artifactFileId);
            }
        }
    }

    /**
     * Returns this artifacts producer as a form of dependency. If the specified artifact file ids are
     * empty, the dependency is made on the main artifact file id.
     */
    default JkComputedDependency asDependency(Iterable<JkArtifactFileId> artifactFileIds) {
        final Iterable<JkArtifactFileId> fileIds = artifactFileIds.iterator().hasNext() ? artifactFileIds
                : JkUtilsIterable.listOf(mainArtifactFileId());
        return new ArtifactProducerDependency(this, artifactFileIds);
    }

    /**
     * Returns this artifacts producer as a form of dependency. If no artifact file ids is specified,
     * the dependency is made on the main artifact file id.
     */
    default JkComputedDependency asDependency(JkArtifactFileId ... artifactFileIds) {
        return asDependency(Arrays.asList(artifactFileIds));
    }

}
