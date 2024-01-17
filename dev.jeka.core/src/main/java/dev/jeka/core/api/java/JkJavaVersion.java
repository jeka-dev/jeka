package dev.jeka.core.api.java;

import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Java specification version
 */
public final class JkJavaVersion implements Comparable<JkJavaVersion> {

    /**
     * Creates a Java specification version from the specified name.
     */
    public static JkJavaVersion of(String stringValue) {
        if (stringValue.startsWith("1.8")) {
            return new JkJavaVersion(8);
        }
        if (stringValue.contains(".")) {
            stringValue = JkUtilsString.substringBeforeFirst(stringValue, ".");
        }
        try {
            int value = Integer.parseInt(stringValue);
            return new JkJavaVersion(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Java version should be an integer as 8, 11, 12,... was " + stringValue);
        }
    }



    public static JkJavaVersion ofCurrent() {
        return JkJavaVersion.of(System.getProperty("java.version"));
    }

    /** Stands for Java version 8 */
    public static final JkJavaVersion V8 = JkJavaVersion.of("8");

    /** Stands for Java Version  11 */
    public static final JkJavaVersion V11 = JkJavaVersion.of("11");

    /** Stands for Java Version  17 */
    public static final JkJavaVersion V17 = JkJavaVersion.of("17");

    /**
     * Stands for Java Version  21
     */
    public static final JkJavaVersion V21 = JkJavaVersion.of("21");

    private final int value;

    private JkJavaVersion(int value) {
        this.value = value;
    }

    /**
     * Returns literal of this version.
     */
    public int get() {
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

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public int compareTo(JkJavaVersion o) {
        return Integer.compare(this.value, o.value);
    }

    /**
     * Determines if the current Java version is equal to or greater than the specified Java version.
     */
    public boolean isEqualOrGreaterThan(JkJavaVersion other) {
        return this.compareTo(other) >= 0;
    }

}
