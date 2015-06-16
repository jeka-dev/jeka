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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((moduleId == null) ? 0 : moduleId.hashCode());
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
		return moduleId+":"+version;
	}

}
