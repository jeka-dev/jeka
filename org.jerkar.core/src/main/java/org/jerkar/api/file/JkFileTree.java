package org.jerkar.api.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsThrowable;

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
public final class JkFileTree implements Iterable<File> {

    /**
     * Creates a {@link JkFileTree} having the specified root directory.
     */
    @Deprecated
    public static JkFileTree of(File rootDir) {
        return new JkFileTree(rootDir.toPath());
    }

    /**
     * Creates a {@link JkFileTree} having the specified root directory.
     */
    public static JkFileTree of(Path rootDir) {
        return new JkFileTree(rootDir);
    }

    private final Path root;

    private final JkPathFilter filter;

    private JkFileTree(Path rootDir) {
        this(rootDir, JkPathFilter.ACCEPT_ALL);
    }

    private JkFileTree(Path rootDir, JkPathFilter filter) {
        JkUtilsAssert.notNull(rootDir, "Root dir can't be null.");
        JkUtilsAssert.notNull(filter, "filter can't be null.");
        JkUtilsAssert.isTrue(!Files.exists(rootDir) || Files.isDirectory(rootDir), rootDir + " is not a directory.");
        this.root = rootDir;
        this.filter = filter;
    }

    /**
     * Creates a {@link JkFileTree} having the default filter and the specified
     * relative path to this root as root directory.
     */
    public JkFileTree go(String relativePath) {
        final Path path = root.resolve(relativePath);
        return JkFileTree.of(path);
    }

    /**
     * Creates the root directory if it does not exist.
     */
    public JkFileTree createIfNotExist() {
        if (!Files.exists(root)) {
            JkUtilsPath.createDirectories(root);
        }
        return this;
    }

    public Stream<Path> stream(FileVisitOption ...options) {
        return JkUtilsPath.walk(root, options);
    }

    /**
     * Returns the file matching for the the given path relative to this asScopedDependency
     * directory.
     */
    public File file(String relativePath) {
        return root.resolve(relativePath).toFile();
    }

