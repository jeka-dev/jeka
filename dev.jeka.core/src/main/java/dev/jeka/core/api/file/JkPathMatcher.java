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

package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * A collection of PathMatcher commonly used each associated with a label for human recognising.
 */
public final class JkPathMatcher implements PathMatcher {

    private static final String ALL_LABEL_PREFIX = "all && ";

    static final JkPathMatcher ACCEPT_ALL = JkPathMatcher.of();

    // --------------------- Factory methods ------------------------------------------------

    /**
     * Creates an instance from a {@link PathMatcher} instance.
     */
    public static JkPathMatcher of(PathMatcher matcher) {
        return new JkPathMatcher(matcher, "?");
    }

    /**
     * A matcher accepting all entries.
     */
    public static JkPathMatcher of() {
        return new JkPathMatcher(path -> true, "all");
    }

    /**
     * A matcher filtering out directories.
     */
    public static JkPathMatcher ofNoDirectory(LinkOption...linkOptions) {
        return new JkPathMatcher(path -> {
            if (path.toString().equals("")) {  // In zip, Files.isDirectory(Paths.get("")) throws an Exception.
                return false;
            }
            return !Files.isDirectory(path, linkOptions);
            }, "No directories");
    }

    /**
     * @see #of(boolean, FileSystem, Iterable)
     */
    public static JkPathMatcher of(boolean positive, FileSystem fileSystem, String ... globPattern) {
        return of(positive, fileSystem, Arrays.asList(globPattern));
    }

    /**
     * Shorthand for <code>of(true, patterns)</code>.
     */
    public static JkPathMatcher of(String ... globPatterns) {
        return of(true, globPatterns);
    }

    /**
     * @see #of(boolean, FileSystem, Iterable)
     */
    public static JkPathMatcher of(boolean positive, String ... globPattern) {
        return of(positive, FileSystems.getDefault(), Arrays.asList(globPattern));
    }

    /**
     * A matcher accepting/refusing if path matches at least one of the specified glob patterns within specified file system.
     * @param positive If <code>true</code> matcher will accept files matching at least one of the specified patterns.
     *                 If <code>false</code> matcher will accept files matching none of the specified pattern.
     */
    public static JkPathMatcher of(boolean positive, FileSystem fileSystem, Iterable<String> globPatterns) {
        Iterator<String> it = globPatterns.iterator();
        if (!it.hasNext()) {
            return JkPathMatcher.of();
        }
        String pattern = it.next();
        PathMatcher result = path -> positive == globMatcher(fileSystem, pattern).matches(path);
        while (it.hasNext()) {
            String itPattern = it.next();
            if (positive) {
                result = new OrMatcher(result, globMatcher(fileSystem, itPattern));
            } else {
                result = new AndMatcher(result, path -> !globMatcher(fileSystem, itPattern).matches(path));
            }
        }
        String name = positive ? "in" : "out";
        return new JkPathMatcher(result, name + globPatterns);
    }

    // ---------------------------- fields and constructors

    private final PathMatcher matcher;

    private final String label;

    private JkPathMatcher(PathMatcher matcher, String label) {
        this.matcher = matcher;
        this.label = label;
    }

    @Override
    public String toString() {
        if (label.startsWith(ALL_LABEL_PREFIX)) {
            return label.substring(ALL_LABEL_PREFIX.length());
        }
        return label;
    }

    /**
     * Returns this matcher as a {@link Predicate}
     */
    public Predicate<Path> toPredicate() {
        return matcher::matches;
    }
    // ------------------------- check methods ---------------------------

    @Override
    public boolean matches(Path path) {
        return matcher.matches(path);
    }

    // ---------------------------- adders ---------------------------------------

    public JkPathMatcher and(PathMatcher other) {
        if (this == ACCEPT_ALL) {
            if (other == ACCEPT_ALL.matcher || other == ACCEPT_ALL) {
                return ACCEPT_ALL;
            }
            return new JkPathMatcher(other, other.toString());
        }
        if (other == ACCEPT_ALL.matcher || other ==ACCEPT_ALL) {
            return this;
        }
        return new JkPathMatcher(new AndMatcher(this.matcher, other),
                this.label + " && " + other.toString());
    }

    public JkPathMatcher or(PathMatcher other) {
        JkUtilsAssert.argument(other != null, "Combined path matcher cannot be null");
        if (this == ACCEPT_ALL || other == ACCEPT_ALL || other == ACCEPT_ALL.matcher) {
            return ACCEPT_ALL;
        }
        return new JkPathMatcher(new OrMatcher(this.matcher, other),
                this.label + " || " + other);
    }

    public JkPathMatcher and(boolean positive, FileSystem fileSystem, String ...patterns) {
        return this.and(JkPathMatcher.of(positive, fileSystem, patterns));
    }

    public JkPathMatcher and(boolean positive, String ...patterns) {
        return this.and(positive, FileSystems.getDefault(), patterns);
    }

    // --------------------------------------------- matcher

    private static PathMatcher globMatcher(FileSystem fileSystem, String pattern) {
        return fileSystem.getPathMatcher("glob:" + pattern);
    }

    private static class AndMatcher implements PathMatcher {

        private final PathMatcher pathMatcher1;
        private final PathMatcher pathMatcher2;

        public AndMatcher(PathMatcher pathMatcher1, PathMatcher pathMatcher2) {
            this.pathMatcher1 = pathMatcher1;
            this.pathMatcher2 = pathMatcher2;
        }

        @Override
        public boolean matches(Path path) {
            return pathMatcher1.matches(path) && pathMatcher2.matches(path);
        }
    }

    private static class OrMatcher implements PathMatcher {

        private final PathMatcher pathMatcher1;
        private final PathMatcher pathMatcher2;

        public OrMatcher(PathMatcher pathMatcher1, PathMatcher pathMatcher2) {
            this.pathMatcher1 = pathMatcher1;
            this.pathMatcher2 = pathMatcher2;
        }

        @Override
        public boolean matches(Path path) {
            return pathMatcher1.matches(path) || pathMatcher2.matches(path);
        }
    }

    // ------------------------------------- Other

    public JkPathMatcher negate() {
        PathMatcher reversedMatcher = path -> !this.matcher.matches(path);
        return new JkPathMatcher(reversedMatcher, "Reverse of " + this.label);
    }

}
