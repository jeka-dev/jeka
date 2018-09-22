package org.jerkar.api.file;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsPath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides a view on files and sub-folders contained in a given directory or zip file. A
 * <code>JkPathTree</code> may have some include/exclude filters to include only
 * or exclude specified files.<br/>
 * When speaking about files contained in a {@link JkPathTree}, we mean all
 * files contained in its root directory or sub-directories, matching positively
 * the filter defined on it.
 *
 * @author Jerome Angibaud
 */
public final class JkPathTree {

    private static final JkPathMatcher NO_FILTER = JkPathMatcher.of();

    /**
     * Creates a {@link JkPathTree} having the specified root directory.
     */
    public static JkPathTree of(Path rootDir) {
        return new JkPathTree(rootDir, false);
    }

    /**
     * Creates a {@link JkPathTree} having the specified root directory.
     */
    public static JkPathTree ofZip(Path zipFile) {
        return new JkPathTree(zipFile.toAbsolutePath(), true);
    }

    private final RootHolder rootHolder;

    private final JkPathMatcher filter;

    private JkPathTree(Path rootDir, boolean zip) {
        this(rootDir, NO_FILTER, zip);
    }

    private JkPathTree(Path rootDirOrArchive, JkPathMatcher matcher, boolean zipFile) {
        this.rootHolder = zipFile ? RootHolder.ofZip(rootDirOrArchive) : RootHolder.ofDir(rootDirOrArchive);
        this.filter = matcher;
    }

    private JkPathTree(RootHolder rootHolder, JkPathMatcher matcher) {
        this.rootHolder = rootHolder;
        this.filter = matcher;
    }

    /**
     * Returns the root directory. In case of zip archive it returns a directory entry
     * within the zip archive.
     */
    public Path root() {
        return rootHolder.get();
    }

    /**
     * Returns root directory if this tree is a directory tree and returns a zip file if this
     * tree has been created from a zip file.
     */
    public Path rootDirOrZipFile() {
        return  rootHolder.rootFile();
    }

    /**
     * Returns the filter defined on this {@link JkPathTree}, never <code>null</code>.
     */
    public JkPathMatcher matcher() {
        return filter;
    }

    /**
     * Returns true if a filter has explicitly been defined on this tree.
     */
    public boolean isDefineFilter() {
        return this.filter == NO_FILTER;
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
        return rootHolder.exists();
    }

    /**
     * Creates root directory if not exists.
     */
    public JkPathTree createIfNotExist() {
        this.rootHolder.createIfNotExist();
        return this;
    }


    // ----------------------- iterate over files ---------------------------------------------------

    /**
     * Returns a path stream of all files of this tree. Returned paths are resolved against this tree root.
     * This means that if this tree root is absolute then streamed paths are absolute as well.
     * Note that the root folder is included in the returned stream.
     * If this method is called for a zip tree instance, then it should be closed or called within
     * <i>try-with-resources</i> statement in order to avoid resource leaks.
     */
    public Stream<Path> stream(FileVisitOption ...options) {
        if(!exists()) {
            return new LinkedList<Path>().stream();
        }
        final JkPathMatcher matcher = JkPathMatcher.of(filter);
        return JkUtilsPath.walk(root(), options)
                .filter(path -> matcher.matches(root().relativize(path)))
                .onClose(() -> rootHolder.closeIfNeeded());
    }

    /**
     * Same as {@link #files()} but returning paths relative to this tree root.
     */
    public List<Path> relativeFiles() {
        try(Stream<Path> stream = stream()) {
            return stream.filter(JkPathMatcher.noDirectory().asPredicate()).map(relativePathFunction()).collect(Collectors.toList());
        }
    }

    /**
     * Returns list of paths returned by {@link #stream(FileVisitOption...)} but excluding
     * directories from the result.
     */
    public List<Path> files() {
        try (Stream<Path> stream = stream()) {
            return stream.filter(JkPathMatcher.noDirectory().asPredicate()).collect(Collectors.toList());
        }
    }


