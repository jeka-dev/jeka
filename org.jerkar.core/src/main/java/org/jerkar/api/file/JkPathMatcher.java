package org.jerkar.api.file;

import java.nio.file.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A collection of PathMatcher commonly used each assosciated with a label for human recognising.
 */
public final class JkPathMatcher implements PathMatcher {

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
    public static JkPathMatcher noDirectory(LinkOption...linkOptions) {
        return new JkPathMatcher(path -> !Files.isDirectory(path, linkOptions), "No directories");
    }

    /**
     * A matcher accepting specified glob pattern within specified file system.
     */
    public static JkPathMatcher accept(FileSystem fileSystem, String ... globPattern) {
        return accept(fileSystem, Arrays.asList(globPattern));
    }

    /**
     * A matcher accepting specified glob patterns within default file system.
     */
    public static JkPathMatcher accept(String ... globPattern) {
        return accept(FileSystems.getDefault(), Arrays.asList(globPattern));
    }

    public static JkPathMatcher accept(FileSystem fileSystem, Iterable<String> globPatterns) {
        PathMatcher result = empty();
        for (final String pattern : globPatterns) {
            result = new OrMatcher(result, globMatcher(fileSystem, pattern));
        }
        return new JkPathMatcher(result, "accept:" + globPatterns);
    }

    public static JkPathMatcher refuse(FileSystem fileSystem, String ... globPatterns) {
        return refuse(fileSystem, Arrays.asList(globPatterns));
    }

    public static JkPathMatcher refuse(String ... globPatterns) {
        return refuse(FileSystems.getDefault(), Arrays.asList(globPatterns));
    }

    public static JkPathMatcher refuse(FileSystem fileSystem, Iterable<String> globPatterns) {
        PathMatcher result = path -> true;
        for (final String pattern : globPatterns) {
            result = new AndMatcher(result, path -> !globMatcher(fileSystem, pattern).matches(path));
        }
        return new JkPathMatcher(result, "refuse:" + globPatterns);
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
        return label;
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
        return new JkPathMatcher(new AndMatcher(this.matcher, other),
                this.label + " & " + other.toString());
    }

    public JkPathMatcher or(PathMatcher other) {
        return new JkPathMatcher(new OrMatcher(this.matcher, other),
                this.label + " | " + other.toString());
    }

    public JkPathMatcher andAccept(FileSystem fileSystem, String pattern) {
        return this.and(JkPathMatcher.accept(fileSystem, pattern));
    }

    public JkPathMatcher andRefuse(FileSystem fileSystem, String ... patterns) {
        return this.and(JkPathMatcher.refuse(fileSystem, patterns));
    }

    public JkPathMatcher andRefuse(String ... patterns) {
        return andRefuse(FileSystems.getDefault(), patterns);
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

}
