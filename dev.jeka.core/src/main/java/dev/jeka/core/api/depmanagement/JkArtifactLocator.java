package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsIterable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines methods for enumerating artifacts files likely to be produced.
 */
public interface JkArtifactLocator {

    /**
     * Returns file ofSystem path where is supposed to be produced the specified artifact file id. This method is supposed
     * to only returns the file reference and not generate it.
     */
    Path getArtifactPath(JkArtifactId artifactId);

    /**
     * Returns the main artifact file id for this producer. By default it returns a artifact file id with no
     * classifier and 'jar" getExtension.
     */
    default JkArtifactId getMainArtifactId() {
        return JkArtifactId.of(JkArtifactId.MAIN_ARTIFACT_NAME, "jar");
    }

    /**
     * Returns all the artifact ids likely to be produced by this artifact producer. By default it returns
     * a single element list containing the main artifact file id
     */
    default List<JkArtifactId> getArtifactIds() {
        return JkUtilsIterable.listOf(getMainArtifactId());
    }

    /**
     * Returns the main artifact path.
     */
    default Path getMainArtifactPath() {
        return getArtifactPath(getMainArtifactId());
    }

    /**
     * Returns non existing files matching for artifacts.
     */
    default List<Path> getMissingFiles() {
        return getArtifactIds().stream()
                .map(this::getArtifactPath)
                .filter(path -> !Files.exists(path))
                .collect(Collectors.toList());
    }

}
