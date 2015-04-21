package org.jerkar;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.utils.JkUtilsAssert;
import org.jerkar.utils.JkUtilsFile;

/**
 * Provides a view on files and sub-folders contained in a given directory. A <code>JkDir</code> may
 * have some include/exclude filters to include only or exclude some files based on ANT pattern matching. <br/>
 * 
 *<p>
 * When speaking about files contained in a {@link JkDir}, we mean all files contained in its root directory
 * or sub-directories, matching positively the filter defined on it.
 * 
 * @author Jerome Angibaud
 */
public final class JkDir implements Iterable<File> {

	/**
	 * Creates a {@link JkDir} having the specified root directory.
	 */
	public static JkDir of(File rootDir) {
		return new JkDir(rootDir);
	}

	private final File root;

	private final JkFileFilter filter;

	private JkDir(File rootDir) {
		this(rootDir, JkFileFilter.ACCEPT_ALL);
	}

	/**
	 * Creates a {@link JkDir} having the specified root directory and filter.
	 */
	private JkDir(File rootDir, JkFileFilter filter) {
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
	 * Creates a {@link JkDir} having the default filter and the specified relative path to this root as
	 * root directory.
	 */
	public JkDir sub(String relativePath) {
		final File newBase = new File(root, relativePath);

		return new JkDir(newBase);
	}

	/**
	 * Creates the root directory if it does not exist.
	 */
	public JkDir createIfNotExist() {
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
	 * Copies files contained in this {@link JkDir} to the specified directory.
	 */
	public int copyTo(File destinationDir) {
		if (!destinationDir.exists()) {
			destinationDir.mkdirs();
		} else {
			JkUtilsFile.assertDir(destinationDir);
		}
		return JkUtilsFile.copyDir(root, destinationDir, filter.toFileFilter(root), true, JkLog.infoStreamIfVerbose());
	}

	public int copyReplacingTokens(File destinationDir, Map<String, String> tokenValues) {
		if (!destinationDir.exists()) {
			destinationDir.mkdirs();
		} else {
			JkUtilsFile.assertDir(destinationDir);
		}
		return JkUtilsFile.copyDirReplacingTokens(root, destinationDir, filter.toFileFilter(root), true, JkLog.infoStreamIfVerbose(), tokenValues);
	}

	/**
	 * Returns the root directory.
	 */
	public File root() {
		return root;
	}

	/**
	 * Returns the filter defined on this {@link JkDir}, never <code>null</code>.
	 */
	public JkFileFilter filter() {
		return filter;
	}

	/**
	 * Copies the content of the specified directory in the root of the root of this directory.
	 * If specified directory does not exist then nothing happen.
	 */
	public JkDir importDirContent(File dirToCopyContent) {
		createIfNotExist();
		if (!dirToCopyContent.exists()) {
			return this;
		}
		JkUtilsFile.copyDir(dirToCopyContent, this.root, null, true);
		return this;
	}

	/**
	 * Copies the specified files in the root of this directory.
	 */
	public JkDir copyInFiles(Iterable<File> files) {
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
	public JkDir importFiles(File ... filesToCopy) {
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
	 * Returns path of each files file contained in this {@link JkDir} relative to its
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
	 * Returns a {@link JkZipper} of this {@link JkDir}.
	 */
	public JkZipper zip() {
		return JkZipper.of(this);
	}

	/**
	 * Creates a {@link JkDir} having the same root directory as this one but without any filter.
	 */
	public JkDir noFiltering() {
		return new JkDir(root);
	}

	/**
	 * Returns if this file is contained in this {@link JkDir}.
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
	 * Creates a {@link JkDir} which is a copy of this {@link JkDir} augmented
	 * with the specified {@link JkFileFilter}
	 */
	public JkDir andFilter(JkFileFilter filter) {
		if (this.filter == JkFileFilter.ACCEPT_ALL) {
			return new JkDir(root, filter);
		}
		return new JkDir(root, this.filter.and(filter));
	}

	/**
	 * Short hand to {@link #andFilter(JkFileFilter)} defining an include Ant pattern filter.
	 */
	public JkDir include(String ... antPatterns) {
		return andFilter(JkFileFilter.include(antPatterns));
	}

	/**
	 * Short hand to {@link #andFilter(JkFileFilter)} defining an exclude Ant pattern filter.
	 */
	public JkDir exclude(String ... antPatterns) {
		return andFilter(JkFileFilter.exclude(antPatterns));
	}

	/**
	 * Deletes each and every files in this tree. Files excluded from this tree are not deleted.
	 */
	public JkDir deleteAll() {
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
	 * Returns a {@link JkDirSet} made of this {@link JkDir} and the specified one.
	 */
	public JkDirSet and(JkDir dirView) {
		return JkDirSet.of(this, dirView);
	}

	/**
	 * Returns the file contained in this {@link JkDir}.
	 */
	public List<File> files(boolean includeFolders) {
		if (!root.exists()) {
			throw new IllegalStateException("Folder " + root.getAbsolutePath() + " does nor exist.");
		}
		return JkUtilsFile.filesOf(root, filter.toFileFilter(root), includeFolders);
	}



	@Override
	public Iterator<File> iterator() {
		return files(false).iterator();
	}

	/**
	 * Returns the file count contained in this {@link JkDir}.
	 */
	public int fileCount(boolean includeFolder) {
		return JkUtilsFile.count(root, filter.toFileFilter(root), includeFolder);
	}

	@Override
	public String toString() {
		return root.getPath() + ":" + filter;
	}



}
