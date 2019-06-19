package dev.jeka.core.api.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipError;

/**
 * Utility class providing convenient methods to deal with {@link java.nio.file.Path}.
 * Mainly for wrapping checked exceptions and handle safely null parameters.
 */
public final class JkUtilsPath {

    private JkUtilsPath() {
        // Do nothing
    }

    /**
     * As {@link Path} implements  {@link Iterable<Path>} it is not obvious if an {@link Iterable} of {@link Path} was
     * intended to be a list of {@link Path} or a single {@link Path}.
     * This methods helps by returning a list of Path containing either a single element if the argument was an instance
     * of {@link Path} nor a list of elements contained in the iterable argument.
     *
     * @param paths
     * @return
     */
    public static List<Path> disambiguate(Iterable<Path> paths) {
        if (paths instanceof Path) {
            Path path = (Path) paths;
            List<Path> result = new LinkedList();
            result.add(path);
            return result;
        }
        LinkedList result = new LinkedList();
        paths.forEach(path -> result.add(path));
        return result;
    }

    /**
     * Returns the first specified path that exist. Returns null if none.
     */
    public static Path firstExisting(Path... paths) {
        for (int i=0; i < paths.length; i++) {
            if (paths[i] != null && Files.exists(paths[i])) {
                return paths[i];
            }
        }
        return null;
    }

