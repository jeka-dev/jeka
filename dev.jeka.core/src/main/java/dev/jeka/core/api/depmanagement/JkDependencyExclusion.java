package dev.jeka.core.api.depmanagement;

/**
 * A piece of information aiming at excluding transitive dependencies.
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyExclusion {

    private final JkModuleId moduleId;

    private final String classifier;

    private final String extension;


    private JkDependencyExclusion(JkModuleId moduleId, String classifier, String extension) {
        super();
        this.moduleId = moduleId;
        this.classifier = classifier;
        this.extension = extension;
    }

    /**
     * Creates an exclusion of the specified module.
     */
    @SuppressWarnings("unchecked")
    public static JkDependencyExclusion of(JkModuleId moduleId) {
        return new JkDependencyExclusion(moduleId, null, null);
    }

    /**
     * Creates an exclusion of the specified module.
     */
    public static JkDependencyExclusion of(String group, String name) {
        return of(JkModuleId.of(group, name));
    }

    /**
     * Creates an exclusion of the specified module.
     */
    public static JkDependencyExclusion of(String groupAndName) {
        return of(JkModuleId.of(groupAndName));
    }

    /**
     * Returns an exclusion identical to this one but with the specified type.
     * Types generally corresponds to getExtension or classifier but not always.
     * Some examples are <i>jar</i>, <i>test-jar</i>, <i>test-client</i>.
     */
    public JkDependencyExclusion withType(String typeArg) {
        return new JkDependencyExclusion(moduleId, typeArg, extension);
    }

    /**
     * Returns an exclusion identical to this one but with the specified getExtension.
     * Types generally corresponds to getExtension or classifier but not always.
     * Some examples are <i>jar</i>, <i>test-jar</i>, <i>test-client</i>.
     */
    public JkDependencyExclusion withExt(String extension) {
        return new JkDependencyExclusion(moduleId, classifier, extension);
    }

    /**
     * Returns the module id to exclude.
     */
    public JkModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Returns the type of the artifact file to exclude.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Returns the getExtension for the artifact files to exclude. If not <code>null</code>
     * only file artifact having this getExtension will be effectively excluded.
     */
    public String getExtension() {
        return extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkDependencyExclusion that = (JkDependencyExclusion) o;
        if (!moduleId.equals(that.moduleId)) return false;
        if (classifier != null ? !classifier.equals(that.classifier) : that.classifier != null) return false;
        return extension != null ? extension.equals(that.extension) : that.extension == null;
    }

    @Override
    public int hashCode() {
        int result = moduleId.hashCode();
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        return result;
    }
}
