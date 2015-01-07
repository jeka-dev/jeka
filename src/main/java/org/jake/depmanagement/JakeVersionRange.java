package org.jake.depmanagement;

import org.jake.utils.JakeUtilsString;

/**
 * Expresses a version constraints for a given external modules. It can be an exact version as 1.4.2
 * or a dynamic version as latest.integration.
 * As this tool rely on Ivy to to perform dependency resolution, you can use any syntax accepted by Ivy.
 * 
 * @see http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.html
 * 
 * @author Jerome Angibaud
 */
public final class JakeVersionRange {

	/**
	 * Creates a version range from String expression described at : http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.html.
	 */
	public static JakeVersionRange of(String definition) {
		return new JakeVersionRange(definition);
	}

	private final String definition;

	private JakeVersionRange(String versionRange) {
		this.definition = versionRange;
	}

	/**
	 * Returns the range definition as string. For example "1.4.2" or "3.2.+".
	 */
	public String definition() {
		return definition;
	}

	/**
	 * Returns <code>true</code> if the definition stands for a dynamic version (as "1.4.+", "[1.0,2.0[", "3.0-SNAPSHOT", ...) or
	 * <code>false</code> if it stands for a fixed one (as "1.4.0, "2.0.3-23654114", ...).
	 */
	public boolean isDynamic() {
		if (definition.endsWith("-SNAPSHOT")) {
			return true;
		}
		return this.isDynamicAndResovable();
	}

	/**
	 * Returns <code>true</code> if the definition stands for a fixed version (as 1.4.2) or
	 * <code>false</code> if it stands for a dynamic one (as 1.4.+, 3.0-SNAPSHOT, [1.0, 2.0[, ...).
	 */
	public boolean isDynamicAndResovable() {
		if (JakeUtilsString.endsWithAny(definition, ".+", ")", "]", "[")) {
			return false;
		}
		if (definition.startsWith("latest.")) {
			return false;
		}
		if (JakeUtilsString.startsWithAny(definition, "[", "]", "(")
				&& JakeUtilsString.endsWithAny(definition, ")", "]", "[")) {
			return false;
		}
		return true;
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
