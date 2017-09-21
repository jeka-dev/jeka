package org.jerkar.api.depmanagement;

import org.jerkar.api.utils.JkUtilsIterable;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines methods for enumerating artifacts files likely to be produced by an object.
 */
public interface JkArtifactLocator {

    /**
     * Returns the file on file system that stands for the specified artifact file id. This method is supposed
     * to only returns the file reference and not generate it.
     */
    File artifactFile(JkArtifactFileId jkArtifactId);

    /**
     * Returns the main artifact file id for this producer. By default it returns a artifact file id with no
     * classifier and 'jar" extension.
     */
    default JkArtifactFileId mainArtifactFileId() {
        return JkArtifactFileId.of(null, "jar");
    }

    /**
     * Returns all the artifact file ids likely to be produced by this artifact producer. By default it returns
     * a single element list containing the main artifact file id
     */
    default Iterable<JkArtifactFileId> artifactFileIds() {
        return JkUtilsIterable.listOf(mainArtifactFileId());
    }

    /**
     * Returns the main artifact file.
     */
    default File mainArtifactFile() {
        return artifactFile(mainArtifactFileId());
    }

    /**
     * Returns all artifact files likely to be produced by this artifact producer.
     */
    default List<File> allArtifactFiles() {
        List<File> result = new LinkedList<>();
        result.add(artifactFile(mainArtifactFileId()));
        artifactFileIds().forEach(artifactFileId -> result.add(artifactFile(artifactFileId)));
        return result;
    }

}
