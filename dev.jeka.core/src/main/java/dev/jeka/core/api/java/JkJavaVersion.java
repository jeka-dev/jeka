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

    /** Stands for Java Version  17 */
    public static final JkJavaVersion V17 = JkJavaVersion.of("17");

    /** Stands for Java Version  18 */
    public static final JkJavaVersion V18 = JkJavaVersion.of("18");

    /** Stands for Java Version  19 */
    public static final JkJavaVersion V19 = JkJavaVersion.of("19");

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


}
