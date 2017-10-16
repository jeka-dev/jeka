package org.jerkar.api.file;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsPath;

/**
 * Provides a view on files and sub-folders contained in a given directory. A
 * <code>JkFileTree</code> may have some include/exclude filters to include only
 * or exclude some files based on ANT pattern matching. <br/>
 *
 * <p>
 * When speaking about files contained in a {@link JkFileTree}, we mean all
 * files contained in its root directory or sub-directories, matching positively
 * the filter defined on it.
 *
 * @author Jerome Angibaud
 */
public final class JkFileTree  {

    /**
     * Creates a {@link JkFileTree} having the specified root directory.
     */
    @Deprecated
    public static JkFileTree of(File rootDir) {
        return new JkFileTree(rootDir.toPath(), false);
    }

    /**
     * Creates a {@link JkFileTree} having the specified root directory.
     */
    public static JkFileTree of(Path rootDir) {
        return new JkFileTree(rootDir, false);
    }

    /**
     * Creates a {@link JkFileTree} having the specified root directory.
     */
    public static JkFileTree ofZip(Path zipFile) {
        return new JkFileTree(zipFile.toAbsolutePath(), true);
    }

    private final RootHolder root;

    private final JkPathMatcher filter;

    private JkFileTree(Path rootDir, boolean zip) {
        this(rootDir, JkPathMatcher.of(path -> true), zip);
    }

    private JkFileTree(Path rootDirOrArchive, JkPathMatcher matcher, boolean zipFile) {
        this.root = zipFile ? RootHolder.ofZip(rootDirOrArchive) : RootHolder.ofDir(rootDirOrArchive);
        this.filter = matcher;
    }

    private JkFileTree(RootHolder rootHolder, JkPathMatcher matcher) {
        this.root = rootHolder;
        this.filter = matcher;
    }

    /**
     * Returns the root directory. In case of zip archive it returns an directory entry
     * within the zip archive.
     */
    public Path root() {
        return root.get();
    }

    /**
     * Returns root of this tree if this tree is a directory tree and returns zip file if this
     * tree is a zip tree.
     */
    public Path rootFile() {
        return  root.rootFile();
    }

    /**
     * Returns the filter defined on this {@link JkFileTree}, never <code>null</code>.
     */
    public JkPathMatcher matcher() {
        return filter;
    }

    // ------------------------------- functional ---------------------------------

    private Predicate<Path> excludeRootFilter() {
        return path -> !path.equals(root());
    }

    private Function<Path, Path> relativePathFunction() {
        return path ->  root().relativize(path);
    }

    // ------------------------- check exists --------------------------------------

    /**
     * Returns <code>true</code> if the root directory exists.
     */
    public boolean exists() {
        return root.exists();
    }

    private void createIfNotExist() {
        this.root.createIfNotExist();
    }


    // ----------------------- iterate over files ---------------------------------------------------

    /**
     * Returns a path stream on this root path and all its children matching this filter.
     * Returned paths are resolved against this tree root.
     * This means that if this tree root is absolute then streamed paths are absolute as well.
     * This also means that the root folder is included in the stream.
     * If this method is called for a zip instance, then it should be closed or called within
     * <i>try-with-resources</i> statement in order to avoid resource leaks.
     */
    public Stream<Path> stream(FileVisitOption ...options) {
        if(!exists()) {
            return new LinkedList<Path>().stream();
        }
        final JkPathMatcher matcher = JkPathMatcher.of(filter);
        return JkUtilsPath.walk(root(), options)
                .filter(path -> matcher.matches(root().relativize(path))).onClose(() -> root.closeIfNeeded());
    }

    /**
     * Same as {@link #files()} but returning paths relative to this root.
     */
    public List<Path> relativeFiles() {
        try(Stream<Path> stream = stream()) {
            return stream.filter(JkPathMatcher.noDirectory()).map(relativePathFunction()).collect(Collectors.toList());
        }
    }

    /**
     * Returns list of paths returned by {@link #stream(FileVisitOption...)} but excluding
     * directories from the result.
     */
    public List<Path> files() {
        try (Stream<Path> stream = stream()) {
            return stream.filter(JkPathMatcher.noDirectory()).collect(Collectors.toList());
        }
    }


    // ---------------------- Navigate -----------------------------------------------------------

