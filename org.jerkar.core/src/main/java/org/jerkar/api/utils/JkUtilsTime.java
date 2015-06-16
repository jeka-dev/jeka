package org.jerkar.api.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class to deal with time.
 * 
 * @author Jerome Angibaud
 */
public class JkUtilsTime {

	/**
	 * Returns the current system date
	 */
	public static Date now() {
		return new Date();
	}

	/**
	 * Returns the current system date as string with the specified format
	 */
	public static String  now(String pattern) {
		return new SimpleDateFormat(pattern).format(now());
	}

	/**
	 * Formats the specified date as <code>yyyyMMdd-HHmmss</code>
	 */
	public static String timestampSec(Date date) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
		return dateFormat.format(date);
	}

	/**
	 * Returns the duration in second between the specified nano time and now.
	 */
	public static float durationInSeconds(long startNano) {
		final long endNano = System.nanoTime();
		final long durationMillis = (endNano - startNano)/ 1000000;
		final float duration = durationMillis / 1000f;
		return duration;
	}

	/**
	 * Returns the duration in millisecond between the specified nano time and now.
	 */
	public static long durationInMillis(long startNano) {
		final long endNano = System.nanoTime();
		return (endNano - startNano)/ 1000000;
	}

}
