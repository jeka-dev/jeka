package org.jake;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.jake.utils.FileUtils;

/**
 * A directory that may be filtered or not. If there is no <code>Filter</code> than this stands 
 * simply for a directory. If a filter is defined on, than this stands for only a part of this directory, 
 * meaning that files or subfolder filtered are not part of this.
 * 
 * @author Jerome Angibaud
 */
public class DirView implements Iterable<File> {

	private final File base;
	
	private final Filter filter;

	public static DirView of(File base) {
		return new DirView(base);
	}
	
	private DirView(File base, Filter filter) {
		super();
		if (base.exists() && !base.isDirectory()) {
			throw new IllegalArgumentException(base + " is not a directory.");
		}
		this.base = base;
		this.filter = filter;
	}
	
	private DirView(File base) {
		this(base, Filter.ACCEPT_ALL);
	}

	public DirView relative(String relativePath) {
		File newBase = new File(base, relativePath);
		
		return new DirView(newBase);
	}
	
	public DirView createIfNotExist() {
		if (!base.exists() ) {
			base.mkdirs();
		}
		return this;
	}
	
	public File file(String relativePath) {
		return new File(base, relativePath);
	}
	
	public int copyTo(File destinationDir) {
		FileUtils.assertDir(destinationDir);
		return FileUtils.copyDir(base, destinationDir, filter.toFileFilter(base), true);
	}
	
	public int copyTo(DirView destinationDir) {
		return copyTo(destinationDir.root());
	}

	public File root() {
		return base;
	}
	
	public Filter getFilter() {
		return filter;
	}
	
	public boolean exists() {
		return base.exists();
	}
	
	public String path() {
		return base.getPath();
	}

	public void zip(File zipFile, int compressLevel) {
		FileUtils.zipDir(zipFile, compressLevel, base);
	}
	
	public DirView noFiltering() {
		return new DirView(base);
	}
	
	public boolean contains(File file) {
		if (!this.isAncestorOf(file)) {
			return false;
		}
		final String relativePath = FileUtils.getRelativePath(base, file);
		return this.filter.accept(relativePath);
	}
	
	public boolean isAncestorOf(File file) {
		return FileUtils.isAncestor(base, file);
	}
	
	
	public DirView filter(Filter filter) {
		if (this.filter == Filter.ACCEPT_ALL) {
			return new DirView(base, filter);
		}
		return new DirView(base, this.filter.and(filter));
	}
	
	public DirView include(String ... antPatterns) {
		return filter(Filter.include(antPatterns));
	}
	
	public DirView exclude(String ... antPatterns) {
		return filter(Filter.exclude(antPatterns));
	}
	
	public DirViews and(DirView dirView) {
		return DirViews.of(this, dirView);
	}
	
	public DirViews and(DirViews dirViews) {
		return DirViews.of(this).and(dirViews);
	}
	
	
	public List<File> listFiles() {
		if (!base.exists()) {
			throw new IllegalStateException("Folder " + base.getAbsolutePath() + " does nor exist.");
		}
		return FileUtils.filesOf(base, filter.toFileFilter(base), false);
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
		return FileUtils.count(base, filter.toFileFilter(base), includeFolder);
	}
	
	@Override
	public String toString() {
		return base.getPath() + ":" + filter;
	}
	
	
	
}
