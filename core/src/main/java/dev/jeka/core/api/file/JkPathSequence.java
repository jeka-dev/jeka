/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.file;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * A sequence of file path (folder or archive). Each file is called an <code>entry</code>.<br/>
 * Instances of this class are immutable.
 *
 * @author Jerome Angibaud
 */
public final class JkPathSequence implements Iterable<Path>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String WILD_CARD = "*";

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
     * Reads the contents of a file at the given path and returns a JkPathSequence object representing
     * the paths extracted from the file contents.
     * The file content is supposed to be have been written using {@link JkPathSequence#writeTo}
     * <p>
     * @throws UncheckedIOException if the file does not exist.
     */
    public static JkPathSequence readFrom(Path path) {
        return JkPathSequence.ofPathString(JkPathFile.of(path).readAsString());
    }

    /**
     * Same as {@link #readFrom} but returns an empty {@link JkPathSequence if the file does not exist.
     */
    public static JkPathSequence readFromQuietly(Path path) {
        if (!Files.exists(path)) {
            return JkPathSequence.of();
        }
        return JkPathSequence.ofPathString(JkPathFile.of(path).readAsString());
    }

    public static JkPathSequence ofPathString(String pathString) {
        List<Path> paths = Arrays.stream(pathString.split(File.pathSeparator))
                .filter(item -> !JkUtilsString.isBlank(item))
                .map(Paths::get)
                .collect(Collectors.toList());
        return JkPathSequence.of(paths);
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

    /**
     * Returns the current classpath as given by
     * <code>System.getProperty("java.class.path")</code>.
     */
    public static JkPathSequence ofSysPropClassPath() {
        final List<Path> files = new LinkedList<>();
        final String classpath = System.getProperty("java.class.path");
        final String[] classpathEntries = classpath.split(File.pathSeparator);
        for (final String classpathEntry : classpathEntries) {
            files.addAll(resolveWildCard(classpathEntry));
        }
        return JkPathSequence.of(files);
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
     * @deprecated Use {@link #toList()} instead.
     */
    @Deprecated
    public List<Path> getEntries() {
        return entries;
    }

    public List<Path> toList() {
        return entries;
    }

    public Path getElement(int index) {
        return entries.get(index);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @deprecated Use {@link #getElement(int)} instead.
     */
    @Deprecated
    public Path getEntry(int index) {
        return entries.get(index);
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
        return String.join(File.pathSeparator, entries.stream()
                .map(Path::toString)
                .collect(Collectors.toList()));
    }

    /**
     * Returns the file names concatenated with ':'
     */
    public String toColonSeparatedPath() {
        return String.join(":", entries.stream().map(Path::toString).collect(Collectors.toList()));
    }

    public String toPathMultiLine(String margin) {
        return String.join("\n", entries.stream().map(path -> margin + path.toString()).collect(Collectors.toList()));
    }

    public Set<Path> toSet() {
        return new LinkedHashSet<>(this.entries);
    }

    public URL[] toUrls() {
        List<URL> urls = entries.stream().map(JkUtilsPath::toUrl).collect(Collectors.toList());
        return urls.toArray(new URL[0]);
    }

    public boolean hasNonExisting() {
        return this.entries.stream().anyMatch(path -> !Files.exists(path));
    }

    public List<Path> getNonExistingFiles() {
        return this.entries.stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
    }

    /**
     * Returns the first entry of this <code>classpath</code> containing the given class.
     */
    public Path findEntryContainingClass(String className) {
        final String path = toFilePath(className);
        for (final Path file : entries) {
            if (!Files.exists(file)) {
                continue;
            }
            if (Files.isDirectory(file)) {
                if (Files.exists(file.resolve(path))) {
                    return file;
                }
            } else {
                final ZipFile zipFile = JkUtilsZip.getZipFile(file.toFile());
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
     * Writes the contents of this JkPathSequence to the specified file path.
     */
    public void writeTo(Path path) {
        JkPathFile.of(path).createIfNotExist().write(toPath(), StandardOpenOption.TRUNCATE_EXISTING);
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

    private static List<Path> resolveWildCard(String candidatePath) {
        final List<Path> result = new LinkedList<>();
        if (candidatePath.endsWith(WILD_CARD)) {
            final String candidateFolder = JkUtilsString.substringBeforeFirst(candidatePath, WILD_CARD);
            final Path parent = Paths.get(candidateFolder);
            if (!Files.exists(parent)) {
                JkLog.verbose("File %s does not exist : classpath entry %s will be ignored.", parent, candidateFolder);
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
