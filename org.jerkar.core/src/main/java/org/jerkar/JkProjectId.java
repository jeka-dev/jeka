package org.jerkar;

import org.jerkar.utils.JkUtilsString;

/**
 * Identifier for project. The identifier will be used to name the generated artifacts and
 * as a moduleId for Maven or Ivy.
 * 
 * @author Jerome Angibaud
 */
public final class JkProjectId {

	/**
	 * Creates a project id according the specified group and name.
	 */
	public static JkProjectId of(String group, String name) {
		return new JkProjectId(group, name);
	}

	/**
	 * Creates a project id according a string supposed to be formatted as <code>group</code>.<code>name</code>.
	 * The last '.' is considered as the separator between the group and the name. <br/>
	 * If there is no '.' then the whole string will serve both for group and name.
	 */
	public static JkProjectId of(String groupAndName) {
		if(groupAndName.contains(".")) {
			final String group = JkUtilsString.substringBeforeLast(groupAndName, ".");
			final String name = JkUtilsString.substringAfterLast(groupAndName, ".");
			return new JkProjectId(group, name);
		}
		return new JkProjectId(groupAndName, groupAndName);
	}

	private final String group;

	private final String name;

	private JkProjectId(String group, String name) {
		super();
		this.group = group;
		this.name = name;
	}

	public String group() {
		return group;
	}

	public String name() {
		return name;
	}

	public String fullName() {
		return name + "." + group;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
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
		final JkProjectId other = (JkProjectId) obj;
		if (group == null) {
			if (other.group != null) {
				return false;
			}
		} else if (!group.equals(other.group)) {
			return false;
		}
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
		return fullName();
	}

}
