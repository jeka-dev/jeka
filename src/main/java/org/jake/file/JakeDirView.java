package org.jake.file;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.jake.file.utils.JakeUtilsFile;

/**
 * A directory that may be filtered or not. If there is no <code>Filter</code> than this stands 
 * simply for a directory. If a filter is defined on, than this stands for only a part of this directory, 
 * meaning that files or subfolder filtered are not part of this.
 * 
 * @author Jerome Angibaud
 */
public class JakeDirView implements Iterable<File> {

	private final File base;
	
	private final JakeFileFilter filter;

	public static JakeDirView of(File base) {
		return new JakeDirView(base);
	}
	
	private JakeDirView(File base, JakeFileFilter filter) {
		super();
		if (base.exists() && !base.isDirectory()) {
			throw new IllegalArgumentException(base + " is not a directory.");
		}
		this.base = base;
		this.filter = filter;
	}
	
	private JakeDirView(File base) {
		this(base, JakeFileFilter.ACCEPT_ALL);
	}

	public JakeDirView sub(String relativePath) {
		File newBase = new File(base, relativePath);
		
		return new JakeDirView(newBase);
	}
	
	public JakeDirView createIfNotExist() {
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
		return JakeUtilsFile.copyDir(base, destinationDir, filter.toFileFilter(base), true);
	}
	
	public int copyTo(JakeDirView destinationDir) {
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
	
	public String path() {
		return base.getPath();
	}

	public void zip(File zipFile, int compressLevel) {
		JakeUtilsFile.zipDir(zipFile, compressLevel, base);
	}
	
	public JakeDirView noFiltering() {
		return new JakeDirView(base);
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
	
	
	public JakeDirView filter(JakeFileFilter filter) {
		if (this.filter == JakeFileFilter.ACCEPT_ALL) {
			return new JakeDirView(base, filter);
		}
		return new JakeDirView(base, this.filter.and(filter));
	}
	
	public JakeDirView include(String ... antPatterns) {
		return filter(JakeFileFilter.include(antPatterns));
	}
	
	public JakeDirView exclude(String ... antPatterns) {
		return filter(JakeFileFilter.exclude(antPatterns));
	}
	
	public JakeDirViewSet and(JakeDirView dirView) {
		return JakeDirViewSet.of(this, dirView);
	}
	
	public JakeDirViewSet and(JakeDirViewSet dirViews) {
		return JakeDirViewSet.of(this).and(dirViews);
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
		for (File file : this) {
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
