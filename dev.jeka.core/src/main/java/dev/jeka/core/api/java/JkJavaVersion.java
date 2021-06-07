package dev.jeka.core.api.java;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.utils.JkUtilsAssert;

/**
 * Java specification version
 */
public final class JkJavaVersion implements Comparable<JkJavaVersion> {

    /**
     * Creates a Java specification version from the specified name.
     */
    public static JkJavaVersion of(String value) {
        JkUtilsAssert.argument(value != null, "version name can't be null. Use 8, 9, 10, 11...");
        return new JkJavaVersion(value);
    }

    /** Stands for Java version 8 */
    public static final JkJavaVersion V8 = JkJavaVersion.of("8");

    /** Stands for Java version 9 */
    public static final JkJavaVersion V9 = JkJavaVersion.of("9");

    /** Stands for Java Version  10 */
    public static final JkJavaVersion V10 = JkJavaVersion.of("10");

    /** Stands for Java Version  11 */
    public static final JkJavaVersion V11 = JkJavaVersion.of("11");

    /** Stands for Java Version  12 */
    public static final JkJavaVersion V12 = JkJavaVersion.of("12");

    /** Stands for Java Version  13 */
    public static final JkJavaVersion V13 = JkJavaVersion.of("13");

    /** Stands for Java Version  14 */
    public static final JkJavaVersion V14 = JkJavaVersion.of("14");

    /** Stands for Java Version  15 */
    public static final JkJavaVersion V15 = JkJavaVersion.of("15");

    /** Stands for Java Version  16 */
    public static final JkJavaVersion V16 = JkJavaVersion.of("16");

    private final String value;

    private JkJavaVersion(String value) {
        this.value = value;
    }

    /**
     * Returns literal of this version.
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

    @Override
    public int compareTo(JkJavaVersion o) {
        return JkVersion.of(value).compareTo(JkVersion.of(o.value));
    }


}
