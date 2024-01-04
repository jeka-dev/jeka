package dev.jeka.core.api.depmanagement.artifact;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Objects;

/**
 * Artifacts are files produced by projects in order to be published and reused by other projects. {@link JkArtifactId}
 * identifies artifacts within a project. <p>
 * The identifier is compound of a name and a file extension. The name maps with the Maven 'classifier' concept.<br/>
 * In a project, we distinguish the <i>main artifact</i> from the others : the main artifact name values to empty string.
 */
public final class JkArtifactId {

    public static final String MAIN_ARTIFACT_NAME = "";
    public static final JkArtifactId SOURCES_ARTIFACT_ID = of("sources", "jar");
    public static final JkArtifactId JAVADOC_ARTIFACT_ID = of("javadoc", "jar");

    private final String name;

    private final String extension;

    private JkArtifactId(String name, String extension) {
        this.name = name.toLowerCase();
        this.extension = extension == null || extension.trim().length() == 0 ? null : extension.trim().toLowerCase();
    }

    /**
     * Creates an artifact id with the specified name and extension. <p>
     * The name cannot be null or be a string composed of spaces.
     * An empty string extension generally means that the file has no extension.<br/>
     */
    public static JkArtifactId of(String name, String extension) {
        JkUtilsAssert.argument(name != null, "Artifact name cannot be null");
        JkUtilsAssert.argument(extension != null, "Artifact extension cannot be null (but blank is ok).");
        JkUtilsAssert.argument(MAIN_ARTIFACT_NAME.equals(name) || !JkUtilsString.isBlank(name),
                "Artifact name cannot be a blank string.");
        return new JkArtifactId(name, extension);
    }

    /**
     * Shorthand for <code>of(MAIN_ARTIFACT_NAME, String)</code>.
     */
    public static JkArtifactId ofMainArtifact(String extension) {
        return JkArtifactId.of(MAIN_ARTIFACT_NAME, extension);
    }

    public boolean isMainArtifact() {
        return MAIN_ARTIFACT_NAME.equals(this.name);
    }

    /**
     * Returns the classifier of this object.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the file getExtension of this object.
     */
    public String getExtension() {
        return extension;
    }

    public String toFileName(String namePart) {
        String classifier = isMainArtifact() ? "" : "-" + getName();
        String ext = JkUtilsString.isBlank(extension) ? "" : "." + getExtension();
        return namePart + classifier + ext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkArtifactId that = (JkArtifactId) o;

        if (!Objects.equals(name, that.name)) return false;
        return Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String classif = name == null ? "[main-artifact]" : "-" + name;
        return classif + '.' + extension;
    }


}
