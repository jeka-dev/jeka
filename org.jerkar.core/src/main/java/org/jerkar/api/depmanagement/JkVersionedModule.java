package org.jerkar.api.depmanagement;

import java.io.Serializable;

/**
 * Identifies a given module in a given version
 * 
 * @author Jerome Angibaud
 */
public final class JkVersionedModule implements Serializable {

    private static final long serialVersionUID = 1L;

    public static JkVersionedModule of(JkModuleId moduleId, JkVersion version) {
	return new JkVersionedModule(moduleId, version);
    }

    /**
     * Creates a <code>JkVersionedModule</code> from a string formatted as
     * <code>groupId:name</code> and another one standing for the version.
     */
    public static final JkVersionedModule of(String groupAndName, String version) {
	final String[] items = groupAndName.split(":");
	if (items.length != 2) {
	    throw new IllegalArgumentException(groupAndName + " does not tespect format groupId:name");
	}
	return JkVersionedModule.of(JkModuleId.of(groupAndName), JkVersion.ofName(version));
    }

    /**
     * Creates a <code>JkVersionedModule</code> from a string formatted as
     * <code>groupId:name:version</code>.
     */
    public static final JkVersionedModule of(String description) {
	final String[] items = description.split(":");
	if (items.length != 3) {
	    throw new IllegalArgumentException(description + " does not tespect format groupId:name:version");
	}
	return JkVersionedModule.of(JkModuleId.of(items[0], items[1]), JkVersion.ofName(items[2]));
    }

    private final JkModuleId moduleId;

    private final JkVersion version;

    private JkVersionedModule(JkModuleId moduleId, JkVersion version) {
	super();
	this.moduleId = moduleId;
	this.version = version;
    }

    public JkModuleId moduleId() {
	return moduleId;
    }

    public JkVersion version() {
	return version;
    }

    public JkVersionedModule withVersion(JkVersion version) {
	return new JkVersionedModule(this.moduleId, version);
    }

    public JkVersionedModule withVersion(String version) {
	return new JkVersionedModule(this.moduleId, JkVersion.ofName(version));
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

}
