package dev.jeka.core.api.depmanagement;

/**
 * Publication specific information to include in POM file in order to be published of a Maven repository.
 * These information contains : <ul>
 *   <li>The artifacts to be published (main artifact and artifacts with classifiers)</li>
 *   <li>Information about describing the project as some public repositories require</li>
 * </ul>
 */
public final class JkMavenPublication {

    private final JkArtifactLocator artifactLocator;

    private final JkPublishedPomMetadata<JkMavenPublication> extraInfo;

    private JkMavenPublication(JkArtifactLocator artifactLocator, JkPublishedPomMetadata extraInfo) {
        this.artifactLocator = artifactLocator;
        this.extraInfo = extraInfo;
    }

    public static JkMavenPublication of(JkArtifactLocator artifactFileLocator, JkPublishedPomMetadata extraInfo) {
        return new JkMavenPublication(artifactFileLocator, extraInfo);
    }


    public JkPublishedPomMetadata<JkMavenPublication> getExtraInfo() {
        return this.extraInfo;
    }

    public JkArtifactLocator getArtifactLocator() {
        return artifactLocator;
    }

    @Override
    public String toString() {
        return "JkMavenPublication{" +
                "artifactFileLocator=" + artifactLocator +
                ", extraInfo=" + extraInfo +
                '}';
    }
}
