package org.jake.file;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;

public class DirViews implements Iterable<DirView> {
	
	private final List<DirView> dirViews;
	
	private DirViews(List<DirView> dirViews) {
		this.dirViews = dirViews;
	}
	
	public static final DirViews of(DirView...dirViews) {
		return new DirViews(Arrays.asList(dirViews));
	}
	
	public final DirViews and(DirView ...dirViews) {
		List<DirView> list = new LinkedList<DirView>(this.dirViews);
		list.addAll(Arrays.asList(dirViews));
		return new DirViews(list);
	}
	
	public final DirViews and(DirViews ...dirViewsList) {
		List<DirView> list = new LinkedList<DirView>(this.dirViews);
		for (DirViews views : dirViewsList) {
			list.addAll(views.dirViews);
		}
		return new DirViews(list);
	}


	@Override
	public Iterator<DirView> iterator() {
		return dirViews.iterator();
	}
	
	public int copyTo(DirView destinationDir) {
		return this.copyTo(destinationDir.root());
	}
	
	public int copyTo(File destinationDir) {
		int count = 0;
		for (DirView dirView : dirViews) {
			if (dirView.exists()) {
				count += dirView.copyTo(destinationDir);
			}
		}
		return count;
	}
	
	public DirViews withFilter(Filter filter) {
		List<DirView> list = new LinkedList<DirView>();
		for (DirView dirView : this.dirViews) {
			list.add(dirView.filter(filter));
		}
		return new DirViews(list);
	}
	
	
	public List<File> listFiles() {
		final LinkedList<File> result = new LinkedList<File>();
		for (DirView dirView : this.dirViews) {
			if (dirView.root().exists()) {
				result.addAll(dirView.listFiles());
			}
		}
		return result;
	}
	
	public List<File> listRoots() {
		final List<File> result = new LinkedList<File>();
		for(DirView dirView : dirViews) {
			result.add(dirView.root());
		}
		return result;
	} 
	
	public int countFiles(boolean includeFolder) {
		int result = 0;
		for (DirView dirView : dirViews) {
			result += dirView.fileCount(includeFolder);
		}
		return result;
	}
	
	public void zip(File destFile, int zipLevel) {
		Zip.of(this).create(destFile, zipLevel);
	}
	
	public void zip(File destFile) {
		this.zip(destFile, Deflater.DEFAULT_COMPRESSION);;
	}
	
	/**
	 * Convenient method to list files over several <code>DirViews</code>.
	 */
	public static List<File> toFiles(DirViews ...dirViewsList) {
		final List<File> result = new LinkedList<File>();
		for (DirViews dirViews : dirViewsList) {
			result.addAll(dirViews.listFiles());
		}
		return result;
	}
	
}
