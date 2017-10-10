package org.jerkar.api.file;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsPath;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides a view on entries contained in a zip files. A
 * <code>JkZipTree</code> may have some include/exclude filters to include only
 * or exclude some entries. <br/>
 *
 * @author Jerome Angibaud
 */
public final class JkZipTree {

    /**
     * Creates a {@link JkZipTree} having the specified root directory.
     */
    public static JkZipTree of(Path zipFile) {
        return new JkZipTree(zipFile);
    }

    private final Path zipFile;

    private final JkPathMatcher filter;

    private Path root;

    private JkZipTree(Path rootDir) {
        this(rootDir, JkPathMatcher.of(path -> true));
    }

    private JkZipTree(Path zipFile, JkPathMatcher matcher) {
        JkUtilsAssert.notNull(zipFile, "zip file dir can't be null.");
        JkUtilsAssert.notNull(matcher, "Matcher can't be null");
        JkUtilsAssert.isTrue(!Files.exists(zipFile) || !Files.isDirectory(zipFile), zipFile + " can't be a directory.");
        this.zipFile = zipFile;
        this.filter = matcher;
        this.root = JkUtilsPath.zipRoot(zipFile);
    }

    /**
     * Returns the root directory.
     */
    public Path root() {
        if (!root.getFileSystem().isOpen()) {
            root = JkUtilsPath.zipRoot(zipFile);
        }
        return root;
    }

    /**
     * Returns the filter defined on this {@link JkZipTree}, never <code>null</code>.
     */
    public JkPathMatcher matcher() {
        return filter;
    }

    // ------------------------------- functional ---------------------------------

    private final Predicate<Path> excludeRootFilter() {
        return path -> !path.equals(root);
    }

    private final Consumer<Path> importer() {
        return path -> JkUtilsPath.copy(path, root.resolve(path.getFileName()));
    }

    private final Function<Path, Path> relativePathFunction() {
        return path ->  root.relativize(path);
    }

    // ------------------------- check exists --------------------------------------

    /**
     * Returns if the asScopedDependency directory exists. (Short hand for #asScopedDependency.exists()).
     */
    public boolean exists() {
        return Files.exists(zipFile);
    }

    private JkZipTree createIfNotExist() {
        if (!Files.exists(root)) {
            JkUtilsPath.createDirectories(root);
        }
        return this;
    }


    // ----------------------- iterate over files ---------------------------------------------------

    /**
     * Returns a path stream on child path of this tree root and matching its filter. Returned paths are resolved against
     * this tree root. This means if this tree root is absolute then streamed paths are absolute as well.
     */
    public Stream<Path> stream(FileVisitOption ...options) {
        if(!exists()) {
            return new LinkedList<Path>().stream();
        }
        final JkPathMatcher matcher = JkPathMatcher.of(filter);
        return JkUtilsPath.walk(root, options)
                .filter(path -> matcher.matches(root.relativize(path)));
    }

    /**
     * Returns absolute path of all files contained in this {@link JkZipTree}.
     * Result does not contains directory entries.
     */
    public List<Path> filesOnlyRelative() {
        return stream().filter(JkPathMatcher.noDirectory()).map(relativePathFunction()).collect(Collectors.toList());
    }

    /**
     * Returns absolute path of all files contained in this {@link JkZipTree}.
     * Result does not contains directory entries.
     */
    public List<Path> filesOnly() {
        return stream().filter(JkPathMatcher.noDirectory()).collect(Collectors.toList());
    }


    // ---------------------- Navigate -----------------------------------------------------------

    /**
     * Returns path relative to this root of the specified relative path.
     */
    public Path get(String relativePath) {
        return root.resolve(relativePath);
    }

    // ----------------------- Write in ----------------------------------------------------------------


    /**
     * Copies the content of the specified directory at the root of this tree.
     * Specified directories to copy might not exist.
     */
    public JkZipTree importContent(Path... dirsToCopy) {
        createIfNotExist();
        for (final Path dirToCopy : dirsToCopy) {
            if (!Files.exists(dirToCopy)) {
                continue;
            }
            JkUtilsPath.copyDirContent(dirToCopy, root, StandardCopyOption.REPLACE_EXISTING);
        }
        return this;
    }

