package org.jerkar.api.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility class for dealing with strings.
 * 
 * @author Jerome Angibaud
 */
public final class JkUtilsString {

    /**
     * Creates a string by concatenating items array of specified items,
     * separating each with the specified separator.
     */
    public static String join(String[] items, String separator) {
	return join(Arrays.asList(items), separator);
    }

    /**
     * Same as {@link #join(String[], String)} but expecting an {@link Iterable}
     * instead of an array
     */
    public static String join(Iterable<?> items, String separator) {
	final StringBuilder builder = new StringBuilder();
	final Iterator<?> it = items.iterator();
	while (it.hasNext()) {
	    builder.append(it.next().toString());
	    if (it.hasNext()) {
		builder.append(separator);
	    }
	}
	return builder.toString();
    }

    /**
     * Returns the specified string but upper-casing its first character.
     */
    public static String capitalize(String string) {
	if (string.isEmpty()) {
	    return string;
	}
	if (string.length() == 1) {
	    return string.toUpperCase();
	}
	final String first = string.substring(0, 1);
	final String remaining = string.substring(1);
	return first.toUpperCase() + remaining;
    }

    /**
     * Returns the first string out of the specified candidates matching the
     * specified string.
     */
    public static String firstMatching(String stringToMatch, String... candidates) {
	for (final String candidate : candidates) {
	    if (stringToMatch.contains(candidate)) {
		return candidate;
	    }
	}
	return null;
    }

    /**
     * Returns occurrence count of the specified character into the specified
     * string.
     */
    public static int countOccurence(String matchedString, char occurrence) {
	int count = 0;
	for (final char c : matchedString.toCharArray()) {
	    if (c == occurrence) {
		++count;
	    }
	}
	return count;
    }

    /**
     * Splits the specified String into an array by separating by the specified
     * delimiter. If <code>str</code> is <code>null</code> then it returns an
     * empty array.
     */
    public static String[] split(String str, String delimiters) {
	if (str == null) {
	    return new String[0];
	}
	final StringTokenizer st = new StringTokenizer(str, delimiters);
	final List<String> tokens = new ArrayList<String>();
	while (st.hasMoreTokens()) {
	    final String token = st.nextToken();
	    tokens.add(token);
	}
	return tokens.toArray(new String[tokens.size()]);
    }

    /**
     * Returns the substring after the last delimiter of the specified
     * occurrence. The delimiter is not part of the result.
     */
    public static String substringAfterLast(String string, String delimiter) {
	final int index = string.lastIndexOf(delimiter);
	if (index == -1 || string.endsWith(delimiter)) {
	    return "";
	}
	return string.substring(index + 1);
    }

    /**
     * Returns the substring before the first delimiter of the specified
     * occurrence. The delimiter is not part of the result.
     */
    public static String substringBeforeFirst(String string, String delimiter) {
	final int index = string.indexOf(delimiter);
	if (index == -1) {
	    return "";
	}
	return string.substring(0, index);
    }

    /**
     * Returns the substring after the first delimiter of the specified
     * occurrence. The delimiter is not part of the result.
     */
    public static String substringAfterFirst(String string, String delimiter) {
	final int index = string.indexOf(delimiter);
	if (index == -1) {
	    return "";
	}
	return string.substring(index + delimiter.length());
    }

    /**
     * Returns the substring before the last delimiter of the specified
     * occurrence. The delimiter is not part of the result.
     */
    public static String substringBeforeLast(String string, String delimiter) {
	final int index = string.lastIndexOf(delimiter);
	if (index == -1 || string.startsWith(delimiter)) {
	    return "";
	}
	return string.substring(0, index);
    }

    public static String repeat(String pattern, int count) {
	final StringBuilder builder = new StringBuilder();
	for (int i = 0; i < count; i++) {
	    builder.append(pattern);
	}
	return builder.toString();
    }

