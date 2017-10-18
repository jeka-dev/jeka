package org.jerkar.api.java;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;

import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsZip;

/**
 * A sequence ofMany file to be used as a <code>class path</code>.<br/>
 * Each file is called an <code>entry</code>.<br/>
 * Each entry is supposed to be either a <code>jar</code> file either a
 * <code>folder</code>.<br/>
 * Non existing files are accepted as valid <code>entry</code>, though they
 * won't contain any classes.
 *
 * @author Djeang
 */
public final class JkClasspath implements Iterable<Path> {

    private static final String WILD_CARD = "*";

    private static final String PATH_SEPARATOR = System.getProperty("path.separator");

    private final List<Path> entries;
    
    // ------------------- constructor && factory methods

    private JkClasspath(Iterable<Path> entries) {
        this.entries = Collections.unmodifiableList(resolveWildCard(entries));
    }

    /**
     * Creates a <code>JkClasspath</code> form specified file entries.
     */
    public static JkClasspath ofMany(Iterable<Path> entries) {
        return new JkClasspath(JkUtilsPath.disambiguate(entries));
    }

    /**
     * Creates a <code>JkClasspath</code> form specified file entries.
     */
    public static JkClasspath of(Path ... paths) {
        return ofMany(Arrays.asList(paths));
    }

    /**
     * Returns the current claaspath as given by
     * <code>System.getProperty("java.class.path")</code>.
     */
    public static JkClasspath current() {
        final List<Path> files = new LinkedList<>();
        final String classpath = System.getProperty("java.class.path");
        final String[] classpathEntries = classpath.split(PATH_SEPARATOR);
        for (final String classpathEntry : classpathEntries) {
            files.addAll(resolveWildCard(classpathEntry));
        }
        return JkClasspath.ofMany(files);
    }
    
    // --------------------------------- Iterate -----------------------------

    /**
     * Short hand to create a {@link JkPathSequence} from this {@link JkClasspath}.
     */
    public JkPathSequence asPath() {
        return JkPathSequence.ofMany(this.entries);
    }

    /**
     * Returns each entries making this <code>classpath</code>.
     */
    public List<Path> entries() {
        return entries;
    }

    @Override
    public Iterator<Path> iterator() {
        return entries.iterator();
    }

    /**
     * Returns this classpath as an array ofMany URL.
     */
    public URL[] asArrayOfUrl() {
        final URL[] result = new URL[this.entries.size()];
        int i = 0;
        for (final Path file : this.entries) {
            try {
                result[i] = file.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(file + " can't be transformed to url", e);
            }
            i++;
        }
        return result;
    }

    /**
     * Returns the first entry ofMany this <code>classpath</code> containing the
     * given class.
     */
    public Path getEntryContainingClass(String className) {
        final String path = toFilePath(className);
        for (final Path file : entries) {
            if (Files.isDirectory(file)) {
                if (Files.exists(file.resolve(path))) {
                    return file;
                }
            } else {
                final ZipFile zipFile = JkUtilsZip.zipFile(file.toFile());
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
    Set<Path> allItemsMatching(JkPathFilter fileFilter) {
        final Set<Path> result = new HashSet<>();
        for (final Path classpathEntry : this.entries) {
            if (Files.isDirectory(classpathEntry)) {
                result.addAll(JkFileTree.of(classpathEntry).andFilter(fileFilter).relativeFiles());
            } else {
                final ZipFile zipFile = JkUtilsZip.zipFile(classpathEntry.toFile());
                for (final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries(); zipEntries
                        .hasMoreElements();) {
                    final ZipEntry zipEntry = zipEntries.nextElement();
                    if (fileFilter.accept(zipEntry.getName())) {
                        result.add(Paths.get(zipEntry.getName()));
                    }
                }
                JkUtilsIO.closeQuietly(zipFile);
            }
        }
        return result;
    }

   // ------------------------------ wither, adder --------------------------------------------

    /**
     * Returns a <code>JkClasspath</code> made ofMany, in the order, the specified
     * entries plus the entries ofMany this one.
     */
    @SuppressWarnings("unchecked")
    public JkClasspath andManyFirst(Iterable<Path> otherEntries) {
        Iterable<Path> paths = JkUtilsPath.disambiguate(otherEntries);
        final LinkedList<Path> list = new LinkedList<>();
        paths.forEach(path -> list.add(path));
        list.addAll(this.entries);
        return new JkClasspath(list);
    }

    /**
     * Returns a <code>JkClasspath</code> made ofMany, in the order, the entries ofMany
     * this one plus the specified ones.
     */
    public JkClasspath andMany(Iterable<Path> otherEntries) {
        Iterable<Path> paths = JkUtilsPath.disambiguate(otherEntries);
        final List<Path> list = new LinkedList<>(this.entries);
        paths.forEach(path -> list.add(path));
        return new JkClasspath(list);
    }

    /**
     * See {@link #andMany(Iterable)}
     */
    public JkClasspath and(Path ... paths) {
        return andMany(Arrays.asList(paths));
    }

    /**
     * See {@link #andManyFirst(Iterable)}
     */
    public JkClasspath andFirst(Path ... paths) {
        return andMany(Arrays.asList(paths));
    }
    
    // ------------------------- canonical methods --------------------------------------
    
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final Iterator<Path> it = this.entries.iterator(); it.hasNext();) {
            builder.append(it.next().toAbsolutePath().toString());
            if (it.hasNext()) {
                builder.append(PATH_SEPARATOR);
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkClasspath classpath = (JkClasspath) o;

        return entries.equals(classpath.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }
    
    // ----------------- privates 

    private static List<Path> resolveWildCard(Iterable<Path> files) {
        final LinkedHashSet<Path> result = new LinkedHashSet<>();
        for (final Path file : files) {
            if (file.getFileName().toString().equals(WILD_CARD)) {
                final Path parent = file.getParent();
                if (!Files.exists(parent)) {
                    JkLog.trace("File " + parent
                            + " does not exist : classpath entry " + file
                            + " will be ignored.");
                } else {
                    result.addAll(JkFileTree.of(parent).include("*.jar").files());
                }
            } else if (!Files.exists(file)) {
                JkLog.trace("File " + file + " does not exist : classpath entry "
                        + file + " will be ignored.");
            } else if (Files.isRegularFile(file)) {
                if (!JkUtilsString.endsWithAny(file.getFileName().toString().toLowerCase(), ".jar", ".zip")) {
                    throw new IllegalArgumentException("Classpath file element "
                            + file.toAbsolutePath().toString()
                            + " is invalid. It must be either a folder either a jar or zip file.");
                }
                result.add(file);
            } else {
                result.add(file);
            }
        }
        return new ArrayList<>(result);
    }

    private static List<Path> resolveWildCard(String candidatePath) {
        List<Path> result = new LinkedList<>();
        if (candidatePath.endsWith(WILD_CARD)) {
            File file = new File(candidatePath);
            final Path parent = file.getParentFile().toPath();
            if (!Files.exists(parent)) {
                JkLog.trace("File " + parent
                        + " does not exist : classpath entry " + file
                        + " will be ignored.");
            } else {
                result.addAll(JkFileTree.of(parent).include("*.jar").files());
            }
        } else {
            result.add(Paths.get(candidatePath));
        }
        return result;
    }

    private static String toFilePath(String className) {
        return className.replace('.', '/').concat(".class");
    }


}
