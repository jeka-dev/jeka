package org.jerkar.api.project;

import org.jerkar.api.file.JkPath;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements this interface in order you project can be consumed as an other project dependency.
 */
@Deprecated // Experimental !!!!
public interface JkArtifactProducer {

    void produceArtifactFile(JkArtifactFileId jkArtifactId);

    File getArtifactFile(JkArtifactFileId jkArtifactId);

    JkPath runtimeDependencies(JkArtifactFileId jkArtifactId);

    default JkArtifactFileId mainArtifactFileId() {
        return JkArtifactFileId.of(null, "jar");
    }

    default Iterable<JkArtifactFileId> extraArtifactFileIds() {
        return new LinkedList<>();
    }

    default void produceAllArtifactFiles() {
        produceArtifactFile(mainArtifactFileId());
        for (JkArtifactFileId artifactFileId : extraArtifactFileIds()) {
            produceArtifactFile(artifactFileId);
        }
    }

    default List<File> getAllArtifactFiles() {
        List<File> result = new LinkedList<>();
        result.add(getArtifactFile(mainArtifactFileId()));
        extraArtifactFileIds().forEach(artifactFileId -> result.add(getArtifactFile(artifactFileId)));
        return result;
    }


}
