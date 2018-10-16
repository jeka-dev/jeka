package org.jerkar.api.java;

import org.jerkar.api.utils.JkUtilsAssert;

/**
 * Java specification projectVersion
 */
public final class JkJavaVersion {

    /**
     * Creates a Java specification projectVersion from the specified name.
     */
    public static JkJavaVersion of(String value) {
        JkUtilsAssert.notNull(value, "projectVersion name can't be null. Use 7, 8, ...");
        return new JkJavaVersion(value);
    }

    /** Stands for Java projectVersion 1.3 */
    public static JkJavaVersion V1_3 = JkJavaVersion.of("1.3");

    /** Stands for Java projectVersion 1.4 */
    public  static final JkJavaVersion V1_4 = JkJavaVersion.of("1.4");

    /** Stands for Java projectVersion 5 */
    public static final JkJavaVersion V5 = JkJavaVersion.of("5");

    /** Stands for Java projectVersion 6 */
    public static final JkJavaVersion V6 = JkJavaVersion.of("6");

    /** Stands for Java projectVersion 7 */
    public static final JkJavaVersion V7 = JkJavaVersion.of("7");

    /** Stands for Java projectVersion 8 */
    public static final JkJavaVersion V8 = JkJavaVersion.of("8");

    /** Stands for Java projectVersion 9 */
    public static final JkJavaVersion V9 = JkJavaVersion.of("9");

    /** Stands for Java Version  10 */
    public static final JkJavaVersion V10 = JkJavaVersion.of("10");

    private final String value;

    private JkJavaVersion(String value) {
        this.value = value;
    }

    /**
     * Returns literal of this projectVersion.
     */
    public String get() {
        return value;
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

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
