package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Used to specify a module version. Versions are comparable.
 *
 * @author Jerome Angibaud
 */
public final class JkVersion implements Comparable<JkVersion> {

    /** Mention that the version is unspecified */
    public static final JkVersion UNSPECIFIED = new JkVersion("UNSPECIFIED-SNAPSHOT");

    /**
     * Creates a {@link JkVersion} with the specified value. If specified name is null, then it creates
     * an {@link #UNSPECIFIED} version.
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
     * Returns the value of the version.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns <code>true</code> if this version stands for a snapshot one.
     */
    public boolean isSnapshot() {
        return this.value.toLowerCase().endsWith("-snapshot");
    }

    @Override
    public int compareTo(JkVersion other) {
        if (this.isUnspecified()) {
            if (other.isUnspecified()) {
                return 0;
            }
            return -1;
        } else if (other.isUnspecified()) {
            return 1;
        }
        return value.compareTo(other.value);
    }

    /**
     * Returns <code>true</code> if this version is to be considered superior to the specified one.
     */
    public boolean isGreaterThan(JkVersion other) {
        return this.compareTo(other) > 0;
    }

    /**
     * Returns <code>true</code> if the definition stands for a dynamic version
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
     * Returns <code>true</code> if this version range is unspecified.
     */
    public boolean isUnspecified() {
        return this.equals(UNSPECIFIED);
    }

    /**
     * Returns <code>true</code> if the definition stands for dynamic resolvable
     * version (as 1.4.+, [1.0, 2.0[, ...).<br/>
     * Returns <code>false</code> if the version is static or snapshot (as
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

    /**
     * If version name is formatted of blocks separated with '.', this methods returns the block at specified index.
     * throw {@link IllegalArgumentException} if no such block found at specified index.
     */
    public String getBlock(int index) {
        String[] items = this.value.split("\\.");
        if (index >= items.length) {
            throw new IllegalArgumentException("Version " + this.value + " does not contains " + (index+1) + " blocks separated with '.'.");
        }
        return items[index];
    }

    /**
     * Returns true when {@link #getBlock(int)} won't throws an {@link IllegalArgumentException}.
     */
    public boolean hasBlockAt(int index) {
        String[] items = this.value.split("\\.");
        return index < items.length;
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
            return other.value == null;
        } else return value.equals(other.value);
    }

    @Override
    public String toString() {
        return value;
    }

}
