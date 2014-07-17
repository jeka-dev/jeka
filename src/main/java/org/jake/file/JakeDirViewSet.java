package org.jake.file;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;

public class JakeDirViewSet implements Iterable<JakeDirView> {
	
	private final List<JakeDirView> dirViews;
	
	private JakeDirViewSet(List<JakeDirView> dirViews) {
		this.dirViews = dirViews;
	}
	
	public static final JakeDirViewSet of(JakeDirView...dirViews) {
		return new JakeDirViewSet(Arrays.asList(dirViews));
	}
	
	public final JakeDirViewSet and(JakeDirView ...dirViews) {
		List<JakeDirView> list = new LinkedList<JakeDirView>(this.dirViews);
		list.addAll(Arrays.asList(dirViews));
		return new JakeDirViewSet(list);
	}
	
	public final JakeDirViewSet and(JakeDirViewSet ...dirViewsList) {
		List<JakeDirView> list = new LinkedList<JakeDirView>(this.dirViews);
		for (JakeDirViewSet views : dirViewsList) {
			list.addAll(views.dirViews);
		}
		return new JakeDirViewSet(list);
	}


	@Override
	public Iterator<JakeDirView> iterator() {
		return dirViews.iterator();
	}
	
	public int copyTo(JakeDirView destinationDir) {
		return this.copyTo(destinationDir.root());
	}
	
	public int copyTo(File destinationDir) {
		int count = 0;
		for (JakeDirView dirView : dirViews) {
			if (dirView.exists()) {
				count += dirView.copyTo(destinationDir);
			}
		}
		return count;
	}
	
	public JakeDirViewSet withFilter(JakeFileFilter filter) {
		List<JakeDirView> list = new LinkedList<JakeDirView>();
		for (JakeDirView dirView : this.dirViews) {
			list.add(dirView.filter(filter));
		}
		return new JakeDirViewSet(list);
	}
	
	
	public List<File> listFiles() {
		final LinkedList<File> result = new LinkedList<File>();
		for (JakeDirView dirView : this.dirViews) {
			if (dirView.root().exists()) {
				result.addAll(dirView.listFiles());
			}
		}
		return result;
	}
	
	public List<File> listRoots() {
		final List<File> result = new LinkedList<File>();
		for(JakeDirView dirView : dirViews) {
			result.add(dirView.root());
		}
		return result;
	} 
	
	public int countFiles(boolean includeFolder) {
		int result = 0;
		for (JakeDirView dirView : dirViews) {
			result += dirView.fileCount(includeFolder);
		}
		return result;
	}
	
	public void zip(File destFile, int zipLevel) {
		JakeZip.of(this).create(destFile, zipLevel);
	}
	
	public void zip(File destFile) {
		this.zip(destFile, Deflater.DEFAULT_COMPRESSION);;
	}
	
	/**
	 * Convenient method to list files over several <code>DirViews</code>.
	 */
	public static List<File> toFiles(JakeDirViewSet ...dirViewsList) {
		final List<File> result = new LinkedList<File>();
		for (JakeDirViewSet dirViews : dirViewsList) {
			result.addAll(dirViews.listFiles());
		}
		return result;
	}
	
}
