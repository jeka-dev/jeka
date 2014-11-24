package org.jake.depmanagement;

import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsString;

public final class JakeVersion implements Comparable<JakeVersion> {

	public static JakeVersion of(String name) {
		return new JakeVersion(name);
	}

	private final String name;

	private JakeVersion(String name) {
		super();
		JakeUtilsAssert.notNull(name, "name can't be null");
		JakeUtilsAssert.isTrue(!JakeUtilsString.isBlank(name), "name can't ne blank");
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public int compareTo(JakeVersion other) {
		return name.compareTo(other.name);
	}

	public boolean isGreaterThan(JakeVersion other) {
		return this.compareTo(other) > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		final JakeVersion other = (JakeVersion) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "JakeVersion [name=" + name + "]";
	}

}
