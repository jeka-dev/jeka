package org.jerkar.api.depmanagement;

import java.io.Serializable;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Expresses a version constraints for a given external modules. It can be an
 * exact version as 1.4.2 or a dynamic version as [1.0,2.0[. As this tool relies
 * on Ivy to to perform dependency resolution, you can use any syntax accepted
 * by Ivy.
 *
 * @see <a href=
 *      "http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.html">
 *      ivy doc</a>
 *
 * @author Jerome Angibaud
 */
public final class JkVersionRange implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Mention that the version is unspecified */
    public static final JkVersionRange UNSPECIFIED = new JkVersionRange("unspecified");

    /**
     * Creates a version range to String expression described at :
     * http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.
     * html.
     */
    public static JkVersionRange of(String definition) {
        return new JkVersionRange(definition);
    }

    private final String definition;

    private JkVersionRange(String definition) {
        JkUtilsAssert.isTrue(!JkUtilsString.isBlank(definition),
                "Can't instantatiate a version range with a blank or null definition.");
        this.definition = definition;
    }

    /**
     * Returns the range definition as string. For example "1.4.2" or "3.2.+".
     */
    public String definition() {
        return definition;
    }

    /**
     * Returns <code>true</code> if the definition stands for a dynamic version
     * (as "1.4.+", "[1.0,2.0[", "3.0-SNAPSHOT", ...) or <code>false</code> if
     * it stands for a fixed one (as "1.4.0, "2.0.3-23654114", ...).
     */
    public boolean isDynamic() {
        if (definition.endsWith("-SNAPSHOT")) {
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
     * . Returns <code>false</code> if the version is static or snapshot (as
     * 1.4.0, 3.1-SNAPSHOT) A snapshot is not considered as 'resolvable'.
     */
    public boolean isDynamicAndResovable() {
        if ("+".equals(definition)) {
            return true;
        }
        if (JkUtilsString.endsWithAny(definition, ".+", ")", "]", "[")) {
            return true;
        }
        if (definition.startsWith("latest.")) {
            return true;
        }
        if (JkUtilsString.startsWithAny(definition, "[", "]", "(")
                && JkUtilsString.endsWithAny(definition, ")", "]", "[")) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return definition;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((definition == null) ? 0 : definition.hashCode());
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
        final JkVersionRange other = (JkVersionRange) obj;
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
