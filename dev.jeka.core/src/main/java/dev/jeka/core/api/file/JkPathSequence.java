package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A sequence of file path (folder or archive). Each file is called an <code>entry</code>.<br/>
 * Instances of this class are immutable.
 *
 * @author Jerome Angibaud
 */
public final class JkPathSequence implements Iterable<Path>, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Path> entries;

    // ----- Constructors & factory methods --------------------------------

    private JkPathSequence(Collection<Path> entries) {
        super();
        this.entries = Collections.unmodifiableList(JkUtilsIterable.listOf(entries));
    }

    /**
     * Creates a <code>JkPathSequence</code> from an <code>Iterable</code> of paths or a single Path.
     * @param paths As {@link Path} class implements { @link Iterable<Path> } the argument can be a single {@link Path}
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
    public static JkPathSequence of(Path path1, Path path2, Path... others) {
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

    public JkPathSequence normalized() {
        return JkPathSequence.of(entries.stream().map(path -> path.normalize()).collect(Collectors.toList()));
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
     * @see #andPrepend(Iterable)
     */
    public JkPathSequence andPrepend(Path path1, Path path2, Path... entries) {
        return andPrepend(JkUtilsIterable.listOf2orMore(path1, path2, entries));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of the specified entries followed by the sequence entries of this object.
     *
     * @param paths As {@link Path} class implements { @link Iterable<Path> } the argument can be a single {@link Path}
     * instance, if so it will be interpreted as a list containing a single element which is this argument.
     */
    public JkPathSequence andPrepend(Iterable<Path> paths) {
        final List<Path> result = JkUtilsPath.disambiguate(paths);
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
    public JkPathSequence and(Iterable<Path> otherEntries) {
        final List<Path> result = JkUtilsPath.disambiguate(otherEntries);
        result.addAll(0, entries);
        return new JkPathSequence(result);
    }

    // --------------- Misc ------------------------

    /**
     * Returns an identical path sequence but replacing relative paths with absolute paths resolved from the
     * specified base directory.
     */
    public JkPathSequence resolvedTo(Path baseDir) {
        return JkPathSequence.of(entries.stream()
                .map(path -> baseDir.resolve(path))
                .collect(Collectors.toList()));
    }

    /**
     * Returns an identical path sequence but replacing relative paths with absolute paths resolved from the
     * specified base directory.
     */
    public JkPathSequence relativizeFromWorkingDir() {
        return JkPathSequence.of(entries.stream()
                .map(JkUtilsPath::relativizeFromWorkingDir)
                .collect(Collectors.toList()));
    }

    /**
     * Returns the file names concatenated with ';' on Windows and ':' on unix.
     */
    public String toPath() {
        return String.join(File.pathSeparator, entries.stream().map(Path::toString).collect(Collectors.toList()));
    }

    public Set<Path> toSet() {
        return new LinkedHashSet<>(this.entries);
    }

    @Override
    public String toString() {
        return toPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JkPathSequence paths = (JkPathSequence) o;
        return entries.equals(paths.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    private  void writeObject(ObjectOutputStream oos) throws IOException {
        List<File> files = JkUtilsPath.toFiles(this.entries);
        oos.writeObject(files);
    }

    private  void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        List<File> files = (List<File>) ois.readObject();
        List<Path> paths = JkUtilsPath.toPaths(files);
        Field field = JkUtilsReflect.getField(JkPathSequence.class, "entries");
        JkUtilsReflect.setFieldValue(this, field, paths);
    }

}
