package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

public final class JkMavenPublication implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public static JkMavenPublication of(File file) {
        return new JkMavenPublication(JkUtilsIterable.listOf(file), Collections.EMPTY_LIST, null);
    }

    private final List<JkClassifiedArtifact> classifiedArtifacts;

    private final List<File> mainArtifacts; // can't have 2 artifacts with same
    // extension

    private final JkMavenPublicationInfo extraInfo;

    private JkMavenPublication(List<File> mainArtifacts, List<JkClassifiedArtifact> classified,
            JkMavenPublicationInfo extraInfo) {
        super();
        this.mainArtifacts = mainArtifacts;
        this.classifiedArtifacts = classified;
        this.extraInfo = extraInfo;
    }

    public JkMavenPublication andIf(boolean condition, File file, String classifier) {
        if (condition) {
            return and(file, classifier);
        }
        return this;
    }

    public JkMavenPublication and(File file, String classifier) {
        JkUtilsAssert.isTrue(!JkUtilsString.isBlank(classifier), "classifier cannot be empty");
        final String fileExt = JkUtilsString.substringAfterLast(file.getName(), ".");
        if (JkUtilsString.isBlank(fileExt)) {
            throw new IllegalArgumentException("the file " + file.getPath()
                    + " must have an extension (as .jar, .zip, ...");
        }
        if (contains(fileExt, classifier)) {
            throw new IllegalArgumentException(
                    "Can't add artifact with extension/classifier equals to [" + fileExt + "/"
                            + classifier
                            + "] as this combination is yet present in this publication " + this);
        }
        final JkClassifiedArtifact artifact = new JkClassifiedArtifact(classifier, file);
        final List<JkClassifiedArtifact> list = new LinkedList<JkClassifiedArtifact>(
                this.classifiedArtifacts);
        list.add(artifact);
        return new JkMavenPublication(this.mainArtifacts, list, this.extraInfo);
    }

    private boolean contains(String ext, String classifier) {
        for (final JkClassifiedArtifact classifiedArtifact : this.classifiedArtifacts) {
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
     * publication extra info required to publish on Maven central repository.
     */
    public JkMavenPublication with(JkMavenPublicationInfo extraInfo) {
        return new JkMavenPublication(this.mainArtifacts, this.classifiedArtifacts, extraInfo);
    }

    public JkMavenPublication andOptional(File file, String classifier) {
        if (file.exists()) {
            return and(file, classifier);
        }
        return this;
    }

    public JkMavenPublication andOptionalIf(boolean conditional, File file, String classifier) {
        if (conditional) {
            return andOptional(file, classifier);
        }
        return this;
    }

    public List<File> mainArtifactFiles() {
        return Collections.unmodifiableList(this.mainArtifacts);
    }

    public List<JkClassifiedArtifact> classifiedArtifacts() {
        return Collections.unmodifiableList(classifiedArtifacts);
    }

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
    public static class JkClassifiedArtifact implements Serializable {

        private static final long serialVersionUID = 1L;

        private String classifier;
        private File file;

        JkClassifiedArtifact(String classifier, File file) {
            super();
            this.classifier = classifier;
            this.file = file;
        }

        public String classifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }

        public File file() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

    }

}
