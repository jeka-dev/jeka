package org.jake;

import java.io.File;
import java.io.FilenameFilter;

import org.jake.utils.FileUtils;

public class Directory {

	private final File base;

	public Directory(File base) {
		super();
		if (base.exists() && !base.isDirectory()) {
			throw new IllegalArgumentException(base + " is not a directory.");
		}
		this.base = base;
	}

	public Directory relative(String relativePath, boolean createIfAbsent) {
		File newBase = new File(base, relativePath);
		if (!newBase.exists() && createIfAbsent) {
			newBase.mkdirs();
		}
		return new Directory(newBase);
	}
	
	public File file(String relativePath) {
		return new File(base, relativePath);
	}
	
	public int copyTo(File destinationDir, FilenameFilter filter) {
		FileUtils.assertDir(destinationDir);
		return FileUtils.copyDir(base, destinationDir, filter);
	}
	
	public int copyTo(Directory destinationDir, FilenameFilter filter) {
		return copyTo(destinationDir.getBase(), filter);
	}

	public File getBase() {
		return base;
	}
	
	public boolean exists() {
		return base.exists();
	}
	
	public String path() {
		return base.getPath();
	}
	
	public FileSet fileSet() {
		return new FileSet(FileUtils.filesOf(getBase()));
	}

	public void asZip(File zipFile, int compressLevel) {
		FileUtils.zipDir(zipFile, compressLevel, base);
	}
	
}
