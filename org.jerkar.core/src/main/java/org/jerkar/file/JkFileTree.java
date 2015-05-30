package org.jerkar.file;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.JkLog;
import org.jerkar.utils.JkUtilsAssert;
import org.jerkar.utils.JkUtilsFile;

/**
 * Provides a view on files and sub-folders contained in a given directory. A <code>JkFileTree</code> may
 * have some include/exclude filters to include only or exclude some files based on ANT pattern matching. <br/>
 * 
 *<p>
 * When speaking about files contained in a {@link JkFileTree}, we mean all files contained in its root directory
 * or sub-directories, matching positively the filter defined on it.
 * 
 * @author Jerome Angibaud
 */
public final class JkFileTree implements Iterable<File> {

	/**
	 * Creates a {@link JkFileTree} having the specified root directory.
	 */
	public static JkFileTree of(File rootDir) {
		return new JkFileTree(rootDir);
	}

	private final File root;

	private final JkPathFilter filter;

	private JkFileTree(File rootDir) {
		this(rootDir, JkPathFilter.ACCEPT_ALL);
	}

	/**
	 * Creates a {@link JkFileTree} having the specified root directory and filter.
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
	 * Creates a {@link JkFileTree} having the default filter and the specified relative path to this root as
	 * root directory.
	 */
	public JkFileTree from(String relativePath) {
		final File newBase = new File(root, relativePath);

		return new JkFileTree(newBase);
	}

	/**
	 * Creates the root directory if it does not exist.
	 */
	public JkFileTree createIfNotExist() {
		if (!root.exists() ) {
			root.mkdirs();
		}
		return this;
	}

	/**
	 * Returns the file matching for the the given path relative to this root directory.
	 */
	public File file(String relativePath) {
		return JkUtilsFile.canonicalFile(new File(root, relativePath));
	}

	/**
	 * Copies files contained in this {@link JkFileTree} to the specified directory.
	 */
	public int copyTo(File destinationDir) {
		if (!destinationDir.exists()) {
			destinationDir.mkdirs();
		} else {
			JkUtilsFile.assertAllDir(destinationDir);
		}
		return JkUtilsFile.copyDirContent(root, destinationDir, filter.toFileFilter(root), true, JkLog.infoStreamIfVerbose());
	}

	/**
	 * Same as {@link #copyTo(File)} but replacing the tokens in <code>${key}</code>
	 * by their corresponding value in the specified tokenValues.
	 * If no key match then the token is not replaced.
	 */
	public int copyReplacingTokens(File destinationDir, Map<String, String> tokenValues) {
		if (!destinationDir.exists()) {
			destinationDir.mkdirs();
		} else {
			JkUtilsFile.assertAllDir(destinationDir);
		}
		return JkUtilsFile.copyDirContentReplacingTokens(root, destinationDir, filter.toFileFilter(root), true, JkLog.infoStreamIfVerbose(), tokenValues);
	}

	/**
	 * Returns the root directory.
	 */
	public File root() {
		return root;
	}

	/**
	 * Returns the filter defined on this {@link JkFileTree}, never <code>null</code>.
	 */
	public JkPathFilter filter() {
		return filter;
	}

	/**
	 * Copies the content of the specified directory in the root of the root of this directory.
	 * If specified directory does not exist then nothing happen.
	 */
	public JkFileTree importDirContent(File dirToCopyContent) {
		createIfNotExist();
		if (!dirToCopyContent.exists()) {
			return this;
		}
		JkUtilsFile.copyDirContent(dirToCopyContent, this.root, null, true);
		return this;
	}

	/**
	 * Copies the specified files in the root of this directory.
	 */
	public JkFileTree importFiles(Iterable<File> files) {
		createIfNotExist();
		for (final File file : files) {
			JkUtilsFile.copyFileToDir(file, this.root, JkLog.infoStreamIfVerbose());
		}
		return this;
	}


	/**
	 * Copies the specified files at the root of this directory.
	 * Folder and unexisting files are ignored.
	 */
	public JkFileTree importFiles(File ... filesToCopy) {
		createIfNotExist();
		for(final File file : filesToCopy ) {
			if (file.exists() && !file.isDirectory()) {
				JkUtilsFile.copyFile(file, this.file(file.getName()));
			}

		}
		return this;
	}

	/**
	 * Returns if the root directory exists. (Short hand for #root.exists()).
	 */
	public boolean exists() {
		return root.exists();
	}

	/**
	 * Returns path of each files file contained in this {@link JkFileTree} relative to its
	 * root.
	 */
	public List<String> relativePathes() {
		final List<String> pathes = new LinkedList<String>();
		for (final File file : this) {
			pathes.add(JkUtilsFile.getRelativePath(this.root, file));
		}
		return pathes;
	}

	/**
	 * Returns the relative path of the given file relative to the root of this tree.
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

	/**
	 * Creates a {@link JkFileTree} having the same root directory as this one but without any filter.
	 */
	public JkFileTree noFiltering() {
		return new JkFileTree(root);
	}

	/**
	 * Returns if this file is contained in this {@link JkFileTree}.
	 */
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

	/**
	 * Creates a {@link JkFileTree} which is a copy of this {@link JkFileTree} augmented
	 * with the specified {@link JkPathFilter}
	 */
	public JkFileTree andFilter(JkPathFilter filter) {
		if (this.filter == JkPathFilter.ACCEPT_ALL) {
			return new JkFileTree(root, filter);
		}
		return new JkFileTree(root, this.filter.and(filter));
	}

	/**
	 * Short hand to {@link #andFilter(JkPathFilter)} defining an include Ant pattern filter.
	 * This will include any file matching at least one of the specified <code>antPatterns</code>.
	 */
	public JkFileTree include(String ... antPatterns) {
		return andFilter(JkPathFilter.include(antPatterns));
	}

	/**
	 * Short hand to {@link #andFilter(JkPathFilter)} defining an exclude Ant pattern filter.
	 * This will exclude any file matching at least one of specified <code>antPatterns</code>.
	 */
	public JkFileTree exclude(String ... antPatterns) {
		return andFilter(JkPathFilter.exclude(antPatterns));
	}

	/**
	 * Deletes each and every files in this tree. Files excluded from this tree are not deleted.
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
	 * Returns a {@link JkFileTreeSet} made of this {@link JkFileTree} and the specified one.
	 */
	public JkFileTreeSet and(JkFileTree dirView) {
		return JkFileTreeSet.of(this, dirView);
	}

	/**
	 * Returns the file contained in this {@link JkFileTree}.
	 */
	public List<File> files(boolean includeFolders) {
		if (!root.exists()) {
			throw new IllegalStateException("Folder " + root.getAbsolutePath() + " does nor exist.");
		}
		return JkUtilsFile.filesOf(root, filter.toFileFilter(root), includeFolders);
	}

	/**
	 * Returns a {@link JkFileTreeSet} containing this tree as its single element.
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



}
