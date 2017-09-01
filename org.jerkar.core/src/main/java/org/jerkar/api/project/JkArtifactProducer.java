package org.jerkar.api.project;

import org.jerkar.api.file.JkPath;
import org.jerkar.api.project.JkArtifactFileId.JkArtifactFileIds;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements this interface in order you project can be consumed as an other project dependency.
 */
@Deprecated // Experimental !!!!
public interface JkArtifactProducer {

    void doArtifactFile(JkArtifactFileId jkArtifactId);

    File artifactFile(JkArtifactFileId jkArtifactId);

    JkPath runtimeDependencies(JkArtifactFileId jkArtifactId);

    default JkArtifactFileId mainArtifactFileId() {
        return JkArtifactFileId.of(null, "jar");
    }

    default Iterable<JkArtifactFileId> artifactFileIds() {
        return JkArtifactFileIds.of(mainArtifactFileId());
    }

    default File mainArtifactFile() {
        return artifactFile(mainArtifactFileId());
    }

    default List<File> allArtifactFiles() {
        List<File> result = new LinkedList<>();
        result.add(artifactFile(mainArtifactFileId()));
        artifactFileIds().forEach(artifactFileId -> result.add(artifactFile(artifactFileId)));
        return result;
    }

    default void doArtifactFiles(Iterable<JkArtifactFileId> artifactFileIds) {
        for (JkArtifactFileId artifactFileId : artifactFileIds) {
            doArtifactFile(artifactFileId);
        }
    }

    default void doArtifactFiles(JkArtifactFileId ... artifactFileIds) {
        doArtifactFiles(Arrays.asList(artifactFileIds));
    }

    default void doAllArtifactFiles() {
        doArtifactFiles(artifactFileIds());
    }

    default void doArtifactFilesIfNecessary(JkArtifactFileId ... artifactFileIds) {
        doArtifactFilesIfNecessary(Arrays.asList(artifactFileIds));
    }

    default void doArtifactFilesIfNecessary(Iterable<JkArtifactFileId> artifactFileIds) {
        for (JkArtifactFileId artifactFileId : artifactFileIds) {
            File file = artifactFile(artifactFileId);
            if (!file.exists()) {
                doArtifactFile(artifactFileId);
            }
        }
    }


}
