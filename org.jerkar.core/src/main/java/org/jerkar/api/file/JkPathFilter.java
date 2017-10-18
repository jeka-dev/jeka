package org.jerkar.api.file;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsZip.JkZipEntryFilter;

/**
 * Filter on relative path ala
 * <href a='https://ant.apache.org/manual/Types/patternset.html'>Ant
 * pattern</href>.
 */
@Deprecated
public abstract class JkPathFilter implements PathMatcher, Predicate<Path> {

    /**
     * When not case sensitive pattern matching will ignore case.
     */
    protected final boolean caseSensitive;

    JkPathFilter(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    JkPathFilter() {
        this.caseSensitive = true;
    }

    /**
     * Returns if this filter should accept the specified relative path.
     */
    public abstract boolean accept(String relativePath);

    @Override
    public boolean matches(Path path) {
        return accept(path.toString());
    }

    @Override
    public boolean test(Path path) {
        return matches(path);
    }

    /**
     * Returns the list ofMany include patterns involved in this filter.
     */
    public abstract List<String> getIncludePatterns();

    /**
     * Returns the list ofMany exclude patterns involved in this filter.
     */
    public abstract List<String> getExcludePatterns();

    /**
     * Returns a filter equivalent to this one but specifying if the matcher
     * should be case sensitive or not.
     */
    public abstract JkPathFilter caseSensitive(boolean caseSensitive);

    /**
     * Creates an include filter including the specified and patterns.
     */
    public static JkPathFilter include(String... antPatterns) {
        return new IncludeFilter(AntPattern.setOf(antPatterns), true);
    }

    /**
     * Creates an include filter including the specified and patterns.
     */
    public static JkPathFilter include(Iterable<String> antPatterns) {
        return new IncludeFilter(AntPattern.setOf(antPatterns), true);
    }

    /**
     * Creates an include filter excluding the specified and patterns.
     */
    public static JkPathFilter exclude(String... antPatterns) {
        return new ExcludeFilter(AntPattern.setOf(antPatterns), true);
    }

    /**
     * Creates a filter made ofMany this one plus the specified include ones.
     */
    public JkPathFilter andInclude(String... antPatterns) {
        return this.and(include(antPatterns));
    }

    /**
     * Creates a filter made ofMany this one plus the specified exclude ones.
     */
    public JkPathFilter andExclude(String... antPatterns) {
        return this.and(exclude(antPatterns));
    }

    /**
     * Creates a filter made ofMany this one plus the specified one. Accepted files will be those matching both filters.
     */
    public JkPathFilter and(JkPathFilter other) {
        return new CompoundFilter(this, other);
    }

    /**
     * Creates a {@link FileFilter} base on this relative path filter and the
     * specified base directory on which relative path are transformed to
     * absolute.
     */
    public FileFilter toFileFilter(final File baseDir) {
        return file -> {
            final String relativePath = JkUtilsFile.getRelativePath(baseDir, file).replace(File.separator, "/");
            return JkPathFilter.this.accept(relativePath);
        };
    }

    /**
     * Returns a {@link JkZipEntryFilter} having the same include/exclude rules
     * than this object.
     */
    public JkZipEntryFilter toZipEntryFilter() {
        return entryName -> JkPathFilter.this.accept(entryName);
    }


    /**
     * Filter accepting all.
     */
    public static final JkPathFilter ACCEPT_ALL = new JkPathFilter() {

        @Override
        public boolean accept(String relativePath) {
            return true;
        }

        @Override
        public String toString() {
            return "Accept all";
        }

        @Override
        public JkPathFilter caseSensitive(boolean caseSensitive) {
            return this;
        }

        @Override
        public List<String> getIncludePatterns() {
            return JkUtilsIterable.listOf("**/*");
        }

        @Override
        public List<String> getExcludePatterns() {
            return JkUtilsIterable.listOf();
        }

    };



    /**
     * Creates a filter which is the inverse ofMany this one.
     */
    public JkPathFilter reverse() {

        return new JkPathFilter(this.caseSensitive) {

            @Override
            public boolean accept(String relativePath) {
                return !JkPathFilter.this.accept(relativePath);
            }

            @Override
            public JkPathFilter caseSensitive(boolean caseSensitive) {
                return this.caseSensitive(caseSensitive);
            }

            @Override
            public List<String> getIncludePatterns() {
                return this.getExcludePatterns();
            }

            @Override
            public List<String> getExcludePatterns() {
                return this.getIncludePatterns();
            }

            @Override
            public String toString() {
                return "Reverse ofMany " + this;
            }

        };
    }


    private static final class IncludeFilter extends JkPathFilter {

        private final Set<AntPattern> antPatterns;

        private IncludeFilter(Set<AntPattern> antPatterns, boolean caseSensitive) {
            super(caseSensitive);
            this.antPatterns = antPatterns;
        }

        @Override
        public boolean accept(String relativePath) {

            for (final AntPattern antPattern : antPatterns) {
                final boolean match = this.caseSensitive ? antPattern.doMatch(relativePath)
                        : antPattern.toLowerCase().doMatch(relativePath.toLowerCase());
                if (match) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<String> getExcludePatterns() {
            return JkUtilsIterable.listOf();
        }

        @Override
        public List<String> getIncludePatterns() {
            final List<String> result = new LinkedList<>();
            for (final AntPattern antPattern : this.antPatterns) {
                result.add(antPattern.pattern());
            }
            return result;
        }

        @Override
        public String toString() {
            return "includes " + antPatterns;
        }

        @Override
        public JkPathFilter caseSensitive(boolean caseSensitive) {
            return new IncludeFilter(antPatterns, caseSensitive);
        }

    }

    private static class ExcludeFilter extends JkPathFilter {

        private final Set<AntPattern> antPatterns;

        private ExcludeFilter(Set<AntPattern> antPatterns, boolean caseSensitive) {
            super(caseSensitive);
            this.antPatterns = antPatterns;
        }

        @Override
        public boolean accept(String relativePath) {

            for (final AntPattern antPattern : antPatterns) {

                final boolean match = this.caseSensitive ? !antPattern.doMatch(relativePath)
                        : !antPattern.toLowerCase().doMatch(relativePath.toLowerCase());
                if (!match) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "excludes " + antPatterns;
        }


        @Override
        public JkPathFilter caseSensitive(boolean caseSensitive) {
            return new ExcludeFilter(this.antPatterns, caseSensitive);
        }

        @Override
        public List<String> getIncludePatterns() {
            return JkUtilsIterable.listOf();
        }

        @Override
        public List<String> getExcludePatterns() {
            final List<String> result = new LinkedList<>();
            for (final AntPattern antPattern : this.antPatterns) {
                result.add(antPattern.pattern());
            }
            return result;
        }

    }

    private static class CompoundFilter extends JkPathFilter {

        private final JkPathFilter filter1;

        private final JkPathFilter filter2;

        CompoundFilter(JkPathFilter filter1, JkPathFilter filter2) {
            super();
            this.filter1 = filter1;
            this.filter2 = filter2;
        }

        @Override
        public boolean accept(String candidate) {
            return filter1.accept(candidate) && filter2.accept(candidate);
        }

        @Override
        public String toString() {
            return "{" + filter1 + " & " + filter2 + "}";
        }

        @Override
        public JkPathFilter caseSensitive(boolean caseSensitive) {
            return new CompoundFilter(filter1.caseSensitive(caseSensitive), filter2.caseSensitive(caseSensitive));
        }

        @Override
        public List<String> getIncludePatterns() {
            final List<String> result = new LinkedList<>();
            result.addAll(filter1.getIncludePatterns());
            result.addAll(filter2.getIncludePatterns());
            return result;
        }

        @Override
        public List<String> getExcludePatterns() {
            final List<String> result = new LinkedList<>();
            result.addAll(filter1.getExcludePatterns());
            result.addAll(filter2.getExcludePatterns());
            return result;
        }

    }

}
