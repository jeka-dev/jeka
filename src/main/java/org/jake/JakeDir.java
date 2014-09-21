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

	private final File base;

	private final JakeFileFilter filter;

	public static JakeDir of(File base) {
		return new JakeDir(base);
	}

	private JakeDir(File base, JakeFileFilter filter) {
		if (filter == null) {
			throw new IllegalArgumentException("filter can't be null.");
		}
		if (base.exists() && !base.isDirectory()) {
			throw new IllegalArgumentException(base + " is not a directory.");
		}
		this.base = base;
		this.filter = filter;
	}

	private JakeDir(File base) {
		this(base, JakeFileFilter.ACCEPT_ALL);
	}

	public JakeDir sub(String relativePath) {
		final File newBase = new File(base, relativePath);

		return new JakeDir(newBase);
	}

	public JakeDir createIfNotExist() {
		if (!base.exists() ) {
			base.mkdirs();
		}
		return this;
	}

	public File file(String relativePath) {
		return new File(base, relativePath);
	}

	public int copyTo(File destinationDir) {
		JakeUtilsFile.assertDir(destinationDir);
		return JakeUtilsFile.copyDir(base, destinationDir, filter.toFileFilter(base), true, JakeOptions.isVerbose(), JakeLog.infoStream());
	}

	public int copyTo(JakeDir destinationDir) {
		return copyTo(destinationDir.root());
	}

	public File root() {
		return base;
	}

	public JakeFileFilter getFilter() {
		return filter;
	}

	public boolean exists() {
		return base.exists();
	}

	public List<String> relativePathes() {
		final List<String> pathes = new LinkedList<String>();
		for (final File file : this) {
			pathes.add(JakeUtilsFile.getRelativePath(this.base, file));
		}
		return pathes;
	}

	public void zip(File zipFile, int compressLevel) {
		JakeUtilsFile.zipDir(zipFile, compressLevel, base);
	}

	public JakeDir noFiltering() {
		return new JakeDir(base);
	}

	public boolean contains(File file) {
		if (!this.isAncestorOf(file)) {
			return false;
		}
		final String relativePath = JakeUtilsFile.getRelativePath(base, file);
		return this.filter.accept(relativePath);
	}

	public boolean isAncestorOf(File file) {
		return JakeUtilsFile.isAncestor(base, file);
	}


	public JakeDir withFilter(JakeFileFilter filter) {
		if (this.filter == JakeFileFilter.ACCEPT_ALL) {
			return new JakeDir(base, filter);
		}
		return new JakeDir(base, this.filter.and(filter));
	}

	public JakeDir include(String ... antPatterns) {
		return withFilter(JakeFileFilter.include(antPatterns));
	}

	public JakeDir exclude(String ... antPatterns) {
		return withFilter(JakeFileFilter.exclude(antPatterns));
	}

	public JakeDirSet and(JakeDir dirView) {
		return JakeDirSet.of(this, dirView);
	}

	public JakeDirSet and(JakeDirSet dirViews) {
		return JakeDirSet.of(this).and(dirViews);
	}


	public List<File> listFiles() {
		if (!base.exists()) {
			throw new IllegalStateException("Folder " + base.getAbsolutePath() + " does nor exist.");
		}
		return JakeUtilsFile.filesOf(base, filter.toFileFilter(base), false);
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
		return JakeUtilsFile.count(base, filter.toFileFilter(base), includeFolder);
	}

	@Override
	public String toString() {
		return base.getPath() + ":" + filter;
	}



}
