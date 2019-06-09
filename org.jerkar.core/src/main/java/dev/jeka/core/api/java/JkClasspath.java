package dev.jeka.core.api.java;

import dev.jeka.core.api.utils.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipFile;


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
     * @param paths As {@link Path} class implements {@link Iterable<Path>} the argument can be a single {@link Path}
     * instance, if so it will be interpreted as a list containing a single element which is this argument.
     */
    public static JkClasspath of(Iterable<Path> paths) {
        return new JkClasspath(JkUtilsPath.disambiguate(paths));
    }

    public static JkClasspath of() {
        return of(Collections.emptyList());
    }

    /**
     * Creates a <code>JkClasspath</code> form specified file entries.
     */
    public static JkClasspath of(Path path1, Path path2, Path... others) {
        return of(JkUtilsIterable.listOf2orMore(path1, path2, others));
    }

    /**
     * Returns the current classpath as given by
     * <code>System.getProperty("java.class.path")</code>.
     */
    public static JkClasspath ofCurrentRuntime() {
        final List<Path> files = new LinkedList<>();
        final String classpath = System.getProperty("java.class.path");
        final String[] classpathEntries = classpath.split(PATH_SEPARATOR);
        for (final String classpathEntry : classpathEntries) {
            files.addAll(resolveWildCard(classpathEntry));
        }
        return JkClasspath.of(files);
    }
    
    // --------------------------------- Iterate -----------------------------

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
     * Returns this classpath as an array of URL.
     */
    public URL[] toArrayOfUrl() {
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
     * Returns the first entry of this <code>classpath</code> containing the given class.
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

               /* if (Files.exists(JkPathTree.ofZip(file).get(path))) {
                    return file;
                }*/
            }
        }
        return null;
    }

    Set<Path> getAllPathMatching(Iterable<String> globPatterns) {
        final Set<Path> result = new LinkedHashSet<>();
        for (final Path classpathEntry : this.entries) {
            JkPathTree tree = Files.isDirectory(classpathEntry) ?
                    JkPathTree.of(classpathEntry) : JkPathTree.ofZip(classpathEntry);
            result.addAll(tree.andMatching(true, globPatterns).getRelativeFiles());
        }
        return result;
    }

   // ------------------------------ wither, adder --------------------------------------------

    /**
     * Returns a <code>JkClasspath</code> made of, in the order, the specified
     * entries plus the entries of this one.
     * @param paths As {@link Path} class implements {@link Iterable<Path>} the argument can be a single {@link Path}
     * instance, if so it will be interpreted as a list containing a single element which is this argument.
     */
    public JkClasspath andPrepending(Iterable<Path> paths) {
        List<Path> result = JkUtilsPath.disambiguate(paths);
        result.addAll(0, this.entries);
        return new JkClasspath(result);
    }

    /**
     * Returns a <code>JkClasspath</code> made of, in the order, the entries of
     * this one plus the specified ones.
     *
     * @param paths As {@link Path} class implements {@link Iterable<Path>} the argument can be a single {@link Path}
     * instance, if so it will be interpreted as a list containing a single element which is this argument.
     */
    public JkClasspath and(Iterable<Path> paths) {
        List<Path> result = JkUtilsPath.disambiguate(paths);
        final List<Path> list = new LinkedList<>(this.entries);
        list.addAll(result);
        return new JkClasspath(list);
    }

    /**
     * See {@link #and(Iterable)}
     */
    public JkClasspath and(Path path1, Path path2, Path ... others) {
        return and(JkUtilsIterable.listOf2orMore(path1, path2, others));
    }

    /**
     * See {@link #andPrepending(Iterable)}
     */
    public JkClasspath andPrepending(Path path1, Path path2, Path... others) {
        return and(JkUtilsIterable.listOf2orMore(path1, path2, others));
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
                    result.addAll(JkPathTree.of(parent).andMatching(true,"*.jar").getFiles());
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
            String candidateFolder = JkUtilsString.substringBeforeFirst(candidatePath, WILD_CARD);
            final Path parent = Paths.get(candidateFolder);
            if (!Files.exists(parent)) {
                JkLog.trace("File " + parent
                        + " does not exist : classpath entry " + candidatePath
                        + " will be ignored.");
            } else {
                result.addAll(JkPathTree.of(parent).andMatching(true,"**.jar").getFiles());
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
