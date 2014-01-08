package org.jake;

import java.io.File;
import java.io.FilenameFilter;

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

	public File getBase() {
		return base;
	}
	
	public FileSet fileSet() {
		return FileUtils.fileSetOf(getBase());
	}

	public void asZip(File zipFile, int compressLevel) {
		FileUtils.zipDir(zipFile, base, compressLevel);
	}
	
}
