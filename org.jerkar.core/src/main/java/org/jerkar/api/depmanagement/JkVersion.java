package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.net.URL;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsString;

public final class JkVersion implements Comparable<JkVersion>, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_VERSION_RESOURCE_NAME = "version.txt";

    public static JkVersion ofName(String name) {
        return new JkVersion(name);
    }

    public static JkVersion fromResource(Class<?> clazz, String name) {
        final URL url = clazz.getResource(name);
        return ofName(JkUtilsIO.read(url).trim());
    }

    public static JkVersion fromResourceOrNull(Class<?> clazz, String name) {
        final URL url = clazz.getResource(name);
        if (url == null) {
            return null;
        }
        final String versionName = JkUtilsIO.read(url).trim();
        if (JkUtilsString.isBlank(versionName)) {
            return null;
        }
        return JkVersion.ofName(versionName);
    }

    public static JkVersion fromResourceOrNull(Class<?> clazz) {
        return fromResourceOrNull(clazz, DEFAULT_VERSION_RESOURCE_NAME);
    }

    public static JkVersion fromResource(Class<?> clazz) {
        return fromResource(clazz, DEFAULT_VERSION_RESOURCE_NAME);
    }

    public static JkVersion firstNonNull(String first, String second) {
        if (!JkUtilsString.isBlank(first)) {
            return JkVersion.ofName(first);
        }
        if (!JkUtilsString.isBlank(second)) {
            return JkVersion.ofName(second);
        }
        throw new IllegalArgumentException("Both first and second can't be null");
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
        return name;
    }

}
