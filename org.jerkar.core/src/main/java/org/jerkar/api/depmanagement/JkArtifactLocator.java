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
     * Returns file ofSystem path where is supposed to be produced the specified artifact file id. This method is supposed
     * to only returns the file reference and not generate it.
     */
    Path getArtifactPath(JkArtifactId jkArtifactId);

    /**
     * Returns the main artifact file id for this producer. By default it returns a artifact file id with no
     * classifier and 'jar" getExtension.
     */
    default JkArtifactId getMainArtifactId() {
        return JkArtifactId.of(null, "jar");
    }

    /**
     * Returns all the artifact ids likely to be produced by this artifact producer. By default it returns
     * a single element list containing the main artifact file id
     */
    default Iterable<JkArtifactId> getArtifactIds() {
        return JkUtilsIterable.listOf(getMainArtifactId());
    }

    /**
     * Returns the main artifact path.
     */
    default Path getMainArtifactPath() {
        return getArtifactPath(getMainArtifactId());
    }

    /**
     * Returns all artifact files likely to be produced by this artifact producer.
     */
    default List<Path> getAllArtifactPaths() {
        final List<Path> result = new LinkedList<>();
        getArtifactIds().forEach(artifactFileId -> result.add(getArtifactPath(artifactFileId)));
        return result;
    }

    /**
     * Returns the arifact file ids having the specified classifier.
     */
    default Set<JkArtifactId> getArtifactIdsWithClassifier(String ... classifiers) {
        final Set<JkArtifactId> result = new LinkedHashSet<>();
        getArtifactIds().forEach((fid) -> {
            if (JkUtilsString.equalsAny(fid.getClassifier(), classifiers)) {
                result.add(fid);
            }
        });
        return result;
    }

}
