package org.jerkar.api.java;

import org.jerkar.api.utils.JkUtilsAssert;

/**
 * Java specification projectVersion
 */
public final class JkJavaVersion {

    /**
     * Creates a Java specification projectVersion from the specified name.
     */
    public static JkJavaVersion name(String value) {
        JkUtilsAssert.notNull(value, "projectVersion name can't be null. Use 7, 8, ...");
        return new JkJavaVersion(value);
    }

    /** Stands for Java projectVersion 1.3 */
    public static JkJavaVersion V1_3 = JkJavaVersion.name("1.3");

    /** Stands for Java projectVersion 1.4 */
    public  static final JkJavaVersion V1_4 = JkJavaVersion.name("1.4");

    /** Stands for Java projectVersion 5 */
    public static final JkJavaVersion V5 = JkJavaVersion.name("5");

    /** Stands for Java projectVersion 6 */
    public static final JkJavaVersion V6 = JkJavaVersion.name("6");

    /** Stands for Java projectVersion 7 */
    public static final JkJavaVersion V7 = JkJavaVersion.name("7");

    /** Stands for Java projectVersion 8 */
    public static final JkJavaVersion V8 = JkJavaVersion.name("8");

    /** Stands for Java projectVersion 8 */
    public static final JkJavaVersion V9 = JkJavaVersion.name("9");

    private final String name;

    private JkJavaVersion(String name) {
        this.name = name;
    }

    /**
     * Returns literal of this projectVersion.
     */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JkJavaVersion that = (JkJavaVersion) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
