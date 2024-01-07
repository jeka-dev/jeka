package dev.jeka.core.api.file;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Provides a view on files and sub-folders contained in a given directory or zip file. A
 * <code>JkPathTree</code> may have some include/exclude filters to include only
 * or exclude specified files.<br/>
 * When speaking about files contained in a {@link JkAbstractPathTree}, we mean all
 * files contained in its root directory or subdirectories, matching positively
 * the filter defined on it.
 *
 * @param <T> for self returning keeping the actual type.
 *
 * @author Jerome Angibaud
 */
public class JkAbstractPathTree<T extends JkAbstractPathTree> {

    protected final Supplier<Path> rootSupplier;

    private final JkPathMatcher matcher;

    protected JkAbstractPathTree(Supplier<Path> rootSupplier, JkPathMatcher matcher) {
        this.rootSupplier = rootSupplier;
        this.matcher = matcher;
    }

    /**
     * Creates a {@link JkAbstractPathTree} having the specified root directory.
     */
    public static JkAbstractPathTree of(Path rootDir) {
        return new JkAbstractPathTree(() -> rootDir, JkPathMatcher.ACCEPT_ALL);
    }

    /**
     * @see #of(Path)
     */
    public static JkAbstractPathTree of(String rootDir) {
        return of(Paths.get(rootDir));
    }