    // ---------------------- Navigate -----------------------------------------------------------

    /**
     * Creates a {@link JkPathTree} having the specified relative path to this root as root directory.
     * Note that the returned tree has no filter even if this tree has one.
     */
    public JkPathTree goTo(String relativePath) {
        final Path path = root().resolve(relativePath).normalize();
        return JkPathTree.of(path);
    }

    /**
     * Returns path relative to this root of the specified relative path.
     */
    public Path get(String relativePath) {
        return root().resolve(relativePath);
    }

    // ----------------------- Write in ----------------------------------------------------------------


    /**
     * Short hand for <code>#merge(JkPathTree.of(dirToCopy, copyOptions)</code>.
     */
    public JkPathTree merge(Path dirToCopy, CopyOption... copyOptions) {
        return merge(JkPathTree.of(dirToCopy), copyOptions);
    }

    /**
     * Copies the content of the specified tree at the root of this one.
     * Specified tree to copy might not exists. The structure of the specified tree
     * is preserved.
     * Note that the the root of the specified tree is not part of the copied content.
     */
    public JkPathTree merge(JkPathTree tree, CopyOption... copyOptions) {
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
    public JkPathTree copyIn(Iterable<Path> files, StandardCopyOption ... copyOptions) {
        Iterable<Path> paths = JkUtilsPath.disambiguate(files);
        createIfNotExist();
        for (final Path file : paths) {
            JkUtilsPath.copy(file, root().resolve(file.getFileName()), copyOptions);
        }
        return this;
    }

    /**
     * Deletes each and every files in this tree except the root and files not matching this tree filter.
     */
    public JkPathTree deleteContent() {
        if (!Files.exists(root())) {
            return this;
        }
        JkUtilsPath.walkFileTree(root(), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return visitFile(file);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return visitDir(dir);
            }

            private FileVisitResult visitFile(Path path) {
                if (filter.matches(root().relativize(path))) {
                    JkUtilsPath.deleteFile(path);
                }
                return FileVisitResult.CONTINUE;
            }

            private FileVisitResult visitDir(Path path) {
                if (!JkUtilsPath.isSameFile(root(), path) && filter.matches(root().relativize(path))
                        && JkUtilsPath.listDirectChildren(path).isEmpty()) {
                    JkUtilsPath.deleteFile(path);
                }
                return FileVisitResult.CONTINUE;
            }

        });
        return this;
    }

    /**
     * Deletes root directory of this tree.
     */
    public JkPathTree deleteRoot() {
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
                JkUtilsPath.deleteFile(path);
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
    public JkPathTree zipTo(Path destination) {
        if (destination.getParent() != null) {
            JkUtilsPath.createDirectories(destination.getParent());
        }
        final Path zipRootEntry = JkUtilsPath.zipRoot(destination);
        try (FileSystem fileSystem = zipRootEntry.getFileSystem();  // zipRootEntry.getFileSystem() need to be called in
             Stream<Path> stream = this.stream()) {                 // try-resources-catch otherwise it fails

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
     * Copies files contained in this {@link JkPathTree} to the specified directory.
     */
    public int copyTo(Path destinationDir, CopyOption ... copyOptions) {
        if (!Files.exists(destinationDir)) {
            JkUtilsPath.createDirectories(destinationDir);
        }
        return JkUtilsPath.copyDirContent(root(), destinationDir, filter, copyOptions);
    }



    // ------------------------- Filter ----------------------------------------------

    /**
     * Creates a {@link JkPathTree} which is a copy of this {@link JkPathTree}
     * augmented with the specified {@link JkPathMatcher}
     */
    public JkPathTree andMatcher(PathMatcher pathMatcher) {
        return new JkPathTree(rootHolder, this.filter.and(pathMatcher));
    }

    /**
     * Creates a {@link JkPathTree} which is a copy of this {@link JkPathTree}
     * but with the specified {@link JkPathMatcher}
     */
    public JkPathTree withMatcher(JkPathMatcher pathMatcher) {
        return new JkPathTree(rootHolder, pathMatcher);
    }

    public JkPathTree andAccept(String... globPatterns) {
        return andAccept(Arrays.asList(globPatterns));
    }

    public JkPathTree andAccept(Iterable<String> globPatterns) {
        return andMatcher(JkPathMatcher.accept(this.root().getFileSystem(), globPatterns));
    }

    public JkPathTree andRefuse(Iterable<String> globPatterns) {
        return andMatcher(JkPathMatcher.refuse(this.root().getFileSystem(), globPatterns));
    }

    public JkPathTree andRefuse(String... globPatterns) {
        return andRefuse(Arrays.asList(globPatterns));
    }

    // ------------------------ Misc ---------------------------------------

    /**
     * Returns the file count contained in this {@link JkPathTree} to concurrence to specified max count.
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
    public JkPathTree resolve(Path path) {
        return new JkPathTree(rootHolder.resolve(path), this.filter);
    }


    /**
     * Returns a {@link JkPathTreeSet} containing this tree as its single
     * element.
     */
    public JkPathTreeSet asSet() {
        return JkPathTreeSet.of(this);
    }

    @Override
    public String toString() {
        return rootHolder + ":" + filter;
    }

    private static class RootHolder {
        final Path zipFile;
        Path dir;

        static RootHolder ofZip(Path zipFile) {
            JkUtilsAssert.notNull(zipFile, "zip archive file can't be null.");
            JkUtilsAssert.isTrue(!Files.exists(zipFile) || !Files.isDirectory(zipFile),
                    "Specified zip file " + zipFile + " can't be a directory");
            return new RootHolder(zipFile, null);
        }

        static RootHolder ofDir(Path dir) {
            JkUtilsAssert.notNull(dir, "Directory rootHolder tree can't be null.");
            JkUtilsAssert.isTrue(!Files.exists(dir) || Files.isDirectory(dir),
                    "Specified zip file " + dir + " must be a directory");
            return new RootHolder(null, dir);
        }

        private RootHolder(Path zipFile, Path root) {
            this.zipFile = zipFile;
            this.dir = root;
        }

        Path get() {
            if (isZip() && dir == null) {
                dir = JkUtilsPath.zipRoot(zipFile);
            }
            return dir;
        }

        void createIfNotExist() {
            if (zipFile == null) {
                if (!Files.exists(dir)) {
                    JkUtilsPath.createDirectories(dir);
                }
            } else {
                if (!Files.exists(zipFile)) {
                    JkUtilsPath.createDirectories(zipFile.getParent());
                    dir = JkUtilsPath.zipRoot(zipFile);
                } else if (dir == null) {
                    dir = JkUtilsPath.zipRoot(zipFile);
                } else if (dir.getFileSystem().isOpen()) {
                    JkUtilsPath.createDirectories(dir);
                } else {
                    Path zipRoot = JkUtilsPath.zipRoot(zipFile);
                    dir = zipRoot.getFileSystem().getPath(dir.toString());
                }
            }
        }

        boolean exists() {
            if (!isZip()) {
                return Files.exists(dir);
            }
            if (!Files.exists(zipFile)) {
                return false;
            }
            if (dir == null) {
                return true; // zip rootHolder always exists
            }
            if (dir.getFileSystem().isOpen()) {
                return Files.exists(dir);
            }
            Path zipRoot = JkUtilsPath.zipRoot(zipFile);
            dir = zipRoot.getFileSystem().getPath(dir.toString());
            return Files.exists(dir);
        }

        boolean isZip() {
            return zipFile != null;
        }

        void closeIfNeeded() {
            if (isZip() && dir != null) {
                try {
                    dir.getFileSystem().close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        RootHolder resolve(Path path) {
            if (isZip()) {
                return this;
            }
            return new RootHolder(null, path.resolve(dir));
        }

        Path rootFile() {
            if (isZip()) {
                return this.zipFile;
            }
            return this.dir;
        }

        @Override
        public String toString() {
            return isZip() ? zipFile.toString() : dir.toString();
        }
    }

}
