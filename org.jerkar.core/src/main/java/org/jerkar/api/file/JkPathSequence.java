package org.jerkar.api.file;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;

/**
 * A sequence of file path (folder or archive). Each file is called an <code>entry</code>.<br/>
 * Instances of this class are immutable.
 * 
 * @author Jerome Angibaud
 */
public final class JkPathSequence implements Iterable<Path> {

    private final List<Path> entries;

    // ----- Constructors & factory methods --------------------------------

    private JkPathSequence(Collection<Path> entries) {
        super();
        this.entries = Collections.unmodifiableList(JkUtilsIterable.listOf(entries));
    }

    /**
     * Creates a <code>JkPathSequence</code> from an <code>Iterable</code> of paths or a single Path.
     * @param paths As {@link Path} class implements {@link Iterable<Path>} the argument can be a single {@link Path}
     * instance, if so it will be interpreted as a list containing a single element which is this argument.
     */
    public static JkPathSequence of(Iterable<Path> paths) {
        return new JkPathSequence(JkUtilsPath.disambiguate(paths));
    }

    public static JkPathSequence of() {
        return new JkPathSequence(Collections.emptyList());
    }

    /**
     * Creates a <code>JkPathSequence</code> from a base directory and string of
     * relative paths separated with a ";".
     */
    public static JkPathSequence of(Path baseDir, String relativePathsAsString) {
        final String[] paths = relativePathsAsString.split(File.pathSeparator);
        final List<Path> result = new LinkedList<>();
        for (final String path : paths) {
            Path file = Paths.get(path);
            if (!file.isAbsolute()) {
                file = baseDir.resolve(path);
            }
            result.add(file);
        }
        return of(result);
    }

    /**
     * Creates a <code>JkPathSequence</code> form specified entries
     */
    public static JkPathSequence of(Path path1, Path path2, Path... others) { ;
        return JkPathSequence.of(JkUtilsIterable.listOf2orMore(path1, path2, others));
    }


    // --------------------------- cleaning ----------------------------------------

    /**
     * Returns a <code>JkPathSequence</code> identical to this one but without duplicates.
     * If a given file in this sequence exist twice or more, then only the first occurrence is kept in the returned
     * sequence.
     */
    public JkPathSequence withoutDuplicates() {
        final List<Path> files = new LinkedList<>();
        for (final Path file : this.entries) {
            if (!files.contains(file)) {
                files.add(file);
            }
        }
        return new JkPathSequence(files);
    }

    // -------------------------- iterate -----------------------------------------

    /**
     * Returns this sequence as a list.
     */
    public List<Path> getEntries() {
        return entries;
    }

    @Override
    public Iterator<Path> iterator() {
        return entries.iterator();
    }

    // ------------------------------ withers and adders ------------------------------------

    /**
     * @see #andPrepending(Iterable)
     */
    public JkPathSequence andPrepending(Path path1, Path path2, Path... entries) {
        return andPrepending(JkUtilsIterable.listOf2orMore(path1, path2, entries));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of the specified entries followed by the sequence entries of this object.
     *
     * @param paths As {@link Path} class implements {@link Iterable<Path>} the argument can be a single {@link Path}
     * instance, if so it will be interpreted as a list containing a single element which is this argument.
     */
    @SuppressWarnings("unchecked")
    public JkPathSequence andPrepending(Iterable<Path> paths) {
        List<Path> result = JkUtilsPath.disambiguate(paths);
        result.addAll(entries);
        return new JkPathSequence(result);
    }

    /**
     * @see #and(Iterable)
     */
    public JkPathSequence and(Path path1, Path path2, Path... others) {
        return and(JkUtilsIterable.listOf2orMore(path1, path2, others));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of, in the order, the entries of this
     * one plus the specified ones.
     */
    @SuppressWarnings("unchecked")
    public JkPathSequence and(Iterable<Path> otherEntries) {
        List<Path> result = JkUtilsPath.disambiguate(otherEntries);
        result.addAll(0, entries);
        return new JkPathSequence(result);
    }

    // --------------- Misc ------------------------

    /**
     * Returns an identical path sequence but replacing relative paths with absolute paths resolved from the
     * specified base directory.
     */
    public JkPathSequence resolveTo(Path baseDir) {
        List<Path> result = new LinkedList<>();
        for(Path entry : entries) {
            if (entry.isAbsolute()) {
                result.add(entry);
            } else {
                result.add(baseDir.toAbsolutePath().resolve(entry));
            }
        }
        return new JkPathSequence(entries);
    }

    // --------------- Canonical methods ------------------------------------------

    /**
     * Returns the file names concatenated with ';'.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final Iterator<Path> it = this.entries.iterator(); it.hasNext();) {
            builder.append(it.next().toAbsolutePath().toString());
            if (it.hasNext()) {
                builder.append(";");
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkPathSequence paths = (JkPathSequence) o;

        return entries.equals(paths.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }
}
