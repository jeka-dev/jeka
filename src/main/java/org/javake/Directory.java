package org.javake;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;

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


	public List<File> allFilesEndingBy(final String... suffixes) {
		return allFiles(FileUtils.endingBy(suffixes));
	}

	public List<File> allFiles(FilenameFilter filter) {
		List<File> dirs = new LinkedList<File>();
		dirs.add(base);
		return filesOf(dirs, filter);
	}
	
	public void asZip(File zipFile, int compressLevel) {
		FileUtils.zipDir(zipFile, base, compressLevel);
	}
	
	

	private static List<File> filesOf(Iterable<File> dirs, FilenameFilter filter) {
		final List<File> result = new LinkedList<File>();
		for (File dir : dirs) {
			List<File> subDirs = new LinkedList<File>();
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					subDirs.add(file);
				} else if (filter == null || filter.accept(dir, file.getName())) {
					result.add(file);
				}
			}
			result.addAll(filesOf(subDirs, filter));
		}
		return result;
	}

}
