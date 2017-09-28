package org.jerkar.api.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A sequence of file (folder or archive) to be used as a <code>path</code>. <br/>
 * Each file is called an <code>entry</code>.<br/>
 * Instances of this class are immutable.
 * 
 * @author Djeang
 */
public final class JkPathSequence implements Iterable<File> {

    private final List<Path> entries;

    private JkPathSequence(Iterable<Path> entries) {
        super();
        this.entries = Collections.unmodifiableList(JkUtilsIterable.listOf(entries));
    }

    /**
     * Creates a path to a sequence of files.
     */
    @Deprecated
    public static JkPathSequence of(Iterable<File> entries) {
        return new JkPathSequence(JkUtilsPath.pathsOf(entries));
    }

    /**
     * Creates a path to a sequence of files.
     */
    public static JkPathSequence ofPath(Iterable<Path> entries) {
        final LinkedHashSet<Path> files = new LinkedHashSet<>(JkUtilsIterable.listOf(entries));
        return new JkPathSequence(files);
    }

    /**
     * Creates a <code>JkPathSequence</code> to a base directory and string of
     * relative paths separated with a ";".
     */
    public static JkPathSequence of(File baseDir, String relativePathAsString) {
        return of(baseDir.toPath(), relativePathAsString);
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
        return ofPath(result);
    }

    /**
     * Creates a path to aa array of files.
     */
    public static JkPathSequence of(File... entries) {
        return JkPathSequence.of(Arrays.asList(entries));
    }

    /**
     * Creates a path to aa array of files.
     */
    public static JkPathSequence ofPath(Path... entries) {
        return JkPathSequence.ofPath(Arrays.asList(entries));
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
    @Deprecated
    public List<File> entries() {
        return JkUtilsPath.filesOf(entries);
    }

    /**
     * Returns the sequence of files as a list.
     */
    public List<Path> pathEntries() {
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
     * @see #andHead(Iterable)
     */
    public JkPathSequence andHead(File... entries) {
        return andHead(JkPathSequence.of(entries));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of, in the order, the specified
     * entries plus the entries of this one.
     */
    @SuppressWarnings("unchecked")
    public JkPathSequence andHead(Iterable<File> otherEntries) {
        return new JkPathSequence(JkUtilsIterable.chain(JkUtilsPath.pathsOf(otherEntries), this.entries));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of, in the order, the specified
     * entries plus the entries of this one.
     */
    @SuppressWarnings("unchecked")
    public JkPathSequence andHeadPath(Iterable<Path> otherEntries) {
        return new JkPathSequence(JkUtilsIterable.chain(otherEntries, this.entries));
    }

    /**
     * @see #and(Iterable)
     */
    public JkPathSequence and(File... files) {
        return and(JkPathSequence.of(files));
    }

    public JkPathSequence and(Path... files) {
        return and(JkPathSequence.ofPath(files));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of, in the order, the entries of this
     * one plus the specified ones.
     */
    @SuppressWarnings("unchecked")
    public JkPathSequence and(Iterable<File> otherFiles) {
        return new JkPathSequence(JkUtilsIterable.chain(this.entries, JkUtilsPath.pathsOf(otherFiles)));
    }

    /**
     * Returns a <code>JkPathSequence</code> made of, in the order, the entries of this
     * one plus the specified ones.
     */
    @SuppressWarnings("unchecked")
    public JkPathSequence andPath(Iterable<Path> otherFiles) {
        return new JkPathSequence(JkUtilsIterable.chain(this.entries, otherFiles));
    }

    /**
     * Returns the file names concatenated with ';'.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final Iterator<File> it = this.iterator(); it.hasNext();) {
            builder.append(it.next().getAbsolutePath());
            if (it.hasNext()) {
                builder.append(";");
            }
        }
        return builder.toString();
    }

    @Override
    public Iterator<File> iterator() {
        return JkUtilsPath.filesOf(entries).iterator();
    }

}
