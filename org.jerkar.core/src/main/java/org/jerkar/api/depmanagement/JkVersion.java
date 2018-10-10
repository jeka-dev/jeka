package org.jerkar.api.depmanagement;

import java.io.Serializable;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Used to specify a module projectVersion. Versions are comparable.
 *
 * @author Jerome Angibaud
 */
public final class JkVersion implements Comparable<JkVersion>, Serializable {

    private static final long serialVersionUID = 1L;

    /** Mention that the projectVersion is unspecified */
    public static final JkVersion UNSPECIFIED = new JkVersion("UNSPECIFIED-SNAPSHOT");

    /**
     * Creates a {@link JkVersion} with the specified value.
     */
    public static JkVersion of(String name) {
        if (name == null) {
            return UNSPECIFIED;
        }
        return new JkVersion(name);
    }

    private final String value;

    private JkVersion(String value) {
        super();
        JkUtilsAssert.notNull(value, "value can't be null");
        JkUtilsAssert.isTrue(!JkUtilsString.isBlank(value), "value can't ne blank");
        this.value = value;
    }

    /**
     * Returns the value of the projectVersion.
     */
    public String value() {
        return value;
    }

    /**
     * Returns <code>true</code> if this projectVersion stands for a snapshot one.
     */
    public boolean isSnapshot() {
        return this.value.toLowerCase().endsWith("-snapshot");
    }

    @Override
    public int compareTo(JkVersion other) {
        return value.compareTo(other.value);
    }

    /**
     * Returns <code>true</code> if this projectVersion is to be considered superior to the specified one.
     */
    public boolean isGreaterThan(JkVersion other) {
        return this.compareTo(other) > 0;
    }

    /**
     * Returns <code>true</code> if the definition stands for a dynamic projectVersion
     * (as "1.4.+", "[1.0,2.0[", "3.0-SNAPSHOT", ...) or <code>false</code> if
     * it stands for a fixed one (as "1.4.0, "2.0.3-23654114", ...).
     */
    public boolean isDynamic() {
        if (value.endsWith("-SNAPSHOT")) {
            return true;
        }
        return this.isDynamicAndResovable();
    }

    /**
     * Returns <code>true</code> if this projectVersion range is unspecified.
     */
    public boolean isUnspecified() {
        return this.equals(UNSPECIFIED);
    }

    /**
     * Returns <code>true</code> if the definition stands for dynamic resolvable
     * projectVersion (as 1.4.+, [1.0, 2.0[, ...).<br/>
     * . Returns <code>false</code> if the projectVersion is static or snapshot (as
     * 1.4.0, 3.1-SNAPSHOT) A snapshot is not considered as 'resolvable'.
     */
    public boolean isDynamicAndResovable() {
        if ("+".equals(value)) {
            return true;
        }
        if (JkUtilsString.endsWithAny(value, ".+", ")", "]", "[")) {
            return true;
        }
        if (value.startsWith("latest.")) {
            return true;
        }
        return JkUtilsString.startsWithAny(value, "[", "]", "(")
                && JkUtilsString.endsWithAny(value, ")", "]", "[");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return value;
    }

}
