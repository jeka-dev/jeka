package org.jake.depmanagement;

public class JakeVersionedModule {

	public static JakeVersionedModule of(JakeModuleId moduleId, JakeVersion version) {
		return new JakeVersionedModule(moduleId, version);
	}

	private final JakeModuleId moduleId;

	private final JakeVersion version;

	private JakeVersionedModule(JakeModuleId moduleId, JakeVersion version) {
		super();
		this.moduleId = moduleId;
		this.version = version;
	}

	public JakeModuleId moduleId() {
		return moduleId;
	}

	public JakeVersion version() {
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
		final JakeVersionedModule other = (JakeVersionedModule) obj;
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
		return "JakeVersionedModule [moduleId=" + moduleId + ", version="
				+ version + "]";
	}

}
