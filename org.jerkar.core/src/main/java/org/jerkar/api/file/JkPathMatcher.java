package org.jerkar.api.file;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A collection ofMany PathMatcher commonly used.
 */
public final class JkPathMatcher implements PathMatcher {

    // --------------------- Factory methods ------------------------------------------------

    public static JkPathMatcher noDirectory(LinkOption...linkOptions) {
        return new JkPathMatcher(path -> !Files.isDirectory(path, linkOptions));
    }

    public static JkPathMatcher in(String ... globPattern) {
        return in(Arrays.asList(globPattern));
    }

    public static JkPathMatcher in(Iterable<String> globPatterns) {
        PathMatcher result = empty();
        for (final String pattern : globPatterns) {
            result = new OrMatcher(result, globMatcher(pattern));
        }
        return new JkPathMatcher(result);
    }

    public static JkPathMatcher in(PathMatcher matcher) {
        return new JkPathMatcher(matcher);
    }

    public static JkPathMatcher notIn(String ... globPatterns) {
        return notIn(Arrays.asList(globPatterns));
    }

    public static JkPathMatcher notIn(Iterable<String> globPatterns) {
        PathMatcher result = path -> true;
        for (final String pattern : globPatterns) {
            result = new AndMatcher(result, path -> !globMatcher(pattern).matches(path));
        }
        return new JkPathMatcher(result);
    }


    // ---------------------------- fields and constructors



    private final PathMatcher matcher;

    private JkPathMatcher(PathMatcher matcher) {
        this.matcher = matcher;
    }

    public Predicate<Path> asPredicate() {
        return path -> matcher.matches(path);
    }

    // ------------------------- check methods ---------------------------

    @Override
    public boolean matches(Path path) {
        return matcher.matches(path);
    }

    public List<String> getIncludePatterns() {
        final List<String> result = new LinkedList<>();
        return result; // TODO
    }

    public List<String> getExcludePatterns() {
        final List<String> result = new LinkedList<>();
        return result; // TODO
    }

    // ---------------------------- adders ---------------------------------------

    public JkPathMatcher and(PathMatcher other) {
        return new JkPathMatcher(new AndMatcher(this.matcher, other));
    }

    public JkPathMatcher or(PathMatcher other) {
        return new JkPathMatcher(new AndMatcher(this.matcher, other));
    }

    public JkPathMatcher andNot(String pattern) {
        return this.and(JkPathMatcher.notIn(pattern));
    }

    private static class Reverse implements PathMatcher  {

        private final PathMatcher matcher;

        Reverse(PathMatcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean matches(Path path) {
            return !matcher.matches(path);
        }

    }

    // --------------------------------------------- matcher

    private static PathMatcher empty() {
        return path -> false;
    }

    private static PathMatcher globMatcher(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + pattern);
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

}
