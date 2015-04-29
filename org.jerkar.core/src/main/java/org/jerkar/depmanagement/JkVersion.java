package org.jerkar.depmanagement;

import java.net.URL;

import org.jerkar.utils.JkUtilsAssert;
import org.jerkar.utils.JkUtilsIO;
import org.jerkar.utils.JkUtilsString;

public final class JkVersion implements Comparable<JkVersion> {

	public static final String DEFAULT_VERSION_RESOURCE_NAME = "version.txt";

	public static JkVersion named(String name) {
		return new JkVersion(name);
	}

	public static JkVersion fromResource(Class<?> clazz, String name) {
		return named(JkUtilsIO.read(clazz.getResource(name)).trim());
	}

	public static JkVersion fromResource(Class<?> clazz) {
		return named(JkUtilsIO.read(clazz.getResource(DEFAULT_VERSION_RESOURCE_NAME)).trim());
	}

	public static JkVersion fromOptionalResourceOrExplicit(Class<?> clazz, String explicit) {
		final URL versionResource = clazz.getResource(JkVersion.DEFAULT_VERSION_RESOURCE_NAME);
		if (versionResource != null) {
			final String version = JkUtilsIO.read(versionResource).trim();
			if (!JkUtilsString.isBlank(version)) {
				return JkVersion.named(version);
			}
		}
		return JkVersion.named(explicit);
	}

	private final String name;

	private JkVersion(String name) {
		super();
		JkUtilsAssert.notNull(name, "name can't be null");
		JkUtilsAssert.isTrue(!JkUtilsString.isBlank(name), "name can't ne blank");
		this.name = name;
	}

	public String name() {
		return name;
	}

	public boolean isSnapshot() {
		return this.name.toLowerCase().endsWith("-snapshot");
	}

	@Override
	public int compareTo(JkVersion other) {
		return name.compareTo(other.name);
	}

	public boolean isGreaterThan(JkVersion other) {
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
		final JkVersion other = (JkVersion) obj;
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
		return  name;
	}

}
