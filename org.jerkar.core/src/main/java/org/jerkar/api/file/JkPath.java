package org.jerkar.api.file;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A sequence of file (folder or archive) to be used as a <code>path</code>. <br/>
 * Each file is called an <code>entry</code>.<br/>
 * Instances of this class are immutable.
 * 
 * @author Djeang
 */
public final class JkPath implements Iterable<File> {

    private final List<File> entries;

    private JkPath(Iterable<File> entries) {
        super();
        this.entries = Collections.unmodifiableList(JkUtilsIterable.listOf(entries));
    }

    /**
     * Creates a path from a sequence of files.
     */
    public static JkPath of(Iterable<File> entries) {
        final LinkedHashSet<File> files = new LinkedHashSet<File>(JkUtilsIterable.listOf(entries));
        return new JkPath(files);
    }

    /**
     * Creates a <code>JkPath</code> from a base directory and string of
     * relative paths separated with a ";".
     */
    public static JkPath of(File baseDir, String relativePathAsString) {
        final String[] paths = JkUtilsString.split(relativePathAsString, File.pathSeparator);
        final List<File> result = new LinkedList<File>();
        for (final String path : paths) {
            File file = new File(path);
            if (!file.isAbsolute()) {
                file = new File(baseDir, path);
            }
            result.add(file);
        }
        return of(result);
    }

    /**
     * Creates a path from aa array of files.
     */
    public static JkPath of(File... entries) {
        return JkPath.of(Arrays.asList(entries));
    }

    /**
     * Throws an {@link IllegalStateException} if at least one entry does not
     * exist.
     */
    public JkPath assertAllEntriesExist() throws IllegalStateException {
        for (final File file : entries) {
            if (!file.exists()) {
                throw new IllegalStateException("File " + file.getAbsolutePath()
                        + " does not exist.");
            }
        }
        return this;
    }

    /**
     * Returns a <code>JkPath</code> identical to this one but without redundant
     * files. So if a given file in the sequence exist twice or more, then only
     * the first occurrence is kept.
     */
    public JkPath withoutDoubloons() {
        final List<File> files = new LinkedList<File>();
        for (final File file : this) {
            if (!files.contains(file)) {
                files.add(file);
            }
        }
        return new JkPath(files);
    }

    /**
     * Returns the sequence of files as a list.
     */
    public List<File> entries() {
        return entries;
    }

    /**
     * Returns the first entry of this path.
     */
    public File first() {
        return entries.get(0);
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
    public JkPath andHead(File... entries) {
        return andHead(JkPath.of(entries));
    }

    /**
     * Returns a <code>JkPath</code> made of, in the order, the specified
     * entries plus the entries of this one.
     */
    @SuppressWarnings("unchecked")
    public JkPath andHead(Iterable<File> otherEntries) {
        return new JkPath(JkUtilsIterable.chain(otherEntries, this.entries));
    }

    /**
     * @see #and(Iterable).
     */
    public JkPath and(File... files) {
        return and(JkPath.of(files));
    }

    /**
     * Returns a <code>JkPath</code> made of, in the order, the entries of this
     * one plus the specified ones.
     */
    @SuppressWarnings("unchecked")
    public JkPath and(Iterable<File> otherFiles) {
        return new JkPath(JkUtilsIterable.chain(this.entries, otherFiles));
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
        return entries.iterator();
    }

}
