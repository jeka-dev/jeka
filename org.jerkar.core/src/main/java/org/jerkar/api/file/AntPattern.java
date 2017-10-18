/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jerkar.api.file;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsString;

/**
 * Stands for an Ant file pattern. These patterns are used to include or exclude
 * files within a folder. In a nutshell :
 * <ul>
 * <li>'**' means any directory</li>
 * <li>'*' means any sequence or 0 or more characters</li>
 * <li>'?' means any single character</li>
 * </ul>
 * For example <code>&#42;&#42;/&#42;.java</code> matches
 * <code>foo/subfoo/bar.java</code> <br/>
 * See <a href="http://ant.apache.org/manual/dirtasks.html#patterns">Ant Pattern
 * documentation</a>
 *
 * <i>
 * <p>
 * Part ofMany this mapping code has been kindly borrowed from <a
 * href="http://ant.apache.org">Apache Ant</a> and <a
 * href="https://github.com/spring-projects/spring-framework">Spring
 * Framework</a></i>
 */
final class AntPattern {

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AntPattern other = (AntPattern) obj;
        if (pattern == null) {
            if (other.pattern != null) {
                return false;
            }
        } else if (!pattern.equals(other.pattern)) {
            return false;
        }
        return true;
    }

    private static final char PATH_SEPARATOR_CHAR = '/';

    private static final String PATH_SEPARATOR = "" + PATH_SEPARATOR_CHAR;

    private final String pattern;

    private AntPattern(String pattern) {
        this.pattern = normalize(pattern);
    }

    static AntPattern of(String pattern) {
        return new AntPattern(pattern);
    }

    static Set<AntPattern> setOf(Iterable<String> patterns) {
        final Set<AntPattern> result = new HashSet<>();
        for (final String pattern : patterns) {
            result.add(AntPattern.of(pattern));
        }
        return result;
    }

    static Set<AntPattern> setOf(String... patterns) {
        return setOf(Arrays.asList(patterns));
    }

    /**
     * Matches the given <code>path</code> against the given
     * <code>pattern</code>.
     */
    boolean doMatch(String path) {

        // First deleteArtifacts path and pattern to remove leading '/', '.' or '\'
        // characters
        final String normalizedPath = normalize(path);

        final String[] pattDirs = JkUtilsString.split(pattern, PATH_SEPARATOR);
        final String[] pathDirs = JkUtilsString.split(normalizedPath, PATH_SEPARATOR);

        int pattIdxStart = 0;
        int pattIdxEnd = pattDirs.length - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathDirs.length - 1;

        // Match all elements up to the first **
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            final String patDir = pattDirs[pattIdxStart];
            if ("**".equals(patDir)) {
                break;
            }
            if (!matchStrings(patDir, pathDirs[pathIdxStart])) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }

        if (pathIdxStart > pathIdxEnd) {
            // Path is exhausted, only match if rest ofMany pattern is * or **'s
            if (pattIdxStart > pattIdxEnd) {
                return (pattern.endsWith(PATH_SEPARATOR) == normalizedPath.endsWith(PATH_SEPARATOR));
            }
            if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*")
                    && normalizedPath.endsWith(PATH_SEPARATOR)) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        } else if (pattIdxStart > pattIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        }

        // up to last '**'
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            final String patDir = pattDirs[pattIdxEnd];
            if (patDir.equals("**")) {
                break;
            }
            if (!matchStrings(patDir, pathDirs[pathIdxEnd])) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            // String is exhausted
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (pattDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                // '**/**' situation, so skip one
                pattIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            final int patLength = (patIdxTmp - pattIdxStart - 1);
            final int strLength = (pathIdxEnd - pathIdxStart + 1);
            int foundIdx = -1;

            strLoop: for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    final String subPat = pattDirs[pattIdxStart + j + 1];
                    final String subStr = pathDirs[pathIdxStart + i + j];
                    if (!matchStrings(subPat, subStr)) {
                        continue strLoop;
                    }
                }
                foundIdx = pathIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }

        for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
            if (!pattDirs[i].equals("**")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the same ANT pattern but in lower case.
     */
    public AntPattern toLowerCase() {
        return new AntPattern(this.pattern.toLowerCase());
    }

    /**
     * Tests whether or not a string matches against a pattern. The pattern may
     * contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern
     *            pattern to match against. Must not be <code>null</code>.
     * @param str
     *            string which must be matched against the pattern. Must not be
     *            <code>null</code>.
     * @return <code>true</code> if the string matches against the pattern, or
     *         <code>false</code> otherwise.
     */
    private static boolean matchStrings(String pattern, String str) {
        final char[] patArr = pattern.toCharArray();
        final char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for (final char element : patArr) {
            if (element == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?') {
                    if (ch != strArr[i]) {
                        return false;// Character mismatch
                    }
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (ch != strArr[strIdxStart]) {
                    return false;// Character mismatch
                }
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (ch != strArr[strIdxEnd]) {
                    return false;// Character mismatch
                }
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            final int patLength = (patIdxTmp - patIdxStart - 1);
            final int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop: for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?') {
                        if (ch != strArr[strIdxStart + i + j]) {
                            continue strLoop;
                        }
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return false;
            }
        }

        return true;
    }

    private static String normalize(String pathOrPattern) {
        if (pathOrPattern.startsWith(PATH_SEPARATOR)) {
            pathOrPattern = pathOrPattern.substring(1);
        }
        pathOrPattern = pathOrPattern.replace(File.separatorChar, PATH_SEPARATOR_CHAR);
        if (pathOrPattern.startsWith("." + PATH_SEPARATOR)
                || pathOrPattern.startsWith("." + File.separator)) {
            pathOrPattern = pathOrPattern.substring(2);
        }
        if (pathOrPattern.startsWith(PATH_SEPARATOR) || pathOrPattern.startsWith(File.separator)) {
            pathOrPattern = pathOrPattern.substring(1);
        }
        return pathOrPattern;
    }

    public String pattern() {
        return this.pattern;
    }

    @Override
    public String toString() {
        return pattern;
    }

}