package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.net.URL;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Used to specify a module version. Versions are comparable.
 *
 * @author Jerome Angibaud
 */
public final class JkVersion implements Comparable<JkVersion>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link JkVersion} with the specified name.
     * @deprecated Use {@link #name()} instead.
     */
    @Deprecated
    public static JkVersion ofName(String name) {
        return new JkVersion(name);
    }

    /**
     * Creates a {@link JkVersion} with the specified name.
     */
    public static JkVersion name(String name) {
        return new JkVersion(name);
    }

    /**
     *
     * @param clazz
     * @param name
     * @return
     */
    public static JkVersion fromResource(Class<?> clazz, String name) {
        final URL url = clazz.getResource(name);
        return name(JkUtilsIO.read(url).trim());
    }

    private final String name;

    private JkVersion(String name) {
        super();
        JkUtilsAssert.notNull(name, "name can't be null");
        JkUtilsAssert.isTrue(!JkUtilsString.isBlank(name), "name can't ne blank");
        this.name = name;
    }

    /**
     * Returns the name of the version.
     */
    public String name() {
        return name;
    }

    /**
     * Returns <code>true</code> if this version stands for a snapshot one.
     */
    public boolean isSnapshot() {
        return this.name.toLowerCase().endsWith("-snapshot");
    }

    @Override
    public int compareTo(JkVersion other) {
        return name.compareTo(other.name);
    }

    /**
     * Returns <code>true</code> if this version is to be considered superior to the specified one.
     */
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
