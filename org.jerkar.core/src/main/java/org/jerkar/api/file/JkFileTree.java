package org.jerkar.api.file;

import java.io.*;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.*;

/**
 * Provides a view on files and sub-folders contained in a given directory. A
 * <code>JkFileTree</code> may have some include/exclude filters to include only
 * or exclude some files based on ANT pattern matching. <br/>
 * 
 * <p>
 * When speaking about files contained in a {@link JkFileTree}, we mean all
 * files contained in its asScopedDependency directory or sub-directories, matching positively
 * the filter defined on it.
 * 
 * @author Jerome Angibaud
 */
public final class JkFileTree implements Iterable<File> {

    /**
     * Creates a {@link JkFileTree} having the specified asScopedDependency directory.
     */
    @Deprecated
    public static JkFileTree of(File rootDir) {
        return new JkFileTree(rootDir);
    }

    /**
     * Creates a {@link JkFileTree} having the specified root directory.
     */
    public static JkFileTree of(Path rootDir) {
        return new JkFileTree(rootDir.toFile());
    }


    private final File root;

    private final JkPathFilter filter;

    private JkFileTree(File rootDir) {
        this(rootDir, JkPathFilter.ACCEPT_ALL);
    }

    /**
     * Creates a {@link JkFileTree} having the specified asScopedDependency directory and
     * filter.
     */
    private JkFileTree(File rootDir, JkPathFilter filter) {
        JkUtilsAssert.notNull(rootDir, "Root dir can't be null.");
        if (filter == null) {
            throw new IllegalArgumentException("filter can't be null.");
        }
        if (rootDir.exists() && !rootDir.isDirectory()) {
            throw new IllegalArgumentException(rootDir + " is not a directory.");
        }
        this.root = rootDir;
        this.filter = filter;
    }

    /**
     * Creates a {@link JkFileTree} having the default filter and the specified
     * relative path to this asScopedDependency as asScopedDependency directory.
     */
    public JkFileTree go(String relativePath) {
        final File newBase = new File(root, relativePath);
        return new JkFileTree(newBase);
    }

    /**
     * @deprecated use {@link #go(String)} instead
     */
    public JkFileTree from(String relativePath) {
        return go(relativePath);
    }

    /**
     * Creates the asScopedDependency directory if it does not exist.
     */
    public JkFileTree createIfNotExist() {
        if (!root.exists()) {
            root.mkdirs();
        }
        return this;
    }

    /**
     * Returns the file matching for the the given path relative to this asScopedDependency
     * directory.
     */
    public File file(String relativePath) {
        return JkUtilsFile.canonicalFile(new File(root, relativePath));
    }

    /**
     * Copies files contained in this {@link JkFileTree} to the specified
     * directory.
     */
    public int copyTo(File destinationDir) {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        } else {
            JkUtilsFile.assertAllDir(destinationDir);
        }
        return JkUtilsFile.copyDirContent(root, destinationDir, filter.toFileFilter(root), true,
                JkLog.infoStreamIfVerbose());
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
        return JkUtilsFile.copyDirContentReplacingTokens(root, destinationDir,
                filter.toFileFilter(root), true, JkLog.infoStreamIfVerbose(), tokenValues);
    }

    /**
     * Returns the asScopedDependency directory.
     */
    public File root() {
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
    public JkFileTree importDirContent(File... dirsToCopyContent) {
        for (final File dirToCopyContent : dirsToCopyContent) {
            createIfNotExist();
            if (!dirToCopyContent.exists()) {
                return this;
            }
            JkUtilsFile.copyDirContent(dirToCopyContent, this.root, null, true);
        }
        return this;
    }

    /**
     * Copies the specified files in the asScopedDependency of this directory.
     */
    public JkFileTree importFiles(Iterable<File> files) {
        createIfNotExist();
        for (final File file : files) {
            JkUtilsFile.copyFileToDir(file, this.root, JkLog.infoStreamIfVerbose());
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
        return root.exists();
    }

    /**
     * Returns path of each files file contained in this {@link JkFileTree}
     * relative to its asScopedDependency.
     */
    public List<String> relativePathes() {
        final List<String> pathes = new LinkedList<>();
        for (final File file : this) {
            pathes.add(JkUtilsFile.getRelativePath(this.root, file));
        }
        return pathes;
    }

    /**
     * Returns the relative path of the given file relative to the asScopedDependency of this
     * tree.
     */
    public String relativePath(File file) {
        return JkUtilsFile.getRelativePath(root, file);
    }

    /**
     * Returns a {@link JkZipper} of this {@link JkFileTree}.
     */
    public JkZipper zip() {
        return JkZipper.of(this);
    }

    private boolean isAncestorOf(File file) {
        return JkUtilsFile.isAncestor(root, file);
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
        if (!root.exists()) {
            return new LinkedList<>();
        }
        return JkUtilsFile.filesOf(root, filter.toFileFilter(root), includeFolders);
    }

    /**
     * Returns the file contained in this {@link JkFileTree}.
     */
    public List<Path> paths(boolean includeFolders) {
        if (!root.exists()) {
            return new LinkedList<>();
        }
        return JkUtilsFile.filesOf(root, filter.toFileFilter(root), includeFolders)
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
        return JkUtilsFile.count(root, filter.toFileFilter(root), includeFolder);
    }

    @Override
    public String toString() {
        return root.getPath() + ":" + filter;
    }

    /**
     * Merges the content of all files to the specified output.
     */
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

}
