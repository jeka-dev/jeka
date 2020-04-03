package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.io.Closeable;
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
public final class JkPathTree implements Closeable {

    private static final JkPathMatcher ACCEPT_ALL = JkPathMatcher.of();

    /**
     * Creates a {@link JkPathTree} having the specified root directory.
     */
    public static JkPathTree of(Path rootDir) {
        return JkPathTree.of(rootDir, false);
    }

    /**
     * Creates a {@link JkPathTree} having the specified root directory.
     */
    public static JkPathTree ofZip(Path zipFile) {
        return JkPathTree.of(zipFile.toAbsolutePath(), true);
    }

    private final RootHolder rootHolder;

    private final JkPathMatcher matcher;

    private static JkPathTree of(Path rootDir, boolean zip) {
        return of(rootDir, ACCEPT_ALL, zip);
    }

    private static JkPathTree of(Path rootDirOrArchive, JkPathMatcher matcher, boolean zipFile) {
        final RootHolder rootHolder = zipFile ? RootHolder.ofZip(rootDirOrArchive) : RootHolder.ofDir(rootDirOrArchive);
        return new JkPathTree(rootHolder, matcher);
    }

    private JkPathTree(RootHolder rootHolder, JkPathMatcher matcher) {
        this.rootHolder = rootHolder;
        this.matcher = matcher;
    }

    /**
     * Returns the root directory. In case of zip archive it returns a directory entry
     * within the zip archive.
     */
    public Path getRoot() {
        return rootHolder.get();
    }

    /**
     * Returns root directory if this tree is a directory tree and returns a zip file if this
     * tree has been created from a zip file.
     */
    public Path getRootDirOrZipFile() {
        return  rootHolder.rootFile();
    }

    /**
     * Returns the filter defined on this {@link JkPathTree}, never <code>null</code>.
     */
    public JkPathMatcher getMatcher() {
        return matcher;
    }

    /**
     * Returns true if a matcher has explicitly been defined on this tree.
     */
    public boolean isDefineMatcher() {
        return this.matcher == ACCEPT_ALL;
    }

    // ------------------------------- functional ---------------------------------

    private Predicate<Path> excludeRootFilter() {
        return path -> !path.equals(getRoot());
    }

    private Function<Path, Path> relativePathFunction() {
        return path ->  getRoot().relativize(path);
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
        final JkPathMatcher matcher = JkPathMatcher.of(this.matcher);
        return JkUtilsPath.walk(getRoot(), options)
                .filter(path -> matcher.matches(getRoot().relativize(path)))
                .onClose(() -> rootHolder.closeIfNeeded());
    }

    /**
     * Same as {@link #getFiles()} but returning paths relative to this tree root.
     */
    public List<Path> getRelativeFiles() {
        try(Stream<Path> stream = stream()) {
            return stream.filter(JkPathMatcher.ofNoDirectory().toPredicate()).map(relativePathFunction()).collect(Collectors.toList());
        }
    }

    /**
     * Returns list of paths returned by {@link #stream(FileVisitOption...)} but excluding directories from the result.
     */
    public List<Path> getFiles() {
        try (Stream<Path> stream = stream()) {
            return stream.filter(JkPathMatcher.ofNoDirectory().toPredicate()).collect(Collectors.toList());
        }
    }


    // ---------------------- Navigate -----------------------------------------------------------

