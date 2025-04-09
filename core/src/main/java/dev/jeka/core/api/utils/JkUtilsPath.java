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

package dev.jeka.core.api.utils;

import dev.jeka.core.api.system.JkLog;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class providing convenient methods to deal with {@link java.nio.file.Path}.
 * Mainly for wrapping checked exceptions and handle safely null parameters.
 */
public final class JkUtilsPath {

    private JkUtilsPath() {
        // Do nothing
    }

    public static String friendlyName(Path path) {
        if (path.isAbsolute()) {
            Path relPath = JkUtilsPath.relativizeFromWorkingDir(path);
            return friendlyName(relPath);
        }
        if (path.getFileName().toString().equals("") || path.getFileName().toString().equals(".")) {
            return path.toAbsolutePath().getFileName().toString();
        }
        return path.toString();
    }

    /**
     * As {@link Path} implements  {@link Iterable<Path>} it is not obvious if an {@link Iterable} of {@link Path} was
     * intended to be a list of {@link Path} or a single {@link Path}.
     * This method helps by returning a list of Path containing either a single element if the argument was an instance
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

    public static JkZipRoot zipRoot(Path zipFile) {
        final URI uri = URI.create("jar:" + zipFile.toUri());
        final Map<String, String> env = JkUtilsIterable.mapOf("create", "true");
        FileSystem fileSystem;
        try {
            try {
                fileSystem = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                fileSystem = FileSystems.newFileSystem(uri, env);  //NOSONAR : this resource will be closed along JkZipRoot
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch(Throwable e) {
            throw JkUtilsThrowable.unchecked(e, "Error while opening zip archive " + zipFile);
        }
        return new JkZipRoot(zipFile, fileSystem.getPath("/"));
    }

    /**
     * @see Files#move(Path, Path, CopyOption...)
     */
    public static void move(Path from, Path to, StandardCopyOption ... options) {
        try {
            Files.move(from, to, options);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the content of the specified property file as a
     * {@link Properties} object.
     */
    public static Properties readPropertyFile(Path propertyFile) {
        final Properties props = new Properties();
        try (InputStream fileInputStream = Files.newInputStream(propertyFile)){
            props.load(fileInputStream);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        return props;
    }

    /**
     * Get the url to the specified file.
     */
    public static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     *  A container object representing both a zip file and its content. The content is seen as a regular
     *  <code>Path</code> representing the content root.
     */
    public static class JkZipRoot implements Supplier<Path>, Closeable {

        private final Path contentRoot;

        private final Path zipFile;

        private JkZipRoot(Path zipFile, Path contentRoot) {
            this.zipFile = zipFile;
            this.contentRoot = contentRoot;
        }

        public Path get() {
            return contentRoot;
        }

        public Path getZipFile() {
            return zipFile;
        }

        public JkZipRoot withRootInsideZip(Path zipEntry) {
            return new JkZipRoot(zipFile, zipEntry);
        }

        @Override
        public void close() {
            JkUtilsIO.closeQuietly(contentRoot.getFileSystem());
        }

        @Override
        public String toString() {
            return contentRoot.toString();
        }

    }

    public static InputStream newInputStream(Path path, OpenOption... openOptions) {
        try {
            return Files.newInputStream(path, openOptions);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        JkUtilsAssert.argument(Files.exists(path1), "path1 does not exist");
        JkUtilsAssert.argument(Files.exists(path2), "path2 does not exist");
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

    public static Stream<String> lines(Path file, Charset charset) {
        try {
            return Files.lines(file, charset);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Delegates to {@link Files#readAllBytes(Path)} (Path)
     */
    public static byte[] readAllBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void deleteQuietly(Path path, boolean ignoreDeleteError) {
        if (!Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            listDirectChildren(path).forEach(sub -> deleteQuietly(sub, ignoreDeleteError));
        }
        try {
            Files.delete(path);
        } catch (IOException e) {
            if (ignoreDeleteError) {
                JkLog.debug("Cannot delete " + path + ". Cause : " + e.getMessage());
            } else {
                throw new UncheckedIOException(e);
            }

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
     * Delegates to Files{@link #deleteIfExists(Path)}
     */
    public static void deleteIfExistsSafely(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (final IOException e) {
            path.toFile().deleteOnExit();
            JkLog.verbose("Cannot delete %s. Cause : %s", path, e.getMessage());
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
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.createFile(path, attrs);
        } catch (FileAlreadyExistsException e) {
            JkLog.warn("File {} already exists", path); // Oddly happens time to time
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
     * Retrieves the path to a resource by its name from the same class loader as the specified class.
     *
     * @param clazz the class used to retrieve the resource's class loader
     * @param resourceName the name of the resource to locate
     * @return the Path object corresponding to the resource location
     * @throws IllegalArgumentException if the resource cannot be located
     * @throws RuntimeException if there is an error converting the resource URL to a Path
     */
    public static Path getResourceAsPath(Class<?> clazz, String resourceName) {
        URL url = clazz.getResource(resourceName);
        JkUtilsAssert.argument(url != null, "Resource not found: " + resourceName);
        try {
            return Paths.get(url.toURI().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot read resource " + resourceName, e);
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
    public static int childrenCount(Path dir, int max, boolean includeDirectories, PathMatcher pathMatcher)  {
        final CountFileVisitor visitor = new CountFileVisitor(dir, max, includeDirectories, pathMatcher);
        walkFileTree(dir, visitor);
        return visitor.count;
    }

    /**
     * Returns the relative path to working dir of the specified path. Returns the specified path itself,
     * if this one is already relative.
     */
    public static Path relativizeFromWorkingDir(Path path) {
        return path.isAbsolute() ? Paths.get("").toAbsolutePath().relativize(path) : path;
    }

    /**
     * Returns the relative path to working dir of the specified path. Returns the specified path itself,
     * if this one is already relative.
     */
    public static Path relativizeFromDirIfAbsolute(Path referent, Path pathToRelativize) {
        return pathToRelativize.isAbsolute() ? Paths.get("").toAbsolutePath().relativize(pathToRelativize)
                : pathToRelativize;
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
        private final PathMatcher pathMatcher;


        CountFileVisitor(Path fromPath, int countMax , boolean includeDirectories, PathMatcher pathMatcher) {
            this.fromPath = fromPath;
            this.countMax = countMax;
            this.includeDirectories = includeDirectories;
            this.pathMatcher = pathMatcher;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            final Path sourceRelativePath = fromPath.relativize(dir);
            if (!pathMatcher.matches(sourceRelativePath)) {
                return FileVisitResult.CONTINUE;
            }
            if (includeDirectories && !fromPath.equals(dir)) {
                count ++;
            }
            if (count > countMax) {
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path relativePath = fromPath.relativize(file);
            if (!pathMatcher.matches(relativePath)) {
                return FileVisitResult.CONTINUE;
            }
            count ++;
            if (count > countMax) {
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    public static Optional<Long> getLastModifiedTime(Path path, LinkOption ...options) {
        JkUtilsAssert.argument(Files.exists(path), "File " + path + " not found.");
        try {
            return Optional.of(Files.getLastModifiedTime(path, options).toMillis());
        } catch (IOException e) {
            JkLog.warn("Cannot get last modified time of file " + path + ".");
            return Optional.empty();
        }
    }

    public static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static WatchKey register(Path dir, WatchService watchService, WatchEvent.Kind<?>... watchEventKinds) {
        try {
            return dir.register(watchService, watchEventKinds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Move directory along all its children and subdirectories recursively
     */
    public static void moveDir(Path source, Path target) {
        // First, copy all contents from source to target directory
        walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        // Then, delete the original directory and its contents
        walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
