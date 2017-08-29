package org.jerkar.api.project;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by angibaudj on 29-08-17.
 */
@Deprecated // Experimental !!!!
public class JkArtifactFileId {

    public static JkArtifactFileId of(String classifier, String extension) {
        return new JkArtifactFileId(classifier, extension);
    }

    private final String classifier, extension;

    private JkArtifactFileId(String classifier, String extension) {
        this.classifier = classifier == null || classifier.trim().length() == 0 ? null : classifier.trim().toLowerCase();
        this.extension = extension == null || extension.trim().length() == 0 ? null : extension.trim().toLowerCase();
    }

    public String classifier() {
        return classifier;
    }

    public String extension() {
        return extension;
    }

    public boolean isClassifier(String... classifiers) {
        for (String classifier : classifiers) {
            if (classifier.trim().toLowerCase().equals(this.classifier)) {
                return true;
            }
        }
        return false;
    }

    public boolean isExtension(String... extensions) {
        for (String extension : extensions) {
            if (extension.trim().toLowerCase().equals(this.extension)) {
                return true;
            }
        }
        return false;
    }

    public JkArtifactFileIds and(JkArtifactFileId... ids) {
        return JkArtifactFileIds.of(this).and(ids);
    }

    public JkArtifactFileIds and(String classifier, String extension) {
        return and(JkArtifactFileId.of(classifier, extension));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkArtifactFileId that = (JkArtifactFileId) o;

        if (classifier != null ? !classifier.equals(that.classifier) : that.classifier != null) return false;
        return extension != null ? extension.equals(that.extension) : that.extension == null;
    }

    @Override
    public int hashCode() {
        int result = classifier != null ? classifier.hashCode() : 0;
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Classifier='" + classifier + '\'' + ", extension='" + extension + '\'' + '}';
    }

    public static class JkArtifactFileIds implements Iterable<JkArtifactFileId> {

        public static JkArtifactFileIds of(JkArtifactFileId... ids) {
            return new JkArtifactFileIds(Arrays.asList(ids));
        }

        private final List<JkArtifactFileId> ids;

        @Override
        public Iterator<JkArtifactFileId> iterator() {
            return ids.iterator();
        }

        private JkArtifactFileIds(List<JkArtifactFileId> ids) {
            this.ids = ids;
        }

        public JkArtifactFileIds and(JkArtifactFileId... ids) {
            List<JkArtifactFileId> jkArtifactIds = new LinkedList(this.ids);
            jkArtifactIds.addAll(Arrays.asList(ids));
            return new JkArtifactFileIds(jkArtifactIds);
        }

        public JkArtifactFileIds and(String classifier, String extension) {
            return and(JkArtifactFileId.of(classifier, extension));
        }
    }
}
