package org.jerkar.api.depmanagement;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Defines methods for enumerating artifacts files likely to be produced by an object.
 */
public interface JkArtifactLocator {


    /**
     * Returns file system path where is supposed to be produced the specified artifact file id. This method is supposed
     * to only returns the file reference and not generate it.
     */
    Path artifactPath(JkArtifactFileId jkArtifactId);

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
     * Returns the main artifact path.
     */
    default Path mainArtifactPath() {
        return artifactPath(mainArtifactFileId());
    }

    /**
     * Returns all artifact files likely to be produced by this artifact producer.
     */
    default List<Path> allArtifactPaths() {
        final List<Path> result = new LinkedList<>();
        result.add(artifactPath(mainArtifactFileId()));
        artifactFileIds().forEach(artifactFileId -> result.add(artifactPath(artifactFileId)));
        return result;
    }

    /**
     * Returns the arifact file ids having the specified classifier.
     */
    default Set<JkArtifactFileId> artifactsFileIdsWithClassifier(String ... classifiers) {
        final Set<JkArtifactFileId> result = new LinkedHashSet<>();
        artifactFileIds().forEach((fid) -> {
            if (JkUtilsString.equalsAny(fid.classifier(), classifiers)) {
                result.add(fid);
            }
        });
        return result;
    }

}
