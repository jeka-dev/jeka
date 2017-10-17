package org.jerkar.api.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A sequence of file (folder or archive) to be used as a <code>path</code>. <br/>
 * Each file is called an <code>entry</code>.<br/>
 * Instances of this class are immutable.
 * 
 * @author Djeang
 */
public final class JkPathSequence {

    private final List<Path> entries;

    private JkPathSequence(Collection<Path> entries) {
        super();
        this.entries = Collections.unmodifiableList(JkUtilsIterable.listOf(entries));
    }

    /**
     * Creates a path to a sequence of files.
     */
    public static JkPathSequence of(Collection<Path> entries) {
        final LinkedHashSet<Path> files = new LinkedHashSet<>(JkUtilsIterable.listOf(entries));
        return new JkPathSequence(files);
    }

    /**
     * Creates a <code>JkPathSequence</code> to a base directory and string of
     * relative paths separated with a ";".
     */
    public static JkPathSequence of(Path baseDir, String relativePathAsString) {
        final String[] paths = JkUtilsString.split(relativePathAsString, File.pathSeparator);
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
     * Creates a path to aa array of files.
     */
    public static JkPathSequence of(Path... entries) {
        return JkPathSequence.of(Arrays.asList(entries));
    }

    /**
     * Throws an {@link IllegalStateException} if at least one entry does not
     * exist.
     */
    public JkPathSequence assertAllEntriesExist() throws IllegalStateException {
        for (final Path file : entries) {
            if (!Files.exists(file)) {
                throw new IllegalStateException("File " + file + " does not exist.");
            }
        }
        return this;
    }

    /**
     * Returns a <code>JkPathSequence</code> identical to this one but without redundant
     * files. So if a given file in the sequence exist twice or more, then only
     * the first occurrence is kept.
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

    /**
     * Returns the sequence of files as a list.
     */
    public List<Path> entries() {
        return entries;
    }

    /**
     * Returns the first entry of this path.
     */
    public File first() {
        return entries.get(0).toFile();
    }

    /**
     * Short hand for <code>entries().isEmpty()</code>.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @see #andHead(Collection)
     */
    public JkPathSequence andHead(Path... entries) {
        return andHead(Arrays.asList(entries));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of, in the order, the specified
     * entries plus the entries of this one.
     */
    @SuppressWarnings("unchecked")
    public JkPathSequence andHead(Collection<Path> otherEntries) {
        List<Path> list = new LinkedList<>(otherEntries);
        list.addAll(entries);
        return new JkPathSequence(list);
    }

    public JkPathSequence and(Path... files) {
        return andPath(Arrays.asList(files));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of, in the order, the entries of this
     * one plus the specified ones.
     */
    @SuppressWarnings("unchecked")
    public JkPathSequence andPath(Collection<Path> otherEntries) {
        List<Path> list = new LinkedList<>(entries);
        list.addAll(otherEntries);
        return new JkPathSequence(list);
    }

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


}
