package org.jerkar.api.file;

import org.jerkar.api.utils.*;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Provides a view on files and sub-folders contained in a given directory. A
 * <code>JkFileTree</code> may have some include/exclude filters to include only
 * or exclude some files based on ANT pattern matching. <br/>
 * 
 * <p>
 * When speaking about files contained in a {@link JkFileTree2}, we mean all
 * files contained in its root directory or sub-directories, matching positively
 * the filter defined on it.
 * 
 * @author Jerome Angibaud
 */
final class JkFileTree2 implements Iterable<Path>  {

    /**
     * Creates a {@link JkFileTree2} having the specified asScopedDependency directory.
     */
    public static JkFileTree2 of(Path rootDir) {
        return new JkFileTree2(rootDir);
    }

    private final Path root;

    private final JkPathFilter filter;

    private JkFileTree2(Path rootDir) {
        this(rootDir, JkPathFilter.ACCEPT_ALL);
    }

    /**
     * Creates a {@link JkFileTree2} having the specified asScopedDependency directory and
     * filter.
     */
    private JkFileTree2(Path rootDir, JkPathFilter filter) {
        JkUtilsAssert.notNull(rootDir, "Root dir can't be null.");
        if (filter == null) {
            throw new IllegalArgumentException("filter can't be null.");
        }
        if (Files.exists(rootDir) && !Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException(rootDir + " is not a directory.");
        }
        this.root = rootDir;
        this.filter = filter;
    }

    /**
     * Creates a {@link JkFileTree2} having the default filter and the specified
     * relative path as it root directory.
     */
    public JkFileTree2 go(String relativePath) {
        return new JkFileTree2(this.root.resolve(relativePath));
    }

    /**
     * Creates the root directory if it does not exist.
     */
    public JkFileTree2 createIfNotExist() {
        JkUtilsPath.createDir(root);
        return this;
    }

    /**
     * Returns the file matching for the the given path relative to this root directory.
     */
    public Path file(String relativePath) {
        return root.resolve(relativePath);
    }

    /**
     * Copies files contained in this {@link JkFileTree} to the specified
     * directory.
     */
    public int copyTo(Path destinationDir) {
        return  JkUtilsPath.copyDirContent(root, destinationDir);
    }

    /**
     * Returns the root directory.
     */
    public Path root() {
        return root;
    }

    /**
     * Returns the filter defined on this {@link JkFileTree}, never
     * <code>null</code>.
     */
    public JkPathFilter filter() {
        return filter;
    }

    /**
     * Copies the content of the specified directory in the asScopedDependency of the asScopedDependency of
     * this directory. If specified directory does not exist then nothing
     * happen.
     */
    public JkFileTree2 importDirContent(Path... dirsToCopyContent) {
        createIfNotExist();
        for (final Path sourcePath : dirsToCopyContent) {
            JkUtilsPath.copyDirContent(sourcePath, root);
        }
        return this;
    }

    /**
     * Copies the specified directories content at the asScopedDependency at this file tree, preserving relative paths.
     * @return this object.
     */
    public JkFileTree2 importDirContent(Iterable<Path> dirsToCopyContent) {
        return importDirContent(JkUtilsIterable.arrayOf(dirsToCopyContent, Path.class));
    }

    /**
     * Copies the specified files into the root.
     */
    public JkFileTree2 importFiles(Iterable<Path> paths) {
        createIfNotExist();
        for (final Path path : paths) {
            JkUtilsPath.copy(path, root.resolve(path.getFileName()));
        }
        return this;
    }

    /**
     * Copies the specified files at the asScopedDependency of this directory. Folder and
     * unexisting files are ignored.
     */
    public JkFileTree2 importFiles(Path... filesToCopy) {
        return importFiles(Arrays.asList(filesToCopy));
    }

    private DirectoryStream.Filter<Path> streamFilter() {
        return new DirectoryStream.Filter<Path>() {

            @Override
            public boolean accept(Path path) {
                Path relativePath = path.relativize(root);
                return JkFileTree2.this.filter.accept(relativePath.toString());
            }
        };
    }

