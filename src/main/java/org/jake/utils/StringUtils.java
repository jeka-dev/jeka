package org.jake.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class StringUtils {
	
	public static String[] split(String str, String delimiters) {
		StringTokenizer st = new StringTokenizer(str, delimiters);
		List<String> tokens = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			tokens.add(token);
		}
		return tokens.toArray(new String[tokens.size()]);
	}

}