    protected T newInstance(Supplier<Path> pathSupplier, JkPathMatcher pathMatcher) {
        return (T) new JkAbstractPathTree(pathSupplier, pathMatcher);
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
     * Returns the filter defined on this {@link JkAbstractPathTree}, never <code>null</code>.
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
    public Stream<Path> stream(FileVisitOption ...options) {
        if(!exists()) {
            return Stream.of();
        }
        final JkPathMatcher matcher = JkPathMatcher.of(this.matcher);
        Path root = getRoot().toString().isEmpty() ? Paths.get(".") : getRoot();
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
     * Creates a {@link JkAbstractPathTree} having the specified relative path to this root as getRoot directory.
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
     * Assuming the root folder is relative, this creates an identical {@link JkAbstractPathTree}
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
        return importTree(JkAbstractPathTree.of(dirToCopy), copyOptions);
    }

    /**
     * Copies the content of the specified tree at the root of this one.
     * Specified dir to copy to might not exist. The structure of the specified tree
     * is preserved.
     * Note that the root of the specified tree is not part of the copied content.
     */
    public T importTree(JkAbstractPathTree tree, CopyOption... copyOptions) {
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
     * Deletes each and every file in this tree except the root and files not matching this tree filter.
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
     * Deletes the root directory of the path tree. This method also deletes all the content inside the root directory.
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
     * Copies files contained in this {@link JkAbstractPathTree} to the specified directory.
     */
    public int copyTo(Path destinationDir, CopyOption ... copyOptions) {
        if (!Files.exists(destinationDir)) {
            JkUtilsPath.createDirectories(destinationDir);
        }
        return JkUtilsPath.copyDirContent(getRoot(), destinationDir, matcher, copyOptions);
    }

    /**
     * Copies a single file contained in this {@link JkAbstractPathTree} to the specified directory. File name remains the same.
     * @param sourcePath The relative path of the source file from this tree root.
     */
    public void copyFile(String sourcePath, Path destinationDir, CopyOption ... copyOptions) {
        if (!Files.exists(destinationDir)) {
            JkUtilsPath.createDirectories(destinationDir);
        }
        Path source = get(sourcePath);
        Path dest = destinationDir.getFileSystem().getPath(destinationDir + "/" + source.getFileName());
        JkUtilsPath.copy(source, dest, copyOptions);
    }

    // ------------------------- Filter ----------------------------------------------

    /**
     * Creates a copy of this {@link JkAbstractPathTree} augmented with the specified {@link JkPathMatcher}
     */
    public T andMatcher(PathMatcher pathMatcher) {
        return withMatcher(this.matcher.and(pathMatcher));
    }

    /**
     * Creates a {@link JkAbstractPathTree} which is a copy of this {@link JkAbstractPathTree}
     * but the matcher is replaced with the specified one.
     */
    public T withMatcher(JkPathMatcher pathMatcher) {
        return newInstance(rootSupplier, pathMatcher);
    }

    /**
     * Creates a copy of this {@link JkAbstractPathTree} augmented with the specified pattern matcher.
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
     * Creates a copy of this {@link JkAbstractPathTree} augmented with the specified pattern matcher.
     */
    public T andMatching(boolean positive, Iterable<String> globPatterns) {
        return andMatcher(JkPathMatcher.of(positive, this.getRoot().getFileSystem(), globPatterns));
    }

    // ------------------------ Misc ---------------------------------------

    /**
     * Returns the file count contained in this {@link JkAbstractPathTree} to concurrence to specified max count.
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


    @Override
    public String toString() {
        return this.hasFilter() ? rootSupplier.get().toString() + ":" + matcher : rootSupplier.get().toString();
    }

    /**
     * Watches file and directory changes involved in this path tree. This method blocks for the specified millis
     * time to capture filesystem changes and pass them to the specified consumer.
     * @param millis Time to wait for capturing file changes
     * @param run Semaphore, this methods loop until this values false
     * @param fileChangeConsumer The consumer to be invoked on file changes
     */
    public void watch(long millis, AtomicBoolean run, Consumer<List<FileChange>> fileChangeConsumer)  {
        try(WatchService watchService = FileSystems.getDefault().newWatchService()) {
            List<WatchKey> watchKeys = getWatchKeys(watchService);
            while (run.get()) {
                JkUtilsSystem.sleep(millis);
                List<FileChange> fileChanges = getFileChanges(watchService, watchKeys);
                if (!fileChanges.isEmpty()) {
                    JkLog.trace("File change detected : " + fileChanges);
                    fileChangeConsumer.accept(fileChanges);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Same as {@link #watch(long, AtomicBoolean, Consumer)} but just triggering a consumer on file changes.
     */
    public void watch(long millis, AtomicBoolean run, Runnable runnable) {
        watch(millis, run, cf -> runnable.run());
    }

    /**
     * Same as {@link #watch(long, AtomicBoolean, Runnable)} but running indefinitely.
     */
    public void watch(long millis, Runnable runnable) {
        watch(millis, new AtomicBoolean(true), cf -> runnable.run());
    }

    List<FileChange> getFileChanges(WatchService watchService, List<WatchKey> watchKeys) {
        List<FileChange> fileChanges = new LinkedList<>();
        for (ListIterator<WatchKey> it = watchKeys.listIterator(); it.hasNext();) {
            WatchKey watchKey = it.next();
            if (!watchKey.isValid()) {
                it.remove();
                continue;
            }
            for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
                Path dir = (Path) watchKey.watchable();
                Path filePath = dir.resolve((Path) watchEvent.context());
                if (Files.isDirectory(filePath) && ENTRY_CREATE.equals(watchEvent.kind())) {
                    it.add(
                            JkUtilsPath.register(filePath, watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
                    JkLog.trace("Watch directory " + filePath);
                }
                Path relPath = this.getRoot().relativize(filePath);

                // We don't want to add dir change (only file change)
                // if a file is deleted, it must be notified. We have no mean to check if it is a dir or file
                // when deleted so we notify for all delete event.
                if (this.matcher.matches(relPath) && (!Files.exists(filePath) || !Files.isDirectory(filePath))) {
                    FileChange fileChange = new FileChange(watchEvent.kind(), filePath);
                    fileChanges.add(fileChange);
                }
            }
        }
        return fileChanges;
    }

    List<WatchKey> getWatchKeys(WatchService watchService) {
        List<WatchKey> watchKeys = JkUtilsPath.walk(this.getRoot(), FileVisitOption.FOLLOW_LINKS)
                .filter(Files::isDirectory)
                .filter(path -> this.matcher.matches(path))
                .map(dir -> JkUtilsPath.register(dir, watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY))
                .collect(Collectors.toCollection(() -> new LinkedList<>()));
        return watchKeys;
    }

    /**
     * Computes the checksum of this tree content. This includes file contents and file structure.
     * This means that changing a file name without but keeping its content unchanged will
     * produce distinct checksum.
     * @param algorithm algorithm used to produce checksum, such as md5 or sha256.
     */
    public String checksum(String algorithm) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        updateDigest(digest);
        return Base64.getEncoder().encodeToString(digest.digest());
    }

    public static class FileChange {

        private final WatchEvent.Kind kind;

        private final Path path;

        FileChange(WatchEvent.Kind kind, Path path) {
            this.kind = kind;
            this.path = path;
        }

        public WatchEvent.Kind getKind() {
            return kind;
        }

        public Path getPath() {
            return path;
        }

        @Override
        public String toString() {
            return kind.name() + " : " + path;
        }
    }

    void updateDigest(MessageDigest messageDigest) {
        for (Path relPath : this.getRelativeFiles()) {
            messageDigest.update(relPath.toString().getBytes(StandardCharsets.UTF_8));
            Path path = this.getRoot().resolve(relPath);
            JkPathFile.of(path).updateDigest(messageDigest);
        }
    }

}