    /**
     * Returns if the asScopedDependency directory exists. (Short hand for #asScopedDependency.exists()).
     */
    public boolean exists() {
        return Files.exists(root);
    }

    /**
     * Returns the file contained in this {@link JkFileTree}.
     */
    @Deprecated  // use Stream
    public List<Path> files(boolean includeFolders) {
        if (!exists()) {
            return new LinkedList<>();
        }
        List<Path> result = new LinkedList<>();
        stream().forEach(item -> result.add(item));
        return result;
    }

    public DirectoryStream<Path> stream() {
        return JkUtilsPath.newDirectoryStream(root, streamFilter());
    }

    @Override
    public Iterator<Path> iterator() {
        return files(false).iterator();
    }


    /**
     * Returns path of each files file contained in this {@link JkFileTree}
     * relative to its asScopedDependency.
     */
    @Deprecated  // can be done with standard api
    public List<String> relativePathes() {
        final List<String> result = new LinkedList<>();
        stream().forEach(item -> result.add(root.relativize(item).toString()));
        return result;
    }

    /**
     * Short hand to {@link #andFilter(JkPathFilter)} defining an include Ant
     * pattern filter. This will include any file matching at least one of the
     * specified <code>antPatterns</code>.
     */
    public JkFileTree2 include(String... antPatterns) {
        return andFilter(JkPathFilter.include(antPatterns));
    }

    /**
     * Short hand to {@link #andFilter(JkPathFilter)} defining an exclude Ant
     * pattern filter. This will exclude any file matching at least one of
     * specified <code>antPatterns</code>.
     */
    public JkFileTree2 exclude(String... antPatterns) {
        return andFilter(JkPathFilter.exclude(antPatterns));
    }

    public JkFileTree2 andFilter(JkPathFilter filter) {
        if (this.filter == JkPathFilter.ACCEPT_ALL) {
            return new JkFileTree2(root, filter);
        }
        return new JkFileTree2(root, this.filter.and(filter));
    }

   /**

    public String relativePath(File file) {
        return JkUtilsFile.getRelativePath(root, file);
    }

    public JkZipper zip() {
        return JkZipper.of(this);
    }

    public JkFileTree noFiltering() {
        return new JkFileTree(root);
    }

    public boolean contains(File file) {
        if (!this.isAncestorOf(file)) {
            return false;
        }
        final String relativePath = JkUtilsFile.getRelativePath(root, file);
        return this.filter.accept(relativePath);
    }

    private boolean isAncestorOf(File file) {
        return JkUtilsFile.isAncestor(root, file);
    }





    public JkFileTree deleteAll() {
        final List<File> files = this.files(true);
        for (final File file : files) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    JkUtilsFile.deleteDirContent(file);
                }
                JkUtilsFile.delete(file);
            }
        }
        return this;
    }

    public JkFileTreeSet and(JkFileTree dirView) {
        return JkFileTreeSet.of(this, dirView);
    }


    public int fileCount(boolean includeFolder) {
        return JkUtilsFile.count(root, filter.toFileFilter(root), includeFolder);
    }

    @Override
    public String toString() {
        return root.getPath() + ":" + filter;
    }


    public JkFileTree mergeTo(File target) {
        JkUtilsFile.createFileIfNotExist(target);
        try (final FileOutputStream outputStream = JkUtilsIO.outputStream(target, true)) {
            mergeTo(outputStream);
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        return this;
    }


    public JkFileTree mergeTo(OutputStream outputStream) {
        for (final File file : this) {
            try (final FileInputStream fileInputStream = JkUtilsIO.inputStream(file)) {
                JkUtilsIO.copy(fileInputStream, outputStream);
            } catch (IOException e) {
                throw JkUtilsThrowable.unchecked(e);
            }
        }
        return this;
    }
*/


}
