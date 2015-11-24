package org.jerkar.api.java;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsZip;

/**
 * A sequence of file to be used as a <code>class path</code>.<br/>
 * Each file is called an <code>entry</code>.<br/>
 * Each entry is supposed to be either a <code>jar</code> file either a
 * <code>folder</code>.<br/>
 * Non existing files are accepted as valid <code>entry</code>, though they
 * won't contain any classes.
 * 
 * @author Djeang
 */
public final class JkClasspath implements Iterable<File> {

    private static final String WILD_CARD = "*";

    private final List<File> entries;

    private JkClasspath(Iterable<File> entries) {
        super();
        this.entries = Collections.unmodifiableList(resolveWildCard(entries));
    }

    /**
     * Creates a <code>JkClasspath</code> form specified file entries.
     */
    public static JkClasspath of(Iterable<File> entries) {
        return new JkClasspath(entries);
    }

    /**
     * Convenient method to create a <code>JkClassLoader</code> from a given
     * entry plus an sequence of other ones. This scheme is often used for
     * launching some test suites.
     */
    public static JkClasspath of(File entry, Iterable<File> otherEntries) {
        return JkClasspath.of(entry).and(otherEntries);
    }

    /**
     * @see #of(Iterable)
     */
    public static JkClasspath of(File... entries) {
        return JkClasspath.of(Arrays.asList(entries));
    }

    /**
     * Returns the current claaspath as given by
     * <code>System.getProperty("java.class.path")</code>.
     */
    public static JkClasspath current() {
        final List<File> files = new LinkedList<File>();
        final String classpath = System.getProperty("java.class.path");
        final String[] classpathEntries = classpath.split(File.pathSeparator);
        for (final String classpathEntry : classpathEntries) {
            files.add(new File(classpathEntry));
        }
        return JkClasspath.of(files);
    }

    /**
     * Short hand to create a {@link JkPath} from this {@link JkClasspath}.
     */
    public JkPath asPath() {
        return JkPath.of(this);
    }

    /**
     * Throws an {@link IllegalStateException} if one of the entries making of
     * this classloader does not exist.
     */
    public JkClasspath assertAllEntriesExist() {
        for (final File file : entries) {
            if (!file.exists()) {
                throw new IllegalStateException("File " + file.getAbsolutePath()
                        + " does not exist.");
            }
        }
        return this;
    }

    /**
     * Returns each entries making this <code>classpath</code>.
     */
    public List<File> entries() {
        return entries;
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
    public JkClasspath andHead(File... entries) {
        return andHead(JkClasspath.of(entries));
    }

    /**
     * Returns a <code>JkClasspath</code> made of, in the order, the specified
     * entries plus the entries of this one.
     */
    @SuppressWarnings("unchecked")
    public JkClasspath andHead(Iterable<File> otherEntries) {
        return new JkClasspath(JkUtilsIterable.chain(otherEntries, this.entries));
    }

    /**
     * @see #and(Iterable)
     */
    public JkClasspath and(File... files) {
        return and(JkClasspath.of(files));
    }

    /**
     * Returns a <code>JkClasspath</code> made of, in the order, the entries of
     * this one plus the specified ones.
     */
    @SuppressWarnings("unchecked")
    public JkClasspath and(Iterable<File> otherFiles) {
        return new JkClasspath(JkUtilsIterable.chain(this.entries, otherFiles));
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final Iterator<File> it = this.iterator(); it.hasNext();) {
            builder.append(it.next().getAbsolutePath());
            if (it.hasNext()) {
                builder.append(File.pathSeparator);
            }
        }
        return builder.toString();
    }

    private static List<File> resolveWildCard(Iterable<File> files) {
        final LinkedHashSet<File> result = new LinkedHashSet<File>();
        for (final File file : files) {
            if (file.getName().equals(WILD_CARD)) {
                final File parent = file.getParentFile();
                if (!parent.exists()) {
                    JkLog.trace("File " + parent.getAbsolutePath()
                            + " does not exist : classpath entry " + file.getAbsolutePath()
                            + " will be ignored.");
                } else {
                    result.addAll(JkFileTree.of(parent).include("*.jar").files(false));
                }
            } else if (!file.exists()) {
                JkLog.trace("File " + file.getAbsolutePath() + " does not exist : classpath entry "
                        + file.getAbsolutePath() + " will be ignored.");
            } else if (file.isFile()) {
                if (!JkUtilsString.endsWithAny(file.getName().toLowerCase(), ".jar", ".zip")) {
                    throw new IllegalArgumentException("Classpath file element "
                            + file.getAbsolutePath()
                            + " is invalid. It must be either a folder either a jar or zip file.");
                }
                result.add(file);
            } else {
                result.add(file);
            }
        }
        return new ArrayList<File>(result);
    }

    /**
     * Iterator over the entries.
     */
    @Override
    public Iterator<File> iterator() {
        return entries.iterator();
    }

    /**
     * Returns the first entry of this <code>classpath</code> containing the
     * given class.
     */
    public File getEntryContainingClass(String className) {
        final String path = toFilePath(className);
        for (final File file : this) {
            if (file.isDirectory()) {
                if (new File(file, path).exists()) {
                    return file;
                }
            } else {
                final ZipFile zipFile = JkUtilsZip.zipFile(file);
                if (zipFile.getEntry(path) != null) {
                    JkUtilsIO.closeQuietly(zipFile);
                    return file;
                }
                JkUtilsIO.closeQuietly(zipFile);
            }
        }
        return null;
    }

    /**
     * Returns all the elements contained in this classpath. It can be either a
     * class file or any resource file. The element is expressed with its path
     * relative to its containing entry.
     */
    public Set<String> allItemsMatching(JkPathFilter fileFilter) {
        final Set<String> result = new HashSet<String>();
        for (final File classpathEntry : this) {
            if (classpathEntry.isDirectory()) {
                result.addAll(JkFileTree.of(classpathEntry).andFilter(fileFilter).relativePathes());
            } else {
                final ZipFile zipFile = JkUtilsZip.zipFile(classpathEntry);
                for (final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries(); zipEntries
                        .hasMoreElements();) {
                    final ZipEntry zipEntry = zipEntries.nextElement();
                    if (fileFilter.accept(zipEntry.getName())) {
                        result.add(zipEntry.getName());
                    }
                }
                JkUtilsIO.closeQuietly(zipFile);
            }
        }
        return result;
    }

    static String toFilePath(String className) {
        return className.replace('.', '/').concat(".class");
    }

    /**
     * Returns this classpath as an array of URL.
     */
    public URL[] asArrayOfUrl() {
        final URL[] result = new URL[this.entries.size()];
        int i = 0;
        for (final File file : this.entries) {
            result[i] = JkUtilsFile.toUrl(file);
            i++;
        }
        return result;
    }

}
