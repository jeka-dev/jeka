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

	@SuppressWarnings("unchecked")
	public static <T> T parse(Class<T> type, String stringValue) throws IllegalArgumentException {
		if (type.equals(String.class)) {
			return (T) stringValue;
		}

		if (type.equals(Boolean.class) || type.equals(boolean.class)) {
			return (T) Boolean.valueOf(stringValue);
		}
		try {
			if (type.equals(Integer.class) || type.equals(int.class)) {
				return (T) Integer.valueOf(stringValue);
			}
			if (type.equals(Long.class) || type.equals(long.class)) {
				return (T) Long.valueOf(stringValue);
			}
			if (type.equals(Short.class) || type.equals(short.class)) {
				return (T) Short.valueOf(stringValue);
			}
			if (type.equals(Byte.class) || type.equals(byte.class)) {
				return (T) Byte.valueOf(stringValue);
			}
			if (type.equals(Double.class) || type.equals(double.class)) {
				return (T) Double.valueOf(stringValue);
			}
			if (type.equals(Float.class) || type.equals(float.class)) {
				return (T) Float.valueOf(stringValue);
			}
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		if (type.isEnum()) {
			@SuppressWarnings("rawtypes")
			final
			Class enumType = type;
			return (T) Enum.valueOf(enumType, stringValue);
		}
		throw new IllegalArgumentException("Can't handle type " + type);

	}

	public static String toString(Object object) {
		if (object == null) {
			return "null";
		}
		return object.toString();
	}

}
