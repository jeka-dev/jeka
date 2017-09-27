package org.jerkar.api.utils;

import com.sun.java.swing.plaf.windows.WindowsTreeUI;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class providing convenient methods to deal with {@link java.nio.file.Path}.
 * Mainly for wrapping checked exceptions and handle safely null parameters.
 */
public class JkUtilsPath {

    private JkUtilsPath() {
        // Do nothing
    }

    public static DirectoryStream<Path> newDirectoryStream(Path root, DirectoryStream.Filter<Path> filter) {
        try {
            return Files.newDirectoryStream(root, filter);
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    public static List<Path> listDirectChildren(Path path) {
        try (Stream stream = Files.list(path)) {
            return (List<Path>) stream.collect(Collectors.toList());
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Delegates to {@link Files#createDirectories(Path, FileAttribute[])} wrapping checked exception.
     */
    public static void createDir(Path path, FileAttribute<?>... attrs) {
        try {
            Files.createDirectories(path, attrs);
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Delegates to {@link Files#copy(Path, Path, CopyOption...)} wrapping checked exception.
     */
    public static void copy(Path source, Path target, CopyOption ...copyOptions) {
        try {
            Files.copy(source, target, copyOptions);
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Copies the content of the source directory into the target directory. The root of source directory is
     * not created as an entry of the target directory.
     * @return the copied file count.
     */
    public static int copyDirContent(Path sourceDir, Path targetDir, CopyOption ... copyOptions)  {
        CopyDirVisitor visitor = new CopyDirVisitor(sourceDir, targetDir, copyOptions);
        createDir(targetDir);
        try {
            Files.walkFileTree(sourceDir, visitor);
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        return visitor.count;
    }

    private static class CopyDirVisitor extends SimpleFileVisitor<Path> {

        int count;

        CopyDirVisitor(Path fromPath, Path toPath, CopyOption ... options) {
            this.fromPath = fromPath;
            this.toPath = toPath;
            this.options = options;
        }

        private final Path fromPath;
        private final Path toPath;
        private final CopyOption[] options;


        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = toPath.resolve(fromPath.relativize(dir));
            Files.createDirectories(targetPath);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, toPath.resolve(fromPath.relativize(file)), options);
            count ++;
            return FileVisitResult.CONTINUE;
        }
    }


}