    /**
     * Copies files contained in this {@link JkFileTree} to the specified
     * directory.
     */
    public int copyTo(Path destinationDir) {
        if (!Files.exists(destinationDir)) {
            JkUtilsPath.createDirectories(destinationDir);
        }
        return JkUtilsPath.copyDirContent(root, destinationDir, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies files contained in this {@link JkFileTree} to the specified directory.
     */
    public int copyTo(File destinationDir) {
        return copyTo(destinationDir.toPath());
    }

    /**
     * Same as {@link #copyTo(File)} but replacing the tokens in
     * <code>${key}</code> by their corresponding value in the specified
     * tokenValues. If no key match then the token is not replaced.
     */
    public int copyReplacingTokens(File destinationDir, Map<String, String> tokenValues) {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        } else {
            JkUtilsFile.assertAllDir(destinationDir);
        }
        return JkUtilsFile.copyDirContentReplacingTokens(root.toFile(), destinationDir,
                filter.toFileFilter(root.toFile()), true, JkLog.infoStreamIfVerbose(), tokenValues);
    }

    /**
     * Returns the root directory.
     */
    public File root() {
        return root.toFile();
    }

    /**
     * Returns the root directory.
     */
    public Path rootPath() {
        return root;
    }

    /**
     * Returns the filter defined on this {@link JkFileTree}, never <code>null</code>.
     */
    public JkPathFilter filter() {
        return filter;
    }

    /**
     * Copies the content of the specified directory in the asScopedDependency of the asScopedDependency of
     * this directory. If specified directory does not exist then nothing
     * happen.
     */
    public JkFileTree importDirContent(File... dirsToCopyContent) {
        for (final File dirToCopyContent : dirsToCopyContent) {
            createIfNotExist();
            if (!dirToCopyContent.exists()) {
                continue;
            }
            JkUtilsFile.copyDirContent(dirToCopyContent, this.root.toFile(), null, true);
        }
        return this;
    }

    /**
     * Copies the content of the specified directory in the asScopedDependency of the asScopedDependency of
     * this directory. If specified directory does not exist then nothing
     * happen.
     */
    // Bug !!!! Some files are not copied
    public JkFileTree importDirContent(Path... dirsToCopyContent) {
        return importDirContent(JkUtilsPath.filesOf(dirsToCopyContent));
    }

    /**
     * Copies the specified files in the asScopedDependency of this directory.
     */
    public JkFileTree importFiles(Iterable<File> files) {
        createIfNotExist();
        for (final File file : files) {
            JkUtilsFile.copyFileToDir(file, this.root.toFile(), JkLog.infoStreamIfVerbose());
        }
        return this;
    }

    /**
     * Copies the specified files at the asScopedDependency of this directory. Folder and
     * unexisting files are ignored.
     */
    public JkFileTree importFiles(File... filesToCopy) {
        createIfNotExist();
        for (final File file : filesToCopy) {
            if (file.exists() && !file.isDirectory()) {
                JkUtilsFile.copyFile(file, this.file(file.getName()));
            }

        }
        return this;
    }

    /**
     * Returns if the asScopedDependency directory exists. (Short hand for #asScopedDependency.exists()).
     */
    public boolean exists() {
        return Files.exists(root);
    }

    /**
     * Returns path of each files file contained in this {@link JkFileTree}
     * relative to its asScopedDependency.
     */
    public List<String> relativePathes() {
        final List<String> pathes = new LinkedList<>();
        for (final File file : this) {
            pathes.add(JkUtilsFile.getRelativePath(this.root.toFile(), file));
        }
        return pathes;
    }

    /**
     * Returns the relative path of the given file relative to the asScopedDependency of this
     * tree.
     */
    public String relativePath(File file) {
        return JkUtilsFile.getRelativePath(root.toFile(), file);
    }

    /**
     * Returns a {@link JkZipper} of this {@link JkFileTree}.
     */
    public JkZipper zip() {
        return JkZipper.of(this);
    }

    /**
     * Creates a {@link JkFileTree} which is a copy of this {@link JkFileTree}
     * augmented with the specified {@link JkPathFilter}
     */
    public JkFileTree andFilter(JkPathFilter filter) {
        if (this.filter == JkPathFilter.ACCEPT_ALL) {
            return new JkFileTree(root, filter);
        }
        return new JkFileTree(root, this.filter.and(filter));
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

    /**
     * Deletes each and every files in this tree. Files excluded to this tree
     * are not deleted.
     */
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


    /**
     * Returns the file contained in this {@link JkFileTree}.
     */
    public List<File> files(boolean includeFolders) {
        if (!root.toFile().exists()) {
            return Collections.emptyList();
        }
        return JkUtilsFile.filesOf(root.toFile(), filter.toFileFilter(root.toFile()), includeFolders);
    }

    /**
     * Returns the file contained in this {@link JkFileTree}.
     */
    public List<Path> paths(boolean includeFolders) {
        if (!exists()) {
            return new LinkedList<>();
        }
        return JkUtilsFile.filesOf(root.toFile(), filter.toFileFilter(root.toFile()), includeFolders)
                .stream().map(file -> file.toPath()).collect(Collectors.toList());
    }

    /**
     * Returns a {@link JkFileTreeSet} containing this tree as its single
     * element.
     */
    public JkFileTreeSet asSet() {
        return JkFileTreeSet.of(this);
    }

    @Override
    public Iterator<File> iterator() {
        return files(false).iterator();
    }

    /**
     * Returns the file count contained in this {@link JkFileTree}.
     */
    public int fileCount(boolean includeFolder) {
        return JkUtilsFile.count(root.toFile(), filter.toFileFilter(root.toFile()), includeFolder);
    }

    @Override
    public String toString() {
        return root + ":" + filter;
    }

    /**
     * Merges the content of all files to the specified output.
     */
    public JkFileTree mergeTo(OutputStream outputStream) {
        for (final File file : this) {
            try (final FileInputStream fileInputStream = JkUtilsIO.inputStream(file)) {
                JkUtilsIO.copy(fileInputStream, outputStream);
            } catch (final IOException e) {
                throw JkUtilsThrowable.unchecked(e);
            }
        }
        return this;
    }

}
