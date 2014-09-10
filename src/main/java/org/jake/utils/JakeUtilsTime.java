package org.jake.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class JakeUtilsTime {

	public static String timestampSec() {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
		return dateFormat.format(new Date());
	}

	public static float durationInSeconds(long startNano) {
		final long endNano = System.nanoTime();
		final long durationMillis = (endNano - startNano)/ 1000000;
		final float duration = durationMillis / 1000f;
		return duration;
	}

	public static long durationInMillis(long startNano) {
		final long endNano = System.nanoTime();
		return (endNano - startNano)/ 1000000;
	}

}
