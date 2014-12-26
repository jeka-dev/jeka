package org.jake.depmanagement;


public final class JakeVersionRange {

	public static JakeVersionRange of(String definition) {
		return new JakeVersionRange(definition);
	}

	private final String definition;

	private JakeVersionRange(String versionRange) {
		this.definition = versionRange;
	}

	public String definition() {
		return definition;
	}

	@Override
	public String toString() {
		return definition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((definition == null) ? 0 : definition.hashCode());
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
		final JakeVersionRange other = (JakeVersionRange) obj;
		if (definition == null) {
			if (other.definition != null) {
				return false;
			}
		} else if (!definition.equals(other.definition)) {
			return false;
		}
		return true;
	}



}
