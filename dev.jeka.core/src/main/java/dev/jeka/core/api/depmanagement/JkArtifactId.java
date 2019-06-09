package dev.jeka.core.api.depmanagement;

/**
 * Artifacts are files produced by projects in order to be published or reused by other projects.
 * This class stands for an identifier for an artifact within a project.
 * An artifact producer is likely to produce several artifact files (main jar, sources, javadoc, native jars, ...). <br/>
 * To distinguish them, Jeka uses the notion of 'classifier' and 'extension'. <br/>
 * Extension is simply the char sequence at the end of of file to determine its technical type (.exe, .jar, .zip, ...). <br/>
 * Classifier is to mention the purpose of the file (main artifact, sources, javadoc, uberjar, native lib, ...).
 */
public final class JkArtifactId {

    /**
     * Creates an artifact file id with the specified classifier and getExtension. Both can be <code>null</code>. <br/>
     * A <code>null</code> or empty classifier generally means the main artifact. <br/>
     * A <code>getExtension</code> or empty getExtension generally means that the file has no getExtension.<br/>
     */
    public static JkArtifactId of(String classifier, String extension) {
        return new JkArtifactId(classifier, extension);
    }

    private final String classifier, extension;

    private JkArtifactId(String classifier, String extension) {
        this.classifier = classifier == null || classifier.trim().length() == 0 ? null : classifier.trim().toLowerCase();
        this.extension = extension == null || extension.trim().length() == 0 ? null : extension.trim().toLowerCase();
    }

    /**
     * Returns the classifier of this object.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Returns the file getExtension of this object.
     */
    public String getExtension() {
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
     * Returns <code>true</code> if any of the specified getExtension is equals to this getExtension.
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

        JkArtifactId that = (JkArtifactId) o;

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
        String classif = classifier == null ? "[main-artifact]" : "-" + classifier;
        return "" + classif + '.' + extension;
    }


}
