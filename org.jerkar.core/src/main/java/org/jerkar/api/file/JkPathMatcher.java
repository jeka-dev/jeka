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
 * A collection of PathMatcher commonly used.
 */
public final class JkPathMatcher implements PathMatcher, Predicate<Path> {

    public static JkPathMatcher noDirectory(LinkOption...linkOptions) {
        return new JkPathMatcher(null, path -> !Files.isDirectory(path, linkOptions));
    }

    private static JkPathMatcher include(String ... globPattern) {
        return include(Arrays.asList(globPattern));
    }

    public static JkPathMatcher include(Iterable<String> globPatterns) {
        JkPathMatcher result = null;
        for (final String pattern : globPatterns) {
            result = new JkPathMatcher(result, globMatcher(pattern));
        }
        return result != null ? result : JkPathMatcher.of(path -> true);
    }


    public static JkPathMatcher exclude(String globPattern) {
        final PathMatcher pathMatcher = globMatcher(globPattern);
        return new JkPathMatcher(null, new Reverse(pathMatcher));
    }

    public static JkPathMatcher of(Predicate<Path> predicate) {
        return new JkPathMatcher(null, path -> predicate.test(path));
    }

    private final PathMatcher matcher;

    private final PathMatcher linked;

    private JkPathMatcher(PathMatcher linked, PathMatcher matcher) {
        this.linked = linked;
        this.matcher = matcher;
    }

    @Override
    public final boolean test(Path path) {
        return matches(path);
    }

    @Override
    public boolean matches(Path path) {
        return linked != null ? linked.matches(path) && matcher.matches(path) : matcher.matches(path);
    }

    public List<String> getIncludePatterns() {
        final List<String> result = new LinkedList<>();
        return result; // TODO
    }

    public List<String> getExcludePatterns() {
        final List<String> result = new LinkedList<>();
        return result; // TODO
    }

    public JkPathMatcher and(PathMatcher matcher) {
        return new JkPathMatcher(this, matcher);
    }

    private static class Reverse implements PathMatcher  {

        private final PathMatcher matcher;

        Reverse(PathMatcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean matches(Path path) {
            return !matches(path);
        }

    }

    private static PathMatcher globMatcher(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob: " + pattern);
    }




}
