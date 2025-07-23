/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Checks if the given string contains any of the specified candidate substrings.
     */
    public static boolean containsAny(String stringToMatch, String... candidates) {
        for (final String candidate : candidates) {
            if (stringToMatch.contains(candidate)) {
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
     * Checks if the given string starts with an uppercase letter.
     *
     * @param str the string to check; may be null or empty
     * @return {@code true} if the string starts with an uppercase letter, {@code false} otherwise
     */
    public static boolean startsWithUpperCase(String str) {
        if (str == null || str.isEmpty()) {
            return false; // Return false if the string is null or empty
        }
        char firstChar = str.charAt(0);
        return Character.isUpperCase(firstChar); // Check if the first character is uppercase
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
     * Extracts variable tokens enclosed in `${}` from the provided string.
     * A variable token is a string contained between `${` and `}`.
     *
     * @param string the input string from which to extract variable tokens
     * @return a list of variable tokens without the enclosing `${}`;
     *         returns an empty list if no variable tokens are present
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
     * Replaces variable tokens in the given string with the corresponding values
     * provided by the replacer function. Variable tokens are enclosed in `${}`.
     *
     * @param string the input string containing variable tokens to be replaced
     * @param replacer a function that takes a variable token (without `${}`)
     *                 and returns the replacement value for the token
     * @return the resulting string after variable replacement
     * @throws IllegalArgumentException if no replacement is defined for a token
     */
    public static String interpolate(String string, Function<String, String> replacer) {
        List<String> variableTokens = JkUtilsString.extractVariableToken(string);
        String result = string;
        for (String token : variableTokens) {
            String value = replacer.apply(token);
            if (token != null) {
                JkUtilsAssert.argument(value != null, "No replacement defined for token %s", token);
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
        if (string.length() <= 3) {
            return string;
        }
        if (string.length() <= max || max < 0) {
            return string;
        }
        return string.substring(0, max-3) + "...";
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
     * @see #parseCommandline(String) 
     */
    public static List<String> parseCommandlineAsList(String commandline) {
        return Arrays.asList(parseCommandline(commandline));
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
     * Converts an empty or blank string to null.
     * If the input string is non-empty and non-blank, it is returned unchanged.
     */
    public static String emptyToNull(String in) {
        return isBlank(in) ? null : in;
    }

    /**
     * Wraps the given string character-wise to fit within the specified width.
     */
    public static String wrapStringCharacterWise(String input, int maxLineLength) {
        StringBuilder stringBuilder = new StringBuilder(Optional.ofNullable(input).orElse(""));
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
                    if (newMaxLength >= maxLineLength) {
                        return input;
                    }
                    return wrapStringCharacterWise(input, newMaxLength);
                }
                stringBuilder.replace(index, index + 1, "\n");
                index++;
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Returns a string with a left margin added to each line.
     * Each line in the provided paragraph will be prepended with the specified margin.
     */
    public static String withLeftMargin(String paragraph, String margin) {
        String all = Arrays.stream(paragraph.split("\n"))
                .map(line -> margin + line)
                .reduce("",  (one, two) -> one + "\n" + two);
        return all.substring(1); // remove first 'br'
    }

    /**
     * Returns a readable representation of command line arguments.
     * Each option in the provided list is split by the platform's file separator and concatenated
     * with a new line character (\n).
     */
    public static String readableCommandAgs(String margin, List<String> options) {
        StringBuilder sb = new StringBuilder();
        options.stream()
                .flatMap(item -> Stream.of(item.split(File.pathSeparator)))
                .filter(item -> !JkUtilsString.isBlank(item))
                .forEach(item -> sb.append(margin + item + "\n"));
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() -1);
        }
        return sb.toString();
    }

    /**
     * Shortens a fully qualified package name by abbreviating all segments except the last class name
     * and appends the remaining part after the class, preserving the structure.
     * For instance, it replaces "com.example.package.ClassName.method" with "c.e.package.ClassName.method".
     *
     * @param fullName The fully qualified package name including the class and optionally the method.
     *                 If null or if the input does not contain any dot, it is returned as-is.
     * @return A shortened version of the given package name with abbreviated segments,
     *         or the original input for invalid cases.
     */
    public static String shortenPackageName(String fullName) {
        if (fullName == null || !fullName.contains(".")) {
            return fullName; // Return as-is for invalid input or no package
        }

        String[] parts = fullName.split("\\.");
        boolean found = false;
        List<String> resultParts = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) {
                resultParts.add("");

            } else if (found) {
                resultParts.add(part);

            } else if (startsWithUpperCase(part)) {
                found = true;
                resultParts.add(part);

            } else {
                resultParts.add(part.substring(0, 1));
            }
        }

        return String.join(".", resultParts);
    }

    /**
     * Removes the package prefix from a fully qualified name, leaving only the last class name
     * and the subsequent part (e.g., method name or variable). This method shortens the package
     * part by retaining only the class name preceding the final segment if it exists.
     *
     * @param fullName The fully qualified name from which the package prefix should be removed.
     *                 If the input is null or does not contain a dot, it is returned as-is.
     * @return The modified string with the package prefix removed, or the original string
     *         if no prefix exists or the input is invalid.
     */
    public static String removePackagePrefix(String fullName) {
        if (fullName == null || !fullName.contains(".")) {
            return fullName; // Return as-is for invalid input or no package
        }

        String[] parts = fullName.split("\\.");
        boolean found = false;
        List<String> resultParts = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) {
                resultParts.add("");

            } else if (found) {
                resultParts.add(part);

            } else if (startsWithUpperCase(part)) {
                found = true;
                resultParts.add(part);
            }
        }

        return String.join(".", resultParts);
    }

    /**
     * Splits the provided string into a list of substrings based on whitespace.
     * Consecutive whitespaces are treated as a single delimiter. Each resulting substring is trimmed.
     * If the input string is null, it is treated as an empty string.
     *  <p>
     *  Null or empty sting result in an empty list.
     */
    public static List<String> splitWhiteSpaces(String string) {
        String sanitized = JkUtilsString.nullToEmpty(string).trim();
        if (sanitized.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(JkUtilsString.nullToEmpty(string).trim().split(" "))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of options into a formatted string representation.
     * The formatting depends on the content of the options, where long options
     * containing path separators are split into separate lines, options starting
     * with a dash are formatted with additional indentation, and others are appended as is.
     *
     * @param options the list of options to be formatted as a string
     * @return a string representation of the options with appropriate formatting
     */
    public static String formatOptions(List<String> options) {
        StringBuilder sb = new StringBuilder();
        boolean followHyphen = false;
        for (String option : options) {
            if (option.contains(File.pathSeparator) && option.length() > 100) {
                Arrays.stream(option.split(File.pathSeparator))
                        .forEach(item -> sb.append("\n    ").append(item));
            } else if (option.startsWith("-")) {
                sb.append("\n  ").append(option);
                followHyphen = true;
            } else if (followHyphen){
                sb.append(" ").append(option);
                followHyphen = false;
            } else {
                sb.append("\n  ").append(option);
                followHyphen = false;
            }
        }
        return sb.toString();
    }

    /**
     * Tests if a string matches a given pattern using '*' as a wildcard joker.
     * The '*' character matches any sequence of characters (including empty sequence).
     *
     * @param text the string to test
     * @param pattern the pattern with '*' wildcards
     * @return true if the text matches the pattern, false otherwise
     */
    public static boolean matchesPattern(String text, String pattern) {
        if (text == null || pattern == null) {
            return text == pattern;
        }

        return matchesPatternRecursive(text, 0, pattern, 0);
    }

    private static boolean matchesPatternRecursive(String text, int textIndex,
                                                   String pattern, int patternIndex) {
        // If we've reached the end of the pattern
        if (patternIndex == pattern.length()) {
            return textIndex == text.length();
        }

        // If we've reached the end of the text but not the pattern
        if (textIndex == text.length()) {
            // Check if remaining pattern consists only of '*'
            for (int i = patternIndex; i < pattern.length(); i++) {
                if (pattern.charAt(i) != '*') {
                    return false;
                }
            }
            return true;
        }

        char patternChar = pattern.charAt(patternIndex);

        if (patternChar == '*') {
            // Try matching '*' with empty string, or with one or more characters
            // First try empty match (skip the '*')
            if (matchesPatternRecursive(text, textIndex, pattern, patternIndex + 1)) {
                return true;
            }
            // Then try matching one or more characters
            return matchesPatternRecursive(text, textIndex + 1, pattern, patternIndex);
        } else {
            // Regular character matching
            if (text.charAt(textIndex) == patternChar) {
                return matchesPatternRecursive(text, textIndex + 1, pattern, patternIndex + 1);
            } else {
                return false;
            }
        }
    }

}