    /**
     * Create an instance of the specified type from its string value. For now
     * handled types are :
     * <ul>
     * <li>primitive Wrapper types</li>
     * <li>{@link File}</li>
     * <li>Enum</li>
     * </ul>
     */
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
	    if (type.equals(File.class)) {
		return (T) new File(stringValue);
	    }
	} catch (final NumberFormatException e) {
	    throw new IllegalArgumentException(e.getMessage(), e);
	}
	if (type.isEnum()) {
	    @SuppressWarnings("rawtypes")
	    final Class enumType = type;
	    return (T) Enum.valueOf(enumType, stringValue);
	}
	throw new IllegalArgumentException("Can't handle type " + type);

    }

    private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4',
	    (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd',
	    (byte) 'e', (byte) 'f' };

    /**
     * Returns the hexadecimal for of the given array of bytes.
     * 
     * @throws IllegalArgumentException
     */
    public static String toHexString(byte[] raw) throws IllegalArgumentException {
	final byte[] hex = new byte[2 * raw.length];
	int index = 0;

	for (final byte b : raw) {
	    final int v = b & 0xFF;
	    hex[index++] = HEX_CHAR_TABLE[v >>> 4];
	    hex[index++] = HEX_CHAR_TABLE[v & 0xF];
	}
	try {
	    return new String(hex, "ASCII");
	} catch (final UnsupportedEncodingException e) {
	    throw new IllegalArgumentException("Illegal Hex string", e);
	}
    }

    /**
     * Returns <code>true</code> if any of the candidate string is equal to the
     * string to match.
     */
    public static boolean equalsAny(String stringToMatch, String... candidates) {
	for (final String candidate : candidates) {
	    if (stringToMatch.equals(candidate)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns <code>true</code> if the specified string ends with any of the
     * candidates.
     */
    public static boolean endsWithAny(String stringToMatch, String... candidates) {
	for (final String candidate : candidates) {
	    if (stringToMatch.endsWith(candidate)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns <code>true</code> if the specified string starts with any of the
     * candidates.
     */
    public static boolean startsWithAny(String stringToMatch, String... stringToCheckEquals) {
	for (final String candidate : stringToCheckEquals) {
	    if (stringToMatch.startsWith(candidate)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Checks if a String is whitespace, empty ("") or null.
     */
    public static boolean isBlank(String string) {
	if (string == null) {
	    return true;
	}
	return string.isEmpty() || " ".equals(string);
    }

    /**
     * Returns the specified string replacing the HTML special characters by
     * their respective code.
     */
    public static String escapeHtml(String s) {
	final StringBuilder out = new StringBuilder(Math.max(16, s.length()));
	for (int i = 0; i < s.length(); i++) {
	    final char c = s.charAt(i);
	    if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
		out.append("&#");
		out.append((int) c);
		out.append(';');
	    } else {
		out.append(c);
	    }
	}
	return out.toString();
    }

    public static String elipse(String string, int max) {
	if (string.length() <= max) {
	    return string;
	}
	return string.substring(0, max) + "...";
    }

    /**
     * Kindly borrowed from ANT
     */
    public static String[] translateCommandline(String toProcess) {
	if (toProcess == null || toProcess.length() == 0) {
	    // no command? no string
	    return new String[0];
	}
	// parse with a simple finite state machine

	final int normal = 0;
	final int inQuote = 1;
	final int inDoubleQuote = 2;
	int state = normal;
	final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
	final ArrayList<String> result = new ArrayList<String>();
	final StringBuilder current = new StringBuilder();
	boolean lastTokenHasBeenQuoted = false;

	while (tok.hasMoreTokens()) {
	    final String nextTok = tok.nextToken();
	    switch (state) {
	    case inQuote:
		if ("\'".equals(nextTok)) {
		    lastTokenHasBeenQuoted = true;
		    state = normal;
		} else {
		    current.append(nextTok);
		}
		break;
	    case inDoubleQuote:
		if ("\"".equals(nextTok)) {
		    lastTokenHasBeenQuoted = true;
		    state = normal;
		} else {
		    current.append(nextTok);
		}
		break;
	    default:
		if ("\'".equals(nextTok)) {
		    state = inQuote;
		} else if ("\"".equals(nextTok)) {
		    state = inDoubleQuote;
		} else if (" ".equals(nextTok)) {
		    if (lastTokenHasBeenQuoted || current.length() != 0) {
			result.add(current.toString());
			current.setLength(0);
		    }
		} else {
		    current.append(nextTok);
		}
		lastTokenHasBeenQuoted = false;
		break;
	    }
	}
	if (lastTokenHasBeenQuoted || current.length() != 0) {
	    result.add(current.toString());
	}
	if (state == inQuote || state == inDoubleQuote) {
	    throw new IllegalArgumentException("unbalanced quotes in " + toProcess);
	}
	return result.toArray(new String[result.size()]);
    }

}
