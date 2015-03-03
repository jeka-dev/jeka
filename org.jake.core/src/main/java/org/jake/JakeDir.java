package org.jake;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsFile;

/**
 * Provides a view on files and sub-folders contained in a given directory. A <code>JakeDir</code> may
 * have some include/exclude filters to include only or exclude some files based on ANT pattern matching. <br/>
 * 
 *<p>
 * When speaking about files contained in a {@link JakeDir}, we mean all files contained in its root directory
 * or sub-directories, matching positively the filter defined on it.
 * 
 * @author Jerome Angibaud
 */
public final class JakeDir implements Iterable<File> {

	/**
	 * Creates a {@link JakeDir} having the specified root directory.
	 */
	public static JakeDir of(File rootDir) {
		return new JakeDir(rootDir);
	}

	private final File root;

	private final JakeFileFilter filter;

	private JakeDir(File rootDir) {
		this(rootDir, JakeFileFilter.ACCEPT_ALL);
	}

	/**
	 * Creates a {@link JakeDir} having the specified root directory and filter.
	 */
	private JakeDir(File rootDir, JakeFileFilter filter) {
		JakeUtilsAssert.notNull(rootDir, "Root dir can't be null.");
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
	 * Creates a {@link JakeDir} having the default filter and the specified relative path to this root as
	 * root directory.
	 */
	public JakeDir sub(String relativePath) {
		final File newBase = new File(root, relativePath);

		return new JakeDir(newBase);
	}

	/**
	 * Creates the root directory if it does not exist.
	 */
	public JakeDir createIfNotExist() {
		if (!root.exists() ) {
			root.mkdirs();
		}
		return this;
	}

	/**
	 * Returns the file matching for the the given path relative to this root directory.
	 */
	public File file(String relativePath) {
		return new File(root, relativePath);
	}

	/**
	 * Copies files contained in this {@link JakeDir} to the specified directory.
	 */
	public int copyTo(File destinationDir) {
		if (!destinationDir.exists()) {
			destinationDir.mkdirs();
		} else {
			JakeUtilsFile.assertDir(destinationDir);
		}
		return JakeUtilsFile.copyDir(root, destinationDir, filter.toFileFilter(root), true, JakeOptions.isVerbose(), JakeLog.infoStream());
	}

	public int copyReplacingTokens(File destinationDir, Map<String, String> tokenValues) {
		if (!destinationDir.exists()) {
			destinationDir.mkdirs();
		} else {
			JakeUtilsFile.assertDir(destinationDir);
		}
		return JakeUtilsFile.copyDirReplacingTokens(root, destinationDir, filter.toFileFilter(root), true, JakeOptions.isVerbose(), JakeLog.infoStream(), tokenValues);
	}

	/**
	 * Returns the root directory.
	 */
	public File root() {
		return root;
	}

	/**
	 * Returns the filter defined on this {@link JakeDir}, never <code>null</code>.
	 */
	public JakeFileFilter filter() {
		return filter;
	}

	/**
	 * Copies the content of the specified directory in the root of the root of this directory.
	 * If specified directory does not exist then nothing happen.
	 */
	public JakeDir copyDirContent(File dirToCopyContent) {
		createIfNotExist();
		if (!dirToCopyContent.exists()) {
			return this;
		}
		JakeUtilsFile.copyDir(dirToCopyContent, this.root, null, true);
		return this;
	}

	/**
	 * Copies the specified files at the root of this directory.
	 * Folder and unexisting files are ignored.
	 */
	public JakeDir copyFiles(File ... filesToCopy) {
		createIfNotExist();
		for(final File file : filesToCopy ) {
			if (file.exists() && !file.isDirectory()) {
				JakeUtilsFile.copyFile(file, this.file(file.getName()));
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
	 * Returns path of each files file contained in this {@link JakeDir} relative to its
	 * root.
	 */
	public List<String> relativePathes() {
		final List<String> pathes = new LinkedList<String>();
		for (final File file : this) {
			pathes.add(JakeUtilsFile.getRelativePath(this.root, file));
		}
		return pathes;
	}

	/**
	 * Returns a {@link JakeZip} of this {@link JakeDir}.
	 */
	public JakeZip zip() {
		return JakeZip.of(this);
	}

	/**
	 * Creates a {@link JakeDir} having the same root directory as this one but without any filter.
	 */
	public JakeDir noFiltering() {
		return new JakeDir(root);
	}

	/**
	 * Returns if this file is contained in this {@link JakeDir}.
	 */
	public boolean contains(File file) {
		if (!this.isAncestorOf(file)) {
			return false;
		}
		final String relativePath = JakeUtilsFile.getRelativePath(root, file);
		return this.filter.accept(relativePath);
	}

	private boolean isAncestorOf(File file) {
		return JakeUtilsFile.isAncestor(root, file);
	}

	/**
	 * Creates a {@link JakeDir} which is a copy of this {@link JakeDir} augmented
	 * with the specified {@link JakeFileFilter}
	 */
	public JakeDir andFilter(JakeFileFilter filter) {
		if (this.filter == JakeFileFilter.ACCEPT_ALL) {
			return new JakeDir(root, filter);
		}
		return new JakeDir(root, this.filter.and(filter));
	}

	/**
	 * Short hand to {@link #andFilter(JakeFileFilter)} defining an include Ant pattern filter.
	 */
	public JakeDir include(String ... antPatterns) {
		return andFilter(JakeFileFilter.include(antPatterns));
	}

	/**
	 * Short hand to {@link #andFilter(JakeFileFilter)} defining an exclude Ant pattern filter.
	 */
	public JakeDir exclude(String ... antPatterns) {
		return andFilter(JakeFileFilter.exclude(antPatterns));
	}

	/**
	 * Returns a {@link JakeDirSet} made of this {@link JakeDir} and the specified one.
	 */
	public JakeDirSet and(JakeDir dirView) {
		return JakeDirSet.of(this, dirView);
	}

	/**
	 * Returns the file contained in this {@link JakeDir}.
	 */
	public List<File> files() {
		if (!root.exists()) {
			throw new IllegalStateException("Folder " + root.getAbsolutePath() + " does nor exist.");
		}
		return JakeUtilsFile.filesOf(root, filter.toFileFilter(root), false);
	}

	@Override
	public Iterator<File> iterator() {
		return files().iterator();
	}

	/**
	 * Returns the file count contained in this {@link JakeDir}.
	 */
	public int fileCount(boolean includeFolder) {
		return JakeUtilsFile.count(root, filter.toFileFilter(root), includeFolder);
	}

	@Override
	public String toString() {
		return root.getPath() + ":" + filter;
	}



}
