package org.jake;

import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.utils.FileUtils;

/**
 * Mutable set of files. Exposes an convenient fluent interface to add, remove, filter
 * files from this set. 
 * The underlying Set is a {@link LinkedHashSet} so the order of iteration respects 
 * the insertion order.
 * 
 * @author Djeang.
 */
public class FileList implements Iterable<File> {
	
	private final Set<File> files;
	
	public static FileList of(Iterable<File> files) {
		return new FileList(files);
	}
	
	public static FileList of(DirViews dirViews) {
		final List<File> list = new LinkedList<File>();
		for (DirView dirView : dirViews) {
			list.addAll(dirView.listFiles());
		}
		return new FileList(list);
	}
	
	public static FileList empty() {
		return new FileList();
	}
	
	private FileList(Iterable<File> files) {
		this.files = new HashSet<File>();
		for (File file : files) {
			this.files.add(file);
		}
	} 
	
	@SuppressWarnings("unchecked")
	private FileList() {
		this(Collections.EMPTY_SET);
	}
	
	public int count() {
		return files.size();
	}
	
	public FileList addSingle(File file) {
		files.add(file);
		return this;
	}
	
	public FileList add(File ...filesToAdd) {
		for (File file : filesToAdd) {
			files.add(file);
		}
		return this;
	}
	
	public FileList add(Iterable<File> filesToAdd) {
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
	public FileList remove(File fileToRemove) {
		files.remove(fileToRemove);
		if (fileToRemove.isDirectory()) {
			for (Iterator<File> it = files.iterator(); it.hasNext();) {
				File file = it.next();
				if (FileUtils.isAncestor(fileToRemove, file)) {
					it.remove();
				}
			}
		}
		return this;
	}
	

	@Override
	public Iterator<File> iterator() {
		return files.iterator();
	}
	
	public void print(PrintStream printStream) {
		for (File file : files) {
			printStream.println(file.getPath().toCharArray() );
		}
	}
	

}