    /**
     * Creates a {@link JkFileTree} having the default filter and the specified
     * relative path to this root as root directory.
     */
    public JkFileTree go(String relativePath) {
        final Path path = root().resolve(relativePath);
        return JkFileTree.of(path);
    }

    /**
     * Returns path relative to this root of the specified relative path.
     */
    public Path get(String relativePath) {
        return root().resolve(relativePath);
    }

    // ----------------------- Write in ----------------------------------------------------------------


    /**
     * Short hand for <code>#importTree(JkFileTree.of(dirToCopy, options)</code>.
     */
    public JkFileTree importDir(Path dirToCopy, CopyOption... copyOptions) {
        return importTree(JkFileTree.of(dirToCopy), copyOptions);
    }

    /**
     * Copies the content of the specified tree at the root of this one.
     * Specified tree to copy might not exists. The structure of the specified tree
     * is preserved.
     */
    public JkFileTree importTree(JkFileTree tree, CopyOption... copyOptions) {
        createIfNotExist();
        if (tree.exists()) {
            tree.stream().filter(excludeRootFilter()).forEach(path -> {
                Path target = this.root().resolve(tree.root().relativize(path).toString());
                if (Files.isDirectory(path)) {
                    JkUtilsPath.createDirectories(target);
                } else {
                    JkUtilsPath.copy(path, target, copyOptions);
                }
            });
        }
        return this;
    }

    /**
     * Copies the specified files at the root of this tree.
     */
    public JkFileTree importFiles(Iterable<Path> files, StandardCopyOption ... copyOptions) {
        createIfNotExist();
        for (final Path file : files) {
            JkUtilsPath.copy(file, root().resolve(file.getFileName()), copyOptions);
        }
        return this;
    }

    /**
     * Copies the specified files at the root of this tree
     */
    // WARN : importFiles(Iterable<Path>) is invoked when calling #importFiles with a single parameters
    // so it has been renamed with distinct name
    public JkFileTree importFile(Path... files) {
        createIfNotExist();
        for (final Path file : files) {
            JkUtilsPath.copy(file, root().resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return this;
    }

    /**
     * Deletes each and every files in this tree except the root and files not matching this tree filter.
     */
    public JkFileTree deleteAll() {
        JkUtilsPath.walkFileTree(root(), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return visit(file);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return visit(dir);
            }

            private FileVisitResult visit(Path path) {
                if (!JkUtilsPath.isSameFile(root(), path) && filter.matches(path)) {
                    JkUtilsPath.deleteFile(path);
                }
                return FileVisitResult.CONTINUE;
            }

        });
        return this;
    }

    // ----------------------- Write out ----------------------------------------------------------------

