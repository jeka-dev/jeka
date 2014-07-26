package org.jake.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class JakeUtilsTime {

	public static String timestampSec() {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
		return dateFormat.format(new Date());
	}

}
