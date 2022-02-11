package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsPath;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
public class JkPathTree<T extends JkPathTree> {

    protected final Supplier<Path> rootSupplier;

    private final JkPathMatcher matcher;

    protected JkPathTree(Supplier<Path> rootSupplier, JkPathMatcher matcher) {
        this.rootSupplier = rootSupplier;
        this.matcher = matcher;
    }

    /**
     * Creates a {@link JkPathTree} having the specified root directory.
     */
    public static JkPathTree of(Path rootDir) {
        return new JkPathTree(() -> rootDir, JkPathMatcher.ACCEPT_ALL);
    }

    /**
     * @see #of(Path)
     */
    public static JkPathTree of(String rootDir) {
        return of(Paths.get(rootDir));
    }

    protected T newInstance(Supplier<Path> pathSupplier, JkPathMatcher pathMatcher) {
        return (T) new JkPathTree(pathSupplier, pathMatcher);
    }

    protected T withRoot(Path newRoot) {
        return newInstance(() -> newRoot, this.matcher);
    }

    /**
     * Returns the root directory. In case of zip archive it returns a directory entry within the zip archive.
     */
    public Path getRoot() {
        return rootSupplier.get();
    }

    /**
     * Returns the filter defined on this {@link JkPathTree}, never <code>null</code>.
     */
    public JkPathMatcher getMatcher() {
        return matcher;
    }

    public boolean  hasFilter() {
        return matcher != JkPathMatcher.ACCEPT_ALL;
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
        return Files.exists(getRoot());
    }

    /**
     * Creates root directory if not exists.
     */
    public T createIfNotExist() {
        if (!Files.exists(getRoot())) {
            JkUtilsPath.createDirectories(getRoot());
        }
        return (T) this;
    }


    // ----------------------- iterate over files ---------------------------------------------------

    /**
     * Returns a path stream of all files of this tree. Returned paths are resolved against this tree root.
     * This means that if this tree root is absolute then streamed paths are absolute as well.
     * Note that the root folder is included in the returned stream.
     * If this method is called for a zip tree instance, then it should be closed or called within
     * <i>try-with-resources</i> statement in order to avoid resource leaks.
     */
    public Stream stream(FileVisitOption ...options) {
        if(!exists()) {
            return new LinkedList<Path>().stream();
        }
        final JkPathMatcher matcher = JkPathMatcher.of(this.matcher);
        Path root = getRoot().toString().equals("") ? Paths.get(".") : getRoot();
        return JkUtilsPath.walk(root, options)
                .filter(path -> matcher.matches(root.relativize(path)));
    }

    /**
     * Same as {@link #getFiles()} but returning paths relative to this tree root.
     */
    public List<Path> getRelativeFiles() {
        try(Stream<Path> stream = stream()) {
            return stream
                    .filter(JkPathMatcher.ofNoDirectory().toPredicate())
                    .map(relativePathFunction())
                    .collect(Collectors.toList());
        }
    }

    /**
     * Returns list of paths returned by {@link #stream(FileVisitOption...)} but excluding directories from the result.
     */
    public List<Path> getFiles() {
        try (Stream<Path> stream = stream()) {
            return stream
                    .filter(JkPathMatcher.ofNoDirectory().toPredicate())
                    .collect(Collectors.toList());
        }
    }


    // ---------------------- Navigate -----------------------------------------------------------



