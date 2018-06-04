package org.jerkar.api.depmanagement;

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
    Path artifactPath(JkArtifactId jkArtifactId);

    /**
     * Returns the main artifact file id for this producer. By default it returns a artifact file id with no
     * classifier and 'jar" extension.
     */
    default JkArtifactId mainArtifactId() {
        return JkArtifactId.of(null, "jar");
    }

    /**
     * Returns all the artifact ids likely to be produced by this artifact producer. By default it returns
     * a single element list containing the main artifact file id
     */
    default Iterable<JkArtifactId> artifactIds() {
        return JkUtilsIterable.listOf(mainArtifactId());
    }

    /**
     * Returns the main artifact path.
     */
    default Path mainArtifactPath() {
        return artifactPath(mainArtifactId());
    }

    /**
     * Returns all artifact files likely to be produced by this artifact producer.
     */
    default List<Path> allArtifactPaths() {
        final List<Path> result = new LinkedList<>();
        artifactIds().forEach(artifactFileId -> result.add(artifactPath(artifactFileId)));
        return result;
    }

    /**
     * Returns the arifact file ids having the specified classifier.
     */
    default Set<JkArtifactId> artifactIdsWithClassifier(String ... classifiers) {
        final Set<JkArtifactId> result = new LinkedHashSet<>();
        artifactIds().forEach((fid) -> {
            if (JkUtilsString.equalsAny(fid.classifier(), classifiers)) {
                result.add(fid);
            }
        });
        return result;
    }

}