    /**
     * Zips the content of this tree to the specified destination file. If the specified destination file
     * already exists, the content of this tree is appended to the existing archive, overriding existing entries within the archive.
     */
    public JkFileTree zipTo(Path destination) {
        if (destination.getParent() != null) {
            JkUtilsPath.createDirectories(destination.getParent());
        }
        final Path zipRootEntry = JkUtilsPath.zipRoot(destination);
        try (FileSystem fileSystem = zipRootEntry.getFileSystem();
             Stream<Path> stream = this.stream()) {

            stream.filter(excludeRootFilter()).forEach(path -> {
                Path zipEntry = zipRootEntry.resolve(root().relativize(path).toString());
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
     * Copies files contained in this {@link JkFileTree} to the specified
     * directory.
     */
    public int copyTo(Path destinationDir, CopyOption ... copyOptions) {
        if (!Files.exists(destinationDir)) {
            JkUtilsPath.createDirectories(destinationDir);
        }
        return JkUtilsPath.copyDirContent(root(), destinationDir, filter, copyOptions);
    }



    // ------------------------- Filter ----------------------------------------------

    /**
     * Creates a {@link JkFileTree} which is a copy of this {@link JkFileTree}
     * augmented with the specified {@link JkPathFilter}
     */
    public JkFileTree andFilter(JkPathFilter filter) {
        return new JkFileTree(root, this.filter.and((PathMatcher) filter));
    }


    /**
     * Creates a {@link JkFileTree} which is a copy of this {@link JkFileTree}
     * augmented with the specified {@link JkPathFilter}
     */
    public JkFileTree andMatcher(PathMatcher pathMatcher) {
        return new JkFileTree(root, this.filter.and(pathMatcher));
    }

    /**
     * Short hand to {@link #andFilter(JkPathFilter)} defining an include Ant
     * pattern filter. This will include any file matching at least one of the
     * specified <code>antPatterns</code>.
     */
    public JkFileTree include(String... antPatterns) {
        return andFilter(JkPathFilter.include(antPatterns));
    }


    /**
     * Short hand to {@link #andFilter(JkPathFilter)} defining an exclude Ant
     * pattern filter. This will exclude any file matching at least one of
     * specified <code>antPatterns</code>.
     */
    public JkFileTree exclude(String... antPatterns) {
        return andFilter(JkPathFilter.exclude(antPatterns));
    }

    // ------------------------ Misc ---------------------------------------

    /**
     * Returns the file count contained in this {@link JkFileTree} to concurrence to specified max count.
     * If the effective count is greater than max count, returns <code>max + 1</code>.
     * This method is designed to stop file traversal as soon as count is greater than max.
     */
    public int count(int max, boolean includeDirectories) {
        if (!exists()) {
            return 0;
        }
        return JkUtilsPath.childrenCount(root(), max, includeDirectories);
    }

    /**
     * If the root of this tree is absolute then this method returns this tree.
     * If the root of this tree is relative then this method returns a tree having a root
     * resolved from the specified path to this root.
     */
    public JkFileTree resolve(Path path) {
        return new JkFileTree(root.resolve(path), this.filter);
    }


    /**
     * Returns a {@link JkFileTreeSet} containing this tree as its single
     * element.
     */
    public JkFileTreeSet asSet() {
        return JkFileTreeSet.of(this);
    }

    @Override
    public String toString() {
        return root + ":" + filter;
    }

    private static class RootHolder {
        final Path zipFile;
        Path root;

        static RootHolder ofZip(Path zipFile) {
            JkUtilsAssert.notNull(zipFile, "zip archive file can't be null.");
            JkUtilsAssert.isTrue(!Files.exists(zipFile) || !Files.isDirectory(zipFile),
                    "Specified zip file " + zipFile + " can't be a directory");
            return new RootHolder(zipFile, null);
        }

        static RootHolder ofDir(Path dir) {
            JkUtilsAssert.notNull(dir, "Directory root tree can't be null.");
            JkUtilsAssert.isTrue(!Files.exists(dir) || Files.isDirectory(dir),
                    "Specified zip file " + dir + " must be a directory");
            return new RootHolder(null, dir);
        }

        private RootHolder(Path zipFile, Path root) {
            this.zipFile = zipFile;
            this.root = root;
        }

        Path get() {
            if (isZip() && root == null) {
                root = JkUtilsPath.zipRoot(zipFile);
            }
            return root;
        }

        void createIfNotExist() {
            if (zipFile == null) {
                if (!Files.exists(root)) {
                    JkUtilsPath.createDirectories(root);
                }
            } else {
                if (!Files.exists(zipFile)) {
                    JkUtilsPath.createDirectories(zipFile.getParent());
                    root = JkUtilsPath.zipRoot(zipFile);
                } else if (root == null) {
                    root = JkUtilsPath.zipRoot(zipFile);
                } else if (root.getFileSystem().isOpen()) {
                    JkUtilsPath.createDirectories(root);
                } else {
                    Path zipRoot = JkUtilsPath.zipRoot(zipFile);
                    root = zipRoot.getFileSystem().getPath(root.toString());
                }
            }
        }

        boolean exists() {
            if (!isZip()) {
                return Files.exists(root);
            }
            if (!Files.exists(zipFile)) {
                return false;
            }
            if (root == null) {
                return true; // zip root always exists
            }
            if (root.getFileSystem().isOpen()) {
                return Files.exists(root);
            }
            Path zipRoot = JkUtilsPath.zipRoot(zipFile);
            root = zipRoot.getFileSystem().getPath(root.toString());
            return Files.exists(root);
        }

        boolean isZip() {
            return zipFile != null;
        }

        void closeIfNeeded() {
            if (isZip() && root != null) {
                try {
                    root.getFileSystem().close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        RootHolder resolve(Path path) {
            if (isZip()) {
                return this;
            }
            return new RootHolder(null, path.resolve(root));
        }

        Path rootFile() {
            if (isZip()) {
                return this.zipFile;
            }
            return this.root;
        }

    }

}
