package org.jake;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.jake.utils.FileUtils;

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
		this(base, Filter.none());
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
		return FileUtils.copyDir(base, destinationDir, filter.fileFilter(), true);
	}
	
	public int copyTo(DirView destinationDir) {
		return copyTo(destinationDir.getBase());
	}

	public File getBase() {
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

	public void asZip(File zipFile, int compressLevel) {
		FileUtils.zipDir(zipFile, compressLevel, base);
	}
	
	public DirView noFiltering() {
		return new DirView(base);
	}
	
	public boolean contains(File file) {
		if (!this.isAncestorOf(file)) {
			return false;
		}
		return this.filter.fileFilter().accept(file);
	}
	
	public boolean isAncestorOf(File file) {
		return FileUtils.isAncestor(base, file);
	}
	
	
	public DirView andFilter(Filter filter) {
		return new DirView(base, this.filter.combine(filter));
	}
	
	public List<File> fileList() {
		return FileUtils.flatten(base, filter.fileFilter(), false);
	}

	@Override
	public Iterator<File> iterator() {
		return fileList().iterator();
	}
	
	public void print(PrintStream printStream) {
		for (File file : this) {
			printStream.println(file.getPath());
		}
	}
	
	@Override
	public String toString() {
		return base.getPath() + ":" + filter;
	}
	
}
