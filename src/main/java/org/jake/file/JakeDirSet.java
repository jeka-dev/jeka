package org.jake.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;

import org.jake.utils.JakeUtilsIterable;

public final class JakeDirSet implements Iterable<JakeDir> {

	private final List<JakeDir> dirViews;

	private JakeDirSet(List<JakeDir> dirs) {
		if (dirs == null) {
			throw new NullPointerException("dirs can't be null.");
		}
		this.dirViews = Collections.unmodifiableList(dirs);
	}

	public static final JakeDirSet of(Iterable<JakeDir> dirs) {
		return new JakeDirSet(JakeUtilsIterable.toList(dirs));
	}

	@SuppressWarnings("unchecked")
	public static final JakeDirSet empty() {
		return new JakeDirSet(Collections.EMPTY_LIST);
	}


	public static final JakeDirSet of(JakeDir...dirViews) {
		return new JakeDirSet(Arrays.asList(dirViews));
	}

	public static final JakeDirSet of(File...folders) {
		final List<JakeDir> dirs = new ArrayList<JakeDir>(folders.length);
		for (final File folder : folders) {
			dirs.add(JakeDir.of(folder));
		}
		return new JakeDirSet(dirs);
	}

	public final JakeDirSet and(JakeDir ...dirViews) {
		final List<JakeDir> list = new LinkedList<JakeDir>(this.dirViews);
		list.addAll(Arrays.asList(dirViews));
		return new JakeDirSet(list);
	}

	public final JakeDirSet and(File ...folders) {
		final List<JakeDir> dirs = new ArrayList<JakeDir>(folders.length);
		for (final File folder : folders) {
			dirs.add(JakeDir.of(folder));
		}
		return this.and(dirs.toArray(new JakeDir[folders.length]));
	}

	public final JakeDirSet and(JakeDirSet ...dirViewsList) {
		final List<JakeDir> list = new LinkedList<JakeDir>(this.dirViews);
		for (final JakeDirSet views : dirViewsList) {
			list.addAll(views.dirViews);
		}
		return new JakeDirSet(list);
	}


	@Override
	public Iterator<JakeDir> iterator() {
		return dirViews.iterator();
	}

	public int copyTo(JakeDir destinationDir) {
		return this.copyTo(destinationDir.root());
	}

	public int copyTo(File destinationDir) {
		int count = 0;
		for (final JakeDir dirView : dirViews) {
			if (dirView.exists()) {
				count += dirView.copyTo(destinationDir);
			}
		}
		return count;
	}

	public JakeDirSet withFilter(JakeFileFilter filter) {
		final List<JakeDir> list = new LinkedList<JakeDir>();
		for (final JakeDir dirView : this.dirViews) {
			list.add(dirView.filter(filter));
		}
		return new JakeDirSet(list);
	}


	public List<File> listFiles() {
		final LinkedList<File> result = new LinkedList<File>();
		for (final JakeDir dirView : this.dirViews) {
			if (dirView.root().exists()) {
				result.addAll(dirView.listFiles());
			}
		}
		return result;
	}

	public List<JakeDir> listJakeDir() {
		return dirViews;
	}

	public List<File> listRoots() {
		final List<File> result = new LinkedList<File>();
		for(final JakeDir dirView : dirViews) {
			result.add(dirView.root());
		}
		return result;
	}

	public int countFiles(boolean includeFolder) {
		int result = 0;
		for (final JakeDir dirView : dirViews) {
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
	public static List<File> toFiles(JakeDirSet ...dirViewsList) {
		final List<File> result = new LinkedList<File>();
		for (final JakeDirSet dirViews : dirViewsList) {
			result.addAll(dirViews.listFiles());
		}
		return result;
	}

	@Override
	public String toString() {
		return this.dirViews.toString();
	}

	public boolean exist() {
		for (final JakeDir jakeDir : this) {
			if (jakeDir.exists()) {
				return true;
			}
		}
		return false;
	}

}
