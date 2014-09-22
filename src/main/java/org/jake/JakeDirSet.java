package org.jake;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;

import org.jake.utils.JakeUtilsIterable;

public final class JakeDirSet implements Iterable<File> {

	private final List<JakeDir> jakeDirs;

	private JakeDirSet(List<JakeDir> dirs) {
		if (dirs == null) {
			throw new IllegalArgumentException("dirs can't be null.");
		}
		this.jakeDirs = Collections.unmodifiableList(dirs);
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
		final List<JakeDir> list = new LinkedList<JakeDir>(this.jakeDirs);
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
		final List<JakeDir> list = new LinkedList<JakeDir>(this.jakeDirs);
		for (final JakeDirSet views : dirViewsList) {
			list.addAll(views.jakeDirs);
		}
		return new JakeDirSet(list);
	}


	@Override
	public Iterator<File> iterator() {
		return listFiles().iterator();
	}

	public int copyTo(JakeDir destinationDir) {
		return this.copyTo(destinationDir.root());
	}

	public int copyTo(File destinationDir) {
		int count = 0;
		for (final JakeDir dirView : jakeDirs) {
			if (dirView.exists()) {
				count += dirView.copyTo(destinationDir);
			}
		}
		return count;
	}

	public JakeDirSet withFilter(JakeFileFilter filter) {
		final List<JakeDir> list = new LinkedList<JakeDir>();
		for (final JakeDir dirView : this.jakeDirs) {
			list.add(dirView.withFilter(filter));
		}
		return new JakeDirSet(list);
	}


	public List<File> listFiles() {
		final LinkedList<File> result = new LinkedList<File>();
		for (final JakeDir dirView : this.jakeDirs) {
			if (dirView.root().exists()) {
				result.addAll(dirView.listFiles());
			}
		}
		return result;
	}

	public List<String> relativePathes() {
		final LinkedList<String> result = new LinkedList<String>();
		for (final JakeDir dir : this.jakeDirs) {
			if (dir.root().exists()) {
				result.addAll(dir.relativePathes());
			}
		}
		return result;
	}

	public List<JakeDir> listJakeDirs() {
		return jakeDirs;
	}

	public List<File> listRoots() {
		final List<File> result = new LinkedList<File>();
		for(final JakeDir dirView : jakeDirs) {
			result.add(dirView.root());
		}
		return result;
	}

	public int countFiles(boolean includeFolder) {
		int result = 0;
		for (final JakeDir dirView : jakeDirs) {
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



	@Override
	public String toString() {
		return this.jakeDirs.toString();
	}

	public boolean exist() {
		for (final JakeDir jakeDir : this.jakeDirs) {
			if (jakeDir.exists()) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Returns a List of all the base directories this set is made of.
	 */
	public List<File> baseDirs() {
		final List<File> result = new LinkedList<File>();
		for (final JakeDir dir : jakeDirs) {
			result.add(dir.root());
		}
		return result;
	}

}