    /**
     * Copies the specified files at the root of this tree
     */
    public JkZipTree importFiles(Iterable<Path> files) {
        createIfNotExist();
        for (final Path file : files) {
            JkUtilsPath.copy(file, root.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return this;
    }

    /**
     * Copies the specified files at the root of this tree
     */
    // WARN : importFiles(Iterable<Path>) is invoked when calling #importFiles with a single parameters
    // so it has been renamed with distinct name
    public JkZipTree importFile(Path... files) {
        createIfNotExist();
        for (final Path file : files) {
            JkUtilsPath.copy(file, root.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return this;
    }

    /**
     * Deletes each and every files in this tree except the root and files not matching this tree filter.
     */
    public JkZipTree deleteAll() {
        JkUtilsPath.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return visit(file);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return visit(dir);
            }

            private FileVisitResult visit(Path path) {
                if (!JkUtilsPath.isSameFile(root, path) && filter.matches(path)) {
                    JkUtilsPath.deleteFile(path);
                }
                return FileVisitResult.CONTINUE;
            }

        });
        return this;
    }

    // ----------------------- Write out ----------------------------------------------------------------

    /**
     * Zip the content of this tree to the specified destination file. If the specified destination file
     * already exist, the content of this tree is appended to the existing archive, overriding existing file
     */
    public JkZipTree zipTo(Path destination) {
        if (destination.getParent() != null) {
            JkUtilsPath.createDirectories(destination.getParent());
        }
        Path zipRootEntry = JkUtilsPath.zipRoot(destination);
        try (FileSystem fileSystem = zipRootEntry.getFileSystem()) {
            this.stream().filter(excludeRootFilter()).forEach(path -> {
                Path zipEntry = zipRootEntry.resolve(root.relativize(path).toString());
                if (!Files.exists(zipEntry) || !Files.isDirectory(zipEntry)) {
                    JkUtilsPath.copy(path, zipEntry, StandardCopyOption.REPLACE_EXISTING);
                }
            });
        } catch (IOException e) {
           throw new UncheckedIOException(e);
        }
        return this;
    }

    /**
     * Copies files contained in this {@link JkZipTree} to the specified
     * directory.
     */
    public int copyTo(Path destinationDir) {
        if (!Files.exists(destinationDir)) {
            JkUtilsPath.createDirectories(destinationDir);
        }
        return JkUtilsPath.copyDirContent(root, destinationDir, StandardCopyOption.REPLACE_EXISTING);
    }



    // ------------------------- Filter ----------------------------------------------

    /**
     * Creates a {@link JkZipTree} which is a copy of this {@link JkZipTree}
     * augmented with the specified {@link JkPathFilter}
     */
    public JkZipTree andFilter(JkPathFilter filter) {
        final PathMatcher pathMatcher = filter;
        return new JkZipTree(root, this.filter.and(pathMatcher));
    }


    /**
     * Creates a {@link JkZipTree} which is a copy of this {@link JkZipTree}
     * augmented with the specified {@link JkPathFilter}
     */
    public JkZipTree andMatcher(PathMatcher pathMatcher) {
        return new JkZipTree(root, this.filter.and(pathMatcher));
    }

    /**
     * Short hand to {@link #andFilter(JkPathFilter)} defining an include Ant
     * pattern filter. This will include any file matching at least one of the
     * specified <code>antPatterns</code>.
     */
    public JkZipTree include(String... antPatterns) {
        return andFilter(JkPathFilter.include(antPatterns));
    }


    /**
     * Short hand to {@link #andFilter(JkPathFilter)} defining an exclude Ant
     * pattern filter. This will exclude any file matching at least one of
     * specified <code>antPatterns</code>.
     */
    public JkZipTree exclude(String... antPatterns) {
        return andFilter(JkPathFilter.exclude(antPatterns));
    }

    // ------------------------ Misc ---------------------------------------

    /**
     * Returns the file count contained in this {@link JkZipTree} to concurrence to specified max count.
     * If the effective count is greater than max count, returns <code>max + 1</code>.
     * This method is designed to stop file traversal as soon as count is greater than max.
     */
    public int count(int max, boolean includeDirectories) {
        if (!exists()) {
            return 0;
        }
        return JkUtilsPath.childrenCount(root, max, includeDirectories);
    }

    @Override
    public String toString() {
        return zipFile + ":" + filter;
    }

}
