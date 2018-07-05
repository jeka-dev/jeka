package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.jar.Attributes;

import org.jerkar.api.java.JkManifest;
import org.jerkar.api.utils.JkUtilsAssert;

/**
 * Identifies a given module in a given projectVersion
 *
 * @author Jerome Angibaud
 */
public final class JkVersionedModule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link JkVersionedModule} from the specified module and projectVersion.
     */
    public static JkVersionedModule of(JkModuleId moduleId, JkVersion version) {
        JkUtilsAssert.notNull(version, "No projectVersion specified for " + moduleId);
        return new JkVersionedModule(moduleId, version);
    }

    /**
     * Creates a <code>JkVersionedModule</code> from a string formatted as
     * <code>groupId:name:projectVersion</code>.
     */
    public static final JkVersionedModule of(String description) {
        final String[] items = description.split(":");
        if (items.length != 3) {
            throw new IllegalArgumentException(description
                    + " does not tespect format groupId:name:projectVersion");
        }
        return JkVersionedModule.of(JkModuleId.of(items[0], items[1]), JkVersion.of(items[2]));
    }

    private final JkModuleId moduleId;

    private final JkVersion version;

    private JkVersionedModule(JkModuleId moduleId, JkVersion version) {
        super();
        this.moduleId = moduleId;
        this.version = version;
    }

    /**
     * Returns the module.
     */
    public JkModuleId moduleId() {
        return moduleId;
    }

    /**
     * Returns the projectVersion.
     */
    public JkVersion version() {
        return version;
    }

    /**
     * Returns a {@link JkVersionedModule} identical to this one but with the specified projectVersion.
     */
    public JkVersionedModule withVersion(JkVersion version) {
        return new JkVersionedModule(this.moduleId, version);
    }

    /**
     * @see #withVersion(JkVersion)
     */
    public JkVersionedModule withVersion(String version) {
        return new JkVersionedModule(this.moduleId, JkVersion.of(version));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((moduleId == null) ? 0 : moduleId.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JkVersionedModule other = (JkVersionedModule) obj;
        if (moduleId == null) {
            if (other.moduleId != null) {
                return false;
            }
        } else if (!moduleId.equals(other.moduleId)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return moduleId + ":" + version;
    }

    /**
     * Fills the manifest with <code>implementation</code> infoString.
     */
    public void populateManifest(JkManifest manifest) {
        manifest.addMainAttribute(Attributes.Name.IMPLEMENTATION_TITLE, moduleId().fullName())
        .addMainAttribute(Attributes.Name.IMPLEMENTATION_VERSION, version().value());
    }

}