    public static Path get(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Url " + url + " can not be turned to an URI", e);
        }
    }

    public static Path zipRoot(Path zipFile) {
        final URI uri = URI.create("jar:" + zipFile.toUri());
        final Map<String, String> env = JkUtilsIterable.mapOf("create", "true");
        FileSystem fileSystem;
        try {
            try {
                fileSystem = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                fileSystem = FileSystems.newFileSystem(uri, env);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch(ZipError e) {
            throw JkUtilsThrowable.unchecked(e, "Error while opening zip archive " + zipFile);
        }
        return fileSystem.getPath("/");
    }

    public static List<File> toFiles(Collection<Path> paths) {
        final List<File> result = new LinkedList<>();
        for (final Path path : paths) {
            result.add(path.toFile());
        }
        return result;
    }

    public static File[] toFiles(Path ... paths) {
        final File[] result = new File[paths.length];
        for (int i = 0; i<paths.length; i++) {
            result[i] = paths[i].toFile();
        }
        return result;
    }

    public static Path[] toPaths(File ... files) {
        final Path[] result = new Path[files.length];
        for (int i = 0; i<files.length; i++) {
            result[i] = files[i].toPath();
        }
        return result;
    }

    public static List<Path> toPaths(Iterable<File> files) {
        final List<Path> result = new LinkedList<>();
        for (final File file : files) {
            result.add(file.toPath());
        }
        return result;
    }

    /**
     * Delegates to Files{@link #isSameFile(Path, Path)}
     */
    public static boolean isSameFile(Path path1, Path path2) {
        try {
            return Files.isSameFile(path1, path2);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to {@link Files#createTempFile(String, String, FileAttribute[])}
     */
    public static Path createTempFile(String prefix, String extension, FileAttribute ... fileAttributes) {
        try {
            return Files.createTempFile(prefix, extension, fileAttributes);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to {@link Files#readAllLines(Path)}
     */
    public static List<String> readAllLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to {@link Files#readAllBytes(Path)} (Path)}
     */
    public static byte[] readAllBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to Files{@link #deleteFile(Path)}
     */
    public static void deleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to Files{@link #deleteIfExists(Path)}
     */
    public static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to {@link Files#createFile(Path, FileAttribute[])}s
     */
    public static void createFile(Path path, FileAttribute<?>... attrs) {
        try {
            Files.createFile(path, attrs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to {@link Files#createFile(Path, FileAttribute[])} but checking first
     * if the specified file does not already exist. If so, do nothing, else creates
     * parent directories if needed prior creating the file.
     */
    public static void createFileSafely(Path path, FileAttribute<?>... attrs) {
        if (Files.exists(path)) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.createFile(path, attrs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to Files{@link #write(Path, byte[], OpenOption...)}
     */
    public static void write(Path path, byte[] bytes, OpenOption ... options) {
        try {
            Files.write(path, bytes, options);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to Files{@link #createTempDirectory(String, FileAttribute[])}
     */
    public static Path createTempDirectory(String prefix, FileAttribute ... fileAttributes) {
        try {
            return Files.createTempDirectory(prefix, fileAttributes);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static DirectoryStream<Path> newDirectoryStream(Path root, DirectoryStream.Filter<Path> filter) {
        try {
            return Files.newDirectoryStream(root, filter);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> listDirectChildren(Path path) {
        try (Stream stream = Files.list(path)) {
            return (List<Path>) stream.collect(Collectors.toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to {@link Files#createDirectories(Path, FileAttribute[])} wrapping checked exception.
     */
    public static void createDirectories(Path path, FileAttribute<?>... attrs) {
        try {
            Files.createDirectories(path, attrs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Delegates to {@link Files#copy(Path, Path, CopyOption...)} wrapping checked exception.
     */
    public static void copy(Path source, Path target, CopyOption ...copyOptions) {
        try {
            Files.copy(source, target, copyOptions);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Get the url to the specified path.
     */
    public static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Stream<Path> walk(Path path, FileVisitOption ...options) {
        try {
            return Files.walk(path, options);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Stream<Path> walk(Path path, int deep, FileVisitOption ...options) {
        try {
            return Files.walk(path, deep, options);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void walkFileTree(Path path, FileVisitor<Path> visitor) {
        try {
            Files.walkFileTree(path, visitor);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copies the content of the source directory into the target directory. The root of source directory is
     * not created as an entry of the target directory.
     * @return the copied file count.
     */
    public static int copyDirContent(Path sourceDir, Path targetDir, PathMatcher pathMatcher, CopyOption ... copyOptions)  {
        final CopyDirVisitor visitor = new CopyDirVisitor(sourceDir, targetDir, pathMatcher, copyOptions);
        createDirectories(targetDir);
        walkFileTree(sourceDir, visitor);
        return visitor.count;
    }

    /**
     * Returns the file count contained in the specified directory (recursively) to concurrence to specified max count.
     * If the effective count is greater than max count, returns <code>max + 1</code>.
     * This method is designed to stop file traversal as soon as count is greater than max.
     */
    public static int childrenCount(Path dir, int max, boolean includeDirectories)  {
        final CountFileVisitor visitor = new CountFileVisitor(dir, max, includeDirectories);
        walkFileTree(dir, visitor);
        return visitor.count;
    }

    private static class CopyDirVisitor extends SimpleFileVisitor<Path> {

        CopyDirVisitor(Path fromDir, Path toDir, PathMatcher pathMatcher, CopyOption ... options) {
            this.fromDir = fromDir;
            this.toDir = toDir;
            this.options = options;
            this.pathMatcher = pathMatcher;
        }

        private final Path fromDir;
        private final Path toDir;
        private final PathMatcher pathMatcher;
        private final CopyOption[] options;
        int count;


        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            final Path sourceRelativePath = fromDir.relativize(dir);
            if (!pathMatcher.matches(sourceRelativePath)) {
                return FileVisitResult.CONTINUE;
            }
            final Path relativePath = toDir.getFileSystem().getPath(toDir.toString(), sourceRelativePath.toString());
            Files.createDirectories(relativePath);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path relativePath = fromDir.relativize(file);
            if (!pathMatcher.matches(relativePath)) {
                return FileVisitResult.CONTINUE;
            }
            final Path target = toDir.getFileSystem().getPath(toDir.toString(), relativePath.toString()); // necessary to deal with both regular file system and zip
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(file, target , options);
            count ++;
            return FileVisitResult.CONTINUE;
        }
    }

    private static class CountFileVisitor extends SimpleFileVisitor<Path> {

        private final Path fromPath;
        private final boolean includeDirectories;
        private final int countMax;
        private int count;


        CountFileVisitor(Path fromPath, int countMax , boolean includeDirectories) {
            this.fromPath = fromPath;
            this.countMax = countMax;
            this.includeDirectories = includeDirectories;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (includeDirectories && !fromPath.equals(dir)) {
                count ++;
            }
            if (count > countMax) {
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            count ++;
            if (count > countMax) {
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
    }

}
