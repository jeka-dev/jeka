package dev.jeka.core.api.utils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Utility class for dealing with strings.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsString {

    /**
     * Joins the elements of the given iterable into a single string using the specified separator.
     *
     * @param items     the iterable containing the elements to be joined
     * @param separator the string to be used as separator between the elements
     */
    static String join(Iterable<?> items, String separator) {
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
     * Returns the specified string but lower-casing its first character.
     */
    public static String uncapitalize(String string) {
        if (string.isEmpty()) {
            return string;
        }
        if (string.length() == 1) {
            return string.toLowerCase();
        }
        final String first = string.substring(0, 1);
        final String remaining = string.substring(1);
        return first.toLowerCase() + remaining;
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
     * Returns occurrence count of the specified character into the specified string.
     */
    public static int countOccurrence(String matchedString, char occurrence) {
        int count = 0;
        for (final char c : matchedString.toCharArray()) {
            if (c == occurrence) {
                ++count;
            }
        }
        return count;
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
        return string.substring(index + delimiter.length());
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
     * Returns empty string if not found.
     */
    public static String substringBeforeLast(String string, String delimiter) {
        final int index = string.lastIndexOf(delimiter);
        if (index == -1 || string.startsWith(delimiter)) {
            return "";
        }
        return string.substring(0, index);
    }

    /**
     * Returns a string made of the specified pattern repeat the
     * specified count. So, for example, <code>repeat("##", 3)</code> will return <i>######</i>
     */
    public static String repeat(String pattern, int count) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(pattern);
        }
        return builder.toString();
    }

    /**
     * Returns a string containing the quantity and noun. The noun is the plural form if
     * quantity > 1.
     */
    public static String pluralize(int count, String singular, String plural) {
        if (count > 1) {
            return count + " " + plural;
        }
        return count + " " + singular;
    }

    /**
     * Returns a string containing the quantity and noun. The noun is the plural form if
     * quantity greater than 1. The plural form is singular form + 's';
     */
    public static String pluralize(int count, String singular) {
        return pluralize(count, singular, singular + 's');
    }

    private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3',
            (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a',
            (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

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
        return new String(hex, StandardCharsets.US_ASCII);
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
     * Checks if the provided string consists only of digits.
     */
    public static boolean isDigits(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    /**
     * Extracts variable tokens from the given string.
     * <p>
     * Variable tokens are string embedded within <code>${}</code>.
     *
     * @param string The input string.
     */
    public static List<String> extractVariableToken(String string) {
        boolean onDollar = false;
        boolean inToken = false;
        String currentToken = "";
        List<String> result = new LinkedList<>();
        for (int i = 0; i < string.length(); i++) {
            char currentChar = string.charAt(i);
            if (inToken) {
                if (currentChar == '}') {
                    result.add(currentToken);
                    currentToken = "";
                    inToken = false;
                } else {
                    currentToken = currentToken + currentChar;
                }
            } else if (onDollar) {
                if (currentChar == '{') {
                    inToken = true;
                }
            }
            onDollar = currentChar == '$';
        }
        return result;
    }

    /**
     * Interpolates a string by replacing variable tokens with their corresponding values.
     *
     * @param string   The string to interpolate.
     * @param replacer The function used to retrieve the value for each variable token.
     *
     * @return The interpolated string.
     */
    public static String interpolate(String string, Function<String, String> replacer) {
        List<String> variableTokens = JkUtilsString.extractVariableToken(string);
        String result = string;
        for (String token : variableTokens) {
            String value = replacer.apply(token);
            if (token != null) {
                result = result.replace("${" + token + "}", value);
            }
        }
        return result;
    }

    /**
     * Returns the specified string truncated and ending with <i>...</i> if the specified
     * string is longer than the specified max length. Otherwise, the specified string is returned as is.
     */
    public static String ellipse(String string, int max) {
        if (string.length() <= max || max < 0) {
            return string;
        }
        return string.substring(0, max) + "...";
    }

    /**
     * Parses a command line string and splits it into an array of individual command line arguments.
     * <p>
     * Borrowed from ANT.
     *
     * @param commandline the command line string to parse
     * @return an array of individual command line arguments
     *
     * @throws IllegalArgumentException if there are unbalanced quotes in the command line string
     */
    public static String[] parseCommandline(String commandline) {
        if (commandline == null || commandline.isEmpty()) {
            // no command? no string
            return new String[0];
        }
        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(commandline, "\"' ", true);
        final ArrayList<String> result = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            final String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("'".equals(nextTok)) {
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
                if ("'".equals(nextTok)) {
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
            throw new IllegalArgumentException("unbalanced quotes in " + commandline);
        }
        return result.toArray(new String[0]);
    }

    /**
     * Pads the given string with the specified pad character to ensure it reaches the minimum length.
     *
     * @param string The string to pad.
     * @param minLength The minimum length the resulting string should have.
     * @param padChar The character to use for padding.
     *
     * @return The padded string.
     */
    public static String padEnd(String string, int minLength, char padChar) {
        if (string.length() >= minLength) {
            return string;
        }
        StringBuilder sb = new StringBuilder(minLength);
        sb.append(string);
        for (int i = string.length(); i < minLength; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    /**
     * Pads the given string with the specified pad character to ensure it reaches the minimum length.
     *
     * @param string     The string to pad.
     * @param minLength  The minimum length the resulting string should have.
     * @param padChar    The character to use for padding.
     *
     * @return The padded string.
     */
    public static String padStart(String string, int minLength, char padChar) {
        if (string.length() >= minLength) {
            return string;
        }
        StringBuilder sb = new StringBuilder(minLength);
        for (int i = string.length(); i < minLength; i++) {
            sb.append(padChar);
        }
        sb.append(string);
        return sb.toString();
    }

    /**
     * Converts a blank string to null. If the input string is null, empty, or consists only of whitespaces,
     * this method returns null. Otherwise, it returns the input string.
     *
     * @param in the input string
     */
    public static String blankToNull(String in) {
        return isBlank(in) ? null : in;
    }

    /**
     * Converts a null string to an empty string. If the input string is null, it returns an empty string.
     * If the input string is not null, it returns the input string as is.
     *
     * @param in the input string
     */
    public static String nullToEmpty(String in) {
        return in == null ? "" : in;
    }

    /**
     * Wraps the given string character-wise to fit within the specified width.
     */
    public static String wrapStringCharacterWise(String input, int maxLineLength) {
        StringBuilder stringBuilder = new StringBuilder(input);
        int index = 0;
        while(stringBuilder.length() > index + maxLineLength) {
            int lastLineReturn = stringBuilder.lastIndexOf("\n", index + maxLineLength);
            if (lastLineReturn > index) {
                index = lastLineReturn;
            } else {
                index = stringBuilder.lastIndexOf(" ", index + maxLineLength);
                if (index == -1) {

                    // One word is too long to fit, try best effort
                    int newMaxLength = Arrays.stream(input.split(" |\n"))
                            .map(String::length)
                            .max(Comparator.naturalOrder())
                            .orElse(input.length() +1);
                    return wrapStringCharacterWise(input, newMaxLength);
                }
                stringBuilder.replace(index, index + 1, "\n");
                index++;
            }
        }
        return stringBuilder.toString();
    }

}
