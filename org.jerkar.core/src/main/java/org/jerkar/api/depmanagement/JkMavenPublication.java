package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Publication specific information to include in POM file accept order to be published of a Maven repository.
 * These information contains : <ul>
 *   <li>The artifacts to be published (main artifact and artifacts with classifiers)</li>
 *   <li>Information about describing the project as some public repositories require</li>
 * </ul>
 *
 */
public final class JkMavenPublication implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<JkClassifiedFileArtifact> classifiedArtifacts;

    private final List<File> mainArtifacts; // can't have 2 artifacts with same extension

    private final JkMavenPublicationInfo extraInfo;

    private JkMavenPublication(List<File> mainArtifacts, List<JkClassifiedFileArtifact> classified,
                               JkMavenPublicationInfo extraInfo) {
        super();
        this.mainArtifacts = mainArtifacts;
        this.classifiedArtifacts = classified;
        this.extraInfo = extraInfo;
    }

    /**
     * Creates a Maven publication specifying the file to publish as main artifact.
     */
    public static JkMavenPublication of(Path file) {
        return new JkMavenPublication(JkUtilsIterable.listOf(file.toFile()),
                Collections.emptyList(), null);
    }

    /**
     * Creates a Maven publication to publish all artifacts referenced in the specified artifact locator.
     */
    public static JkMavenPublication of(JkArtifactLocator artifactLocator, Set<JkArtifactId> excludedArtifacts) {
        JkMavenPublication result = JkMavenPublication.of(artifactLocator.artifactPath(artifactLocator.mainArtifactId()));
        for (final JkArtifactId artifactFileId : artifactLocator.artifactIds()) {
            if (excludedArtifacts.contains(artifactFileId) || artifactFileId.classifier() == null) {
                continue;
            }
            final Path file = artifactLocator.artifactPath(artifactFileId);
            result = result.andOptional(file, artifactFileId.classifier());
        }
        return result;
    }

    /**
     * Same as {@link #and(Path, String)} but effective only if the specified condition is <code>true</code>.
     */
    public JkMavenPublication andIf(boolean condition, Path file, String classifier) {
        if (condition) {
            return and(file, classifier);
        }
        return this;
    }

    /**
     * Returns a {@link JkMavenPublication} identical to this one but adding a classified artifact.
     */
    public JkMavenPublication and(Path file, String classifier) {
        JkUtilsAssert.isTrue(!JkUtilsString.isBlank(classifier), "classifier can not be empty. Use JkMavenPublication#of " +
                "for creating a publication with the main artifact.");
        final String fileExt = JkUtilsString.substringAfterLast(file.getFileName().toString(), ".");
        if (JkUtilsString.isBlank(fileExt)) {
            throw new IllegalArgumentException("File " + file
            + " must have an extension (as .jar, .zip, ...");
        }
        if (contains(fileExt, classifier)) {
            throw new IllegalArgumentException(
                    "Can't add artifact with extension/classifier equals to [" + fileExt + "/"
                            + classifier
                            + "] as this combination is yet present in this publication " + this);
        }
        final JkClassifiedFileArtifact artifact = new JkClassifiedFileArtifact(classifier, file);
        final List<JkClassifiedFileArtifact> list = new LinkedList<>(
                this.classifiedArtifacts);
        list.add(artifact);
        return new JkMavenPublication(this.mainArtifacts, list, this.extraInfo);
    }

    private boolean contains(String ext, String classifier) {
        for (final JkClassifiedFileArtifact classifiedArtifact : this.classifiedArtifacts) {
            final String fileExt = JkUtilsString.substringAfterLast(
                    classifiedArtifact.file.getName(), ".");
            if (classifier.contains(classifiedArtifact.classifier) && fileExt.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a new publication based on this one but with the specified
     * publication extra infoString required to publish on Maven central repository.
     */
    public JkMavenPublication with(JkMavenPublicationInfo extraInfo) {
        return new JkMavenPublication(this.mainArtifacts, this.classifiedArtifacts, extraInfo);
    }

    /**
     * Same as {@link #and(Path, String)} but effective only if the specified file exists.
     * If not the case, this method returns this object.
     */
    public JkMavenPublication andOptional(Path file, String classifier) {
        if (Files.exists(file)) {
            return and(file, classifier);
        }
        return this;
    }

    /**
     * Same as {@link #andOptional(Path, String)}  but effective only if the specified condition is <code>true</code>
     */
    public JkMavenPublication andOptionalIf(boolean conditional, Path file, String classifier) {
        if (conditional) {
            return andOptional(file, classifier);
        }
        return this;
    }

    /** Files constituting main artifact */
    public List<Path> mainArtifactFiles() {
        return JkUtilsPath.toPaths(this.mainArtifacts);
    }

    /** Files constituting classified artifacts */
    public List<JkClassifiedFileArtifact> classifiedArtifacts() {
        return Collections.unmodifiableList(classifiedArtifacts);
    }

    /**  */
    public JkMavenPublicationInfo extraInfo() {
        return this.extraInfo;
    }

    @Override
    public String toString() {
        return mainArtifacts.toString() + " / " + classifiedArtifacts.toString();
    }

    /**
     * An artifact with a classifier for Maven repository.
     */
    public static class JkClassifiedFileArtifact implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String classifier;
        private final File file;  // Path not serializable

        JkClassifiedFileArtifact(String classifier, Path file) {
            super();
            this.classifier = classifier;
            this.file = file.toFile();
        }

        /** Classifier string for this classified artifact */
        public String classifier() {
            return classifier;
        }

        /** File for this classified artifact */
        public Path file() {
            return file.toPath();
        }

    }

}
