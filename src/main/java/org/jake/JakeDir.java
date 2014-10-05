package org.jake;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jake.utils.JakeUtilsFile;

/**
 * A directory that may be filtered or not. If there is no <code>Filter</code> than this stands
 * simply for a directory. If a filter is defined on, than this stands for only a part of this directory,
 * meaning that files or subfolder filtered are not part of this.
 * 
 * @author Jerome Angibaud
 */
public final class JakeDir implements Iterable<File> {

	private final File root;

	private final JakeFileFilter filter;

	/**
	 * Creates a {@link JakeDir} having the specified root directory.
	 */
	public static JakeDir of(File rootDir) {
		return new JakeDir(rootDir);
	}

	private JakeDir(File rootDir, JakeFileFilter filter) {
		if (filter == null) {
			throw new IllegalArgumentException("filter can't be null.");
		}
		if (rootDir.exists() && !rootDir.isDirectory()) {
			throw new IllegalArgumentException(rootDir + " is not a directory.");
		}
		this.root = rootDir;
		this.filter = filter;
	}

	private JakeDir(File rootDir) {
		this(rootDir, JakeFileFilter.ACCEPT_ALL);
	}

	/**
	 * Creates a {@link JakeDir} having the default filter and the specified relative path to this root as
	 * root directory.
	 */
	public JakeDir sub(String relativePath) {
		final File newBase = new File(root, relativePath);

		return new JakeDir(newBase);
	}

	public JakeDir createIfNotExist() {
		if (!root.exists() ) {
			root.mkdirs();
		}
		return this;
	}

	public File file(String relativePath) {
		return new File(root, relativePath);
	}

	public int copyTo(File destinationDir) {
		if (!destinationDir.exists()) {
			destinationDir.mkdirs();
		} else {
			JakeUtilsFile.assertDir(destinationDir);
		}
		return JakeUtilsFile.copyDir(root, destinationDir, filter.toFileFilter(root), true, JakeOptions.isVerbose(), JakeLog.infoStream());
	}

	public int copyTo(JakeDir destinationDir) {
		return copyTo(destinationDir.root());
	}

	public File root() {
		return root;
	}

	public JakeFileFilter getFilter() {
		return filter;
	}

	public boolean exists() {
		return root.exists();
	}

	public List<String> relativePathes() {
		final List<String> pathes = new LinkedList<String>();
		for (final File file : this) {
			pathes.add(JakeUtilsFile.getRelativePath(this.root, file));
		}
		return pathes;
	}

	public JakeZip zip() {
		return JakeZip.of(this);
	}

	public JakeDir noFiltering() {
		return new JakeDir(root);
	}

	public boolean contains(File file) {
		if (!this.isAncestorOf(file)) {
			return false;
		}
		final String relativePath = JakeUtilsFile.getRelativePath(root, file);
		return this.filter.accept(relativePath);
	}

	public boolean isAncestorOf(File file) {
		return JakeUtilsFile.isAncestor(root, file);
	}

	/**
	 * Creates a {@link JakeDir} which is a copy of this {@link JakeDir} augmented
	 * with the specified {@link Filter#}
	 */
	public JakeDir andFilter(JakeFileFilter filter) {
		if (this.filter == JakeFileFilter.ACCEPT_ALL) {
			return new JakeDir(root, filter);
		}
		return new JakeDir(root, this.filter.and(filter));
	}

	public JakeDir include(String ... antPatterns) {
		return andFilter(JakeFileFilter.include(antPatterns));
	}

	public JakeDir exclude(String ... antPatterns) {
		return andFilter(JakeFileFilter.exclude(antPatterns));
	}

	public JakeDirSet and(JakeDir dirView) {
		return JakeDirSet.of(this, dirView);
	}

	public JakeDirSet and(JakeDirSet dirViews) {
		return JakeDirSet.of(this).and(dirViews);
	}


	public List<File> listFiles() {
		if (!root.exists()) {
			throw new IllegalStateException("Folder " + root.getAbsolutePath() + " does nor exist.");
		}
		return JakeUtilsFile.filesOf(root, filter.toFileFilter(root), false);
	}

	@Override
	public Iterator<File> iterator() {
		return listFiles().iterator();
	}

	public void print(PrintStream printStream) {
		for (final File file : this) {
			printStream.println(file.getPath());
		}
	}

	public int fileCount(boolean includeFolder) {
		return JakeUtilsFile.count(root, filter.toFileFilter(root), includeFolder);
	}

	@Override
	public String toString() {
		return root.getPath() + ":" + filter;
	}



}
