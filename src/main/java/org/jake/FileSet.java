package org.jake;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Mutable set of files. Exposes an convenient fluent interface to add, remove, filter
 * files from this set. 
 * The underlying Set is a {@link LinkedHashSet} so the order of iteration respects 
 * the insertion order.
 * 
 * @author Djeang.
 */
public class FileSet implements Iterable<File> {
	
	private final Set<File> files;
	
	public FileSet(Iterable<File> files) {
		this.files = new HashSet<File>();
		for (File file : files) {
			this.files.add(file);
		}
	} 
	
	@SuppressWarnings("unchecked")
	public FileSet() {
		this(Collections.EMPTY_SET);
	}
	
	public Set<File> asSet() {
		return Collections.unmodifiableSet(files);
	} 
	
	public FileSet addSingle(File file) {
		files.add(file);
		return this;
	}
	
	public FileSet add(File ...filesToAdd) {
		for (File file : filesToAdd) {
			files.add(file);
		}
		return this;
	}
	
	public FileSet add(Iterable<File> filesToAdd) {
		for (File file : filesToAdd) {
			files.add(file);
		}
		return this;
	}
	
	/**
	 * Removes the given file from this FileSet. If the given file is a directory,
	 * it removes recursively all children of this directory from this file set.
	 * @param fileToRemove
	 * @return
	 */
	public FileSet remove(File fileToRemove) {
		files.remove(fileToRemove);
		if (fileToRemove.isDirectory()) {
			for (Iterator<File> it = files.iterator(); it.hasNext();) {
				File file = it.next();
				if (FileUtils.isParent(fileToRemove, file)) {
					it.remove();
				}
			}
		}
		return this;
	}
	
	public FileSet retainOnly(FilenameFilter filter) {
		for (Iterator<File> it = this.iterator(); it.hasNext();) {
			File file = it.next();
			if(! filter.accept(file.getParentFile(), file.getName())) {
				it.remove();
			}
		}
		return this;
	}
	
	public FileSet retainsOnlyFilesEndingBy(final String... suffixes) {
		return retainOnly(FileUtils.endingBy(suffixes));
	}
	

	@Override
	public Iterator<File> iterator() {
		return files.iterator();
	}
	

}
