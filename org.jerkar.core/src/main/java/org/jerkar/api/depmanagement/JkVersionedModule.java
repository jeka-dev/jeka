package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.jar.Attributes;

import org.jerkar.api.java.JkManifest;
import org.jerkar.api.utils.JkUtilsAssert;

/**
 * Identifies a given module in a given version
 *
 * @author Jerome Angibaud
 */
public final class JkVersionedModule implements Serializable {

    private static final long serialVersionUID = 1L;

    private final JkModuleId moduleId;

    private final JkVersion version;

    private JkVersionedModule(JkModuleId moduleId, JkVersion version) {
        super();
        this.moduleId = moduleId;
        this.version = version == null ? JkVersion.UNSPECIFIED : version;
    }

    /**
     * Creates a {@link JkVersionedModule} from the specified module and version.
     */
    public static JkVersionedModule of(JkModuleId moduleId, JkVersion version) {
        JkUtilsAssert.notNull(version, "Null version specified for " + moduleId + ". Must be at least UNSPECIFIED.");
        return new JkVersionedModule(moduleId, version);
    }

    /**
     * Creates a an unspecified version of {@link JkVersionedModule}.
     */
    public static JkVersionedModule ofUnspecifiedVerion(JkModuleId moduleId) {
        return of(moduleId, JkVersion.UNSPECIFIED);
    }

    /**
     * Creates a <code>JkVersionedModule</code> from a string formatted as
     * <code>groupId:name:version</code>.
     */
    public static final JkVersionedModule of(String description) {
        final String[] items = description.split(":");
        if (items.length != 3) {
            throw new IllegalArgumentException(description
                    + " does not respect format groupId:name:version");
        }
        return JkVersionedModule.of(JkModuleId.of(items[0], items[1]), JkVersion.of(items[2]));
    }

    /**
     * Creates a <code>JkVersionedModule</code> from a string formatted as
     * <code>groupId:name:version</code>.
     */
    public static final JkVersionedModule ofRootDirName(String rootDirName) {
        return ofUnspecifiedVerion(JkModuleId.of(rootDirName));
    }

    /**
     * Returns the module.
     */
    public JkModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Returns the version.
     */
    public JkVersion getVersion() {
        return version;
    }

    /**
     * Returns a {@link JkVersionedModule} identical to this one but with the specified version.
     */
    public JkVersionedModule withVersion(JkVersion version) {
        return of(this.moduleId, version);
    }

    /**
     * @see #withVersion(JkVersion)
     */
    public JkVersionedModule withVersion(String version) {
        return withVersion(JkVersion.of(version));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkVersionedModule that = (JkVersionedModule) o;

        if (!moduleId.equals(that.moduleId)) return false;
        return version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = moduleId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return moduleId + ":" + version;
    }

    /**
     * Fills <code>implementation title</code> and <code>implentation version </code> attributes of the
     * specified manifest.
     */
    public void populateManifest(JkManifest manifest) {
        manifest.addMainAttribute(Attributes.Name.IMPLEMENTATION_TITLE, getModuleId().getDotedName())
        .addMainAttribute(Attributes.Name.IMPLEMENTATION_VERSION, getVersion().getValue());
    }

}
