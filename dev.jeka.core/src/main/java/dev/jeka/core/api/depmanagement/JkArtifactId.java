package dev.jeka.core.api.depmanagement;

/**
 * Artifacts are files produced by projects in order to be published and reused by other projects. {@link JkArtifactId}
 * identifies artifacts within a project. <p>
 * The identifier is compound of a name and a file extension. The name maps with the Maven 'classifier' concept.<br/>
 * In a project, we distinguish the <i>main artifact</i> from the others : the main artifact name values to {@code null}.
 */
public final class JkArtifactId {

    private final String name, extension;

    private JkArtifactId(String name, String extension) {
        this.name = name == null || name.trim().length() == 0 ? null : name.trim().toLowerCase();
        this.extension = extension == null || extension.trim().length() == 0 ? null : extension.trim().toLowerCase();
    }

    /**
     * Creates an artifact file id with the specified classifier and getExtension. Both can be <code>null</code>. <br/>
     * A <code>null</code> or empty classifier generally means the main artifact. <br/>
     * A <code>getExtension</code> or empty getExtension generally means that the file has no getExtension.<br/>
     */
    public static JkArtifactId of(String name, String extension) {
        return new JkArtifactId(name, extension);
    }

    public static JkArtifactId ofMainArtifact(String extension) {
        return new JkArtifactId(null, extension);
    }

    public boolean isMainArtifact() {
        return this.name == null;
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
        String classifier = getName() == null ? "" : "-" + getName();
        String extension = getExtension() == null ? "" : "." + getExtension();
        return namePart + classifier + extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkArtifactId that = (JkArtifactId) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return extension != null ? extension.equals(that.extension) : that.extension == null;
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
        return "" + classif + '.' + extension;
    }


}
