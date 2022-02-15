package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;

/**
 * Identifies a given module in a given version
 *
 * @author Jerome Angibaud
 */
public final class JkVersionedModule {

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
        JkUtilsAssert.argument(version != null, "Null version specified for " + moduleId + ". Must be at least UNSPECIFIED.");
        return new JkVersionedModule(moduleId, version);
    }

    /**
     * Creates an unspecified version of {@link JkVersionedModule}.
     */
    public static JkVersionedModule ofUnspecifiedVersion(JkModuleId moduleId) {
        return of(moduleId, JkVersion.UNSPECIFIED);
    }

    /**
     * Creates a <code>JkVersionedModule</code> from a string formatted as
     * <code>groupId:name:version</code>.
     */
    public static JkVersionedModule of(String description) {
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
    public static JkVersionedModule ofRootDirName(String rootDirName) {
        return ofUnspecifiedVersion(JkModuleId.of(rootDirName));
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

    public JkVersionedModule resolveConflict(JkVersion other, ConflictStrategy strategy) {
        if (version.isUnspecified()) {
            return withVersion(other);
        }
        if (other.isUnspecified()) {
            return this;
        }
        if (strategy == ConflictStrategy.FAIL && !version.equals(other)) {
            throw new IllegalStateException("Module " + this.moduleId + " has been declared with both version '" + version +
                    "' and '" + other + "'");
        }
        if (version.isSnapshot() && !other.isSnapshot()) {
            return withVersion(other);
        }
        if (!version.isSnapshot() && other.isSnapshot()) {
            return this;
        }
        if (strategy == ConflictStrategy.TAKE_FIRST) {
            return this;
        }
        return strategy == ConflictStrategy.TAKE_HIGHEST && version.isGreaterThan(other) ?
                this : this.withVersion(other);
    }

    public enum ConflictStrategy {
        TAKE_FIRST, TAKE_HIGHEST, TAKE_LOWEST, FAIL
    }


}