    /**
     * Creates a {@link JkPathTree} having the specified relative path to this root as getRoot directory.
     * Note that the returned tree has no filter even if this tree has one.
     */
    public JkPathTree goTo(String relativePath) {
        final Path path = getRoot().resolve(relativePath).normalize();
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalArgumentException(getRoot() + "/" + relativePath + " is not a directory");
        }
        RootHolder rootHolder = new RootHolder(this.rootHolder.zipFile, path);
        return new JkPathTree(rootHolder, this.matcher);
    }

    /**
     * Assuming the root folder is relative, this creates an identical {@link JkPathTree}
     * but having the root as :  [specified new root]/[former root]
     */
    public JkPathTree resolvedTo(Path newRoot) {
        final Path path = newRoot.resolve(getRoot()).normalize();
        RootHolder rootHolder = new RootHolder(this.rootHolder.zipFile, path);
        return new JkPathTree(rootHolder, this.matcher);
    }

    /**
     * Returns path relative to this root of the specified relative path.
     */
    public Path get(String relativePath) {
        return getRoot().resolve(relativePath);
    }

    // ----------------------- Write in ----------------------------------------------------------------


    /**
     * Copies the specified directory and its content at the root of this tree.
     */
    public JkPathTree importDir(Path dirToCopy, CopyOption... copyOptions) {
        return importTree(JkPathTree.of(dirToCopy), copyOptions);
    }

    /**
     * Copies the content of the specified tree at the root of this one.
     * Specified dir to copy to might not exists. The structure of the specified tree
     * is preserved.
     * Note that the the root of the specified tree is not part of the copied content.
     */
    public JkPathTree importTree(JkPathTree tree, CopyOption... copyOptions) {
        createIfNotExist();
        if (tree.exists()) {
            tree.stream().filter(excludeRootFilter()).forEach(path -> {
                Path target = this.getRoot().resolve(tree.getRoot().relativize(path).toString());
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
     * Copies the specified files at the root of this tree. The copy is not recursive.
     */
    public JkPathTree importFiles(Iterable<Path> files, StandardCopyOption ... copyOptions) {
        createIfNotExist();
        Iterable<Path> paths = JkUtilsPath.disambiguate(files);
        for (final Path file : paths) {
            JkUtilsPath.copy(file, getRoot().resolve(file.getFileName()), copyOptions);
        }
        return this;
    }

    /**
     * Copies the specified file at the specified path within this tree.
     */
    public JkPathTree importFile(Path src, String targetName, StandardCopyOption ... copyOptions) {
        createIfNotExist();
        Path parentTarget = getRoot().resolve(targetName).getParent();
        if (parentTarget != null && !Files.exists(parentTarget)) {
            JkUtilsPath.createDirectories(parentTarget);
        }
        JkUtilsPath.copy(src, getRoot().resolve(targetName), copyOptions);
        return this;
    }

    /**
     * Deletes each and every files in this tree except the root and files not matching this tree filter.
     */
    public JkPathTree deleteContent() {
        if (!Files.exists(getRoot())) {
            return this;
        }
        JkUtilsPath.walkFileTree(getRoot(), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                return visitFile(file);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return visitDir(dir);
            }

            private FileVisitResult visitFile(Path path) {
                if (matcher.matches(getRoot().relativize(path))) {
                    JkUtilsPath.deleteFile(path);
                }
                return FileVisitResult.CONTINUE;
            }

            private FileVisitResult visitDir(Path path) {
                if (!JkUtilsPath.isSameFile(getRoot(), path) && matcher.matches(getRoot().relativize(path))
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
        JkUtilsPath.walkFileTree(getRoot(), new SimpleFileVisitor<Path>() {

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
        try (Stream<Path> stream = this.stream()) {
            stream.filter(excludeRootFilter()).forEach(path -> {
                Path zipEntry = zipRootEntry.resolve(getRoot().relativize(path).toString());
                if (!Files.exists(zipEntry) || !Files.isDirectory(zipEntry)) {
                    JkUtilsPath.createDirectories(zipEntry.getParent());
                    JkUtilsPath.copy(path, zipEntry, StandardCopyOption.REPLACE_EXISTING);
                }
            });
            zipRootEntry.getFileSystem().close();
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
        return JkUtilsPath.copyDirContent(getRoot(), destinationDir, matcher, copyOptions);
    }

    /**
     * Copies a single file contained in this {@link JkPathTree} to the specified directory. File name remains the same.
     * @param sourcePath The relative path of the source file from this tree root.
     */
    public void copyFile(String sourcePath, Path destinationDir, CopyOption ... copyOptions) {
        if (!Files.exists(destinationDir)) {
            JkUtilsPath.createDirectories(destinationDir);
        }
        Path source = get(sourcePath);
        Path dest = destinationDir.getFileSystem().getPath(destinationDir.toString() + "/" + source.getFileName());
        JkUtilsPath.copy(source, dest, copyOptions);
    }

    // ------------------------- Filter ----------------------------------------------

    /**
     * Creates a copy of this {@link JkPathTree} augmented with the specified {@link JkPathMatcher}
     */
    public JkPathTree andMatcher(PathMatcher pathMatcher) {
        return new JkPathTree(rootHolder, this.matcher.and(pathMatcher));
    }

    /**
     * Creates a {@link JkPathTree} which is a copy of this {@link JkPathTree}
     * but the matcher is replaced with the specified one.
     */
    public JkPathTree withMatcher(JkPathMatcher pathMatcher) {
        return new JkPathTree(rootHolder, pathMatcher);
    }

    /**
     * Creates a copy of this {@link JkPathTree} augmented with the specified pattern matcher.
     */
    public JkPathTree andMatching(boolean positive, String... globPatterns) {
        return andMatching(positive, Arrays.asList(globPatterns));
    }

    /**
     * Shorthand to <code>andMatching(true, globPatterns...)</code>.
     */
    public JkPathTree andMatching(String... globPatterns) {
        return andMatching(true, globPatterns);
    }

    /**
     * Creates a copy of this {@link JkPathTree} augmented with the specified pattern matcher.
     */
    public JkPathTree andMatching(boolean positive, Iterable<String> globPatterns) {
        return andMatcher(JkPathMatcher.of(positive, this.getRoot().getFileSystem(), globPatterns));
    }

    // ------------------------ Misc ---------------------------------------

    /**
     * Returns the file count contained in this {@link JkPathTree} to concurrence to specified max count.
     * If the effective count is greater than max count, this method returns <code>max count + 1</code>.
     * This method is designed to stop file traversal as soon as count is greater than max.
     */
    public int count(int max, boolean includeDirectories) {
        if (!exists()) {
            return 0;
        }
        return JkUtilsPath.childrenCount(getRoot(), max, includeDirectories, this.matcher);
    }

    /**
     * If the root of this tree is absolute then this method returns this tree.
     * If the root of this tree is relative then this method returns a tree having a getRoot
     * resolved from the specified path to this root.
     */
    public JkPathTree resolve(Path path) {
        return new JkPathTree(rootHolder.resolve(path), this.matcher);
    }


    /**
     * Returns a {@link JkPathTreeSet} containing this tree as its single
     * element.
     */
    public JkPathTreeSet toSet() {
        return JkPathTreeSet.of(this);
    }

    @Override
    public String toString() {
        return rootHolder + ":" + matcher;
    }

    /**
     * This method close the underlying file system. It is only significant for zip trees.
     * @throws IOException
     */
    @Override
    public void close()  {
        if (this.rootHolder.isZip()) {
            JkUtilsIO.closeQuietly(rootHolder.get().getFileSystem());
        }
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
