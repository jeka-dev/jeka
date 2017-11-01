package org.jerkar.api.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class to deal with time.
 * 
 * @author Jerome Angibaud
 */
public final class JkUtilsTime {

    private JkUtilsTime() {}

    /**
     * Returns the current system date
     */
    public static Date now() {
        return new Date();
    }

    /**
     * Returns the current system date as string with the specified format
     */
    public static String now(String pattern) {
        return new SimpleDateFormat(pattern).format(now());
    }

    /**
     * Returns the current system date as string with the specified format
     */
    public static String nowUtc(String pattern) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(now());
    }

    /**
     * Returns the duration in second between the specified nano time and now.
     */
    public static float durationInSeconds(long startNano) {
        final long endNano = System.nanoTime();
        final long durationMillis = (endNano - startNano) / 1000000;
        return durationMillis / 1000f;
    }

    /**
     * Returns the duration in millisecond between the specified nano time and
     * now.
     */
    public static long durationInMillis(long startNano) {
        final long endNano = System.nanoTime();
        return (endNano - startNano) / 1000000;
    }

}