    /**
     * Creates a {@link JkPathTree} having the specified relative path to this root as getRoot directory.
     * Note that the returned tree has no filter even if this tree has one.
     */
    public T goTo(String relativePath) {
        final Path path = getRoot().resolve(relativePath).normalize();
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalArgumentException(getRoot() + "/" + relativePath + " is not a directory");
        }
        return withRoot(path);
    }

    /**
     * Assuming the root folder is relative, this creates an identical {@link JkPathTree}
     * but having the root as :  [specified new root]/[former root]
     */
    public T resolvedTo(Path newRoot) {
        final Path path = newRoot.resolve(getRoot()).normalize();
        return withRoot(path);
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
    public T importDir(Path dirToCopy, CopyOption... copyOptions) {
        return importTree(JkPathTree.of(dirToCopy), copyOptions);
    }

    /**
     * Copies the content of the specified tree at the root of this one.
     * Specified dir to copy to might not exists. The structure of the specified tree
     * is preserved.
     * Note that the the root of the specified tree is not part of the copied content.
     */
    public T importTree(JkPathTree tree, CopyOption... copyOptions) {
        createIfNotExist();
        if (tree.exists()) {
            tree.stream()
                    .filter(excludeRootFilter())
                    .forEach(object -> {
                        Path path = (Path) object;
                        Path target = this.getRoot().resolve(tree.getRoot().relativize(path).toString());
                        if (Files.isDirectory(path)) {
                            JkUtilsPath.createDirectories(target);
                        } else {
                            JkUtilsPath.copy(path, target, copyOptions);
                        }
                    });
        }
        return (T) this;
    }

    /**
     * Copies the specified files at the root of this tree. The copy is not recursive.
     */
    public T importFiles(Iterable<Path> files, StandardCopyOption ... copyOptions) {
        createIfNotExist();
        Iterable<Path> paths = JkUtilsPath.disambiguate(files);
        for (final Path file : paths) {
            JkUtilsPath.copy(file, getRoot().resolve(file.getFileName()), copyOptions);
        }
        return (T) this;
    }

    /**
     * Copies the specified file at the specified path within this tree.
     */
    public T importFile(Path src, String targetName, StandardCopyOption ... copyOptions) {
        createIfNotExist();
        Path parentTarget = getRoot().resolve(targetName).getParent();
        if (parentTarget != null && !Files.exists(parentTarget)) {
            JkUtilsPath.createDirectories(parentTarget);
        }
        JkUtilsPath.copy(src, getRoot().resolve(targetName), copyOptions);
        return (T) this;
    }

    /**
     * Deletes each and every files in this tree except the root and files not matching this tree filter.
     */
    public T deleteContent() {
        if (!Files.exists(getRoot())) {
            return (T) this;
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
        return (T) this;
    }

    /**
     * Deletes root directory of this tree.
     */
    public T deleteRoot() {
        withMatcher(JkPathMatcher.ACCEPT_ALL).deleteContent();
        JkUtilsPath.deleteFile(getRoot());
        return (T) this;
    }

    // ----------------------- Write out ----------------------------------------------------------------

    /**
     * Zips the content of this tree to the specified destination file. If the specified destination file
     * already exists, the content of this tree is appended to the existing archive, overriding existing entries within the archive.
     */
    public T zipTo(Path destination) {
        if (destination.getParent() != null) {
            JkUtilsPath.createDirectories(destination.getParent());
        }
        try (Stream<Path> stream = this.stream();
             JkUtilsPath.JkZipRoot zipRoot = JkUtilsPath.zipRoot(destination)) {

            stream.filter(excludeRootFilter()).forEach(path -> {
                Path zipEntry = zipRoot.get().resolve(getRoot().relativize(path).toString());
                if (!Files.exists(zipEntry) || !Files.isDirectory(zipEntry)) {
                    JkUtilsPath.createDirectories(zipEntry.getParent());
                    JkUtilsPath.copy(path, zipEntry, StandardCopyOption.REPLACE_EXISTING);
                }
            });
        }
        return (T) this;
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
    public T andMatcher(PathMatcher pathMatcher) {
        return withMatcher(this.matcher.and(pathMatcher));
    }

    /**
     * Creates a {@link JkPathTree} which is a copy of this {@link JkPathTree}
     * but the matcher is replaced with the specified one.
     */
    public T withMatcher(JkPathMatcher pathMatcher) {
        return newInstance(rootSupplier, pathMatcher);
    }

    /**
     * Creates a copy of this {@link JkPathTree} augmented with the specified pattern matcher.
     */
    public T andMatching(boolean positive, String... globPatterns) {
        return andMatching(positive, Arrays.asList(globPatterns));
    }

    /**
     * Shorthand to <code>andMatching(true, globPatterns...)</code>.
     */
    public T andMatching(String... globPatterns) {
        return andMatching(true, globPatterns);
    }

    /**
     * Creates a copy of this {@link JkPathTree} augmented with the specified pattern matcher.
     */
    public T andMatching(boolean positive, Iterable<String> globPatterns) {
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

    public boolean containFiles() {
        return count(1, false) > 0;
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
        return this.hasFilter() ? rootSupplier.get().toString() + ":" + matcher : rootSupplier.get().toString();
    }


}
