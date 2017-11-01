package org.jerkar.api.depmanagement;

/**
 * An artifact producer is likely to produce several artifact files (main jar, sources, javadoc, native jars, ...). <br/>
 * To distinguish them one use the notion of 'classifier' and 'extension'. <br/>
 * Extension is simply the char sequence at the end of of file to determine its technical type (.exe, .jar, .zip, ...). <br/>
 * Classifier is to mention the purpose of the file (main artifact, sources, javadoc, uberjar, native lib, ...).
 */
public final class JkArtifactFileId {

    /**
     * Creates an artifact file id with the specified classifier and extension. Both can be <code>null</code>. <br/>
     * A <code>null</code> or empty classifier generally means the main artifact. <br/>
     * A <code>extension</code> or empty extension generally means that the file has no extension.<br/>
     */
    public static JkArtifactFileId of(String classifier, String extension) {
        return new JkArtifactFileId(classifier, extension);
    }

    private final String classifier, extension;

    private JkArtifactFileId(String classifier, String extension) {
        this.classifier = classifier == null || classifier.trim().length() == 0 ? null : classifier.trim().toLowerCase();
        this.extension = extension == null || extension.trim().length() == 0 ? null : extension.trim().toLowerCase();
    }

    /**
     * Returns the classifier of this object.
     */
    public String classifier() {
        return classifier;
    }

    /**
     * Returns the file extension of this object.
     */
    public String extension() {
        return extension;
    }

    /**
     * Returns <code>true</code> if any of the specified classifiers is equals to this classifier.
     */
    public boolean isClassifier(String... classifiers) {
        for (String classifier : classifiers) {
            if (classifier.trim().toLowerCase().equals(this.classifier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if any of the specified extension is equals to this extension.
     */
    public boolean isExtension(String... extensions) {
        for (String extension : extensions) {
            if (extension.trim().toLowerCase().equals(this.extension)) {
                return true;
            }
        }
        return false;
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


}
