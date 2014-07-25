package org.jake.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class JakeUtilsString {

	public static String[] split(String str, String delimiters) {
		final StringTokenizer st = new StringTokenizer(str, delimiters);
		final List<String> tokens = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			final String token = st.nextToken();
			tokens.add(token);
		}
		return tokens.toArray(new String[tokens.size()]);
	}

	public static String repeat(String pattern, int count) {
		final StringBuilder builder = new StringBuilder();
		for (int i=0; i<count; i++) {
			builder.append(pattern);
		}
		return builder.toString();
	}

}
