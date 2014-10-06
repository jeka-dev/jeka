package org.jake;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIterable;

/**
 * A set of {@link JakeDir}.
 * 
 * @author Jerome Angibaud
 */
public final class JakeDirSet implements Iterable<File> {

	private final List<JakeDir> jakeDirs;

	private JakeDirSet(List<JakeDir> dirs) {
		if (dirs == null) {
			throw new IllegalArgumentException("dirs can't be null.");
		}
		this.jakeDirs = Collections.unmodifiableList(dirs);
	}

	/**
	 * Creates a {@link JakeDirSet} from a sequence of {@link JakeDir}.
	 */
	public static final JakeDirSet of(Iterable<JakeDir> dirs) {
		return new JakeDirSet(JakeUtilsIterable.toList(dirs));
	}

	/**
	 * Creates an empty {@link JakeDirSet}.
	 */
	@SuppressWarnings("unchecked")
	public static final JakeDirSet empty() {
		return new JakeDirSet(Collections.EMPTY_LIST);
	}

	/**
	 * Creates a {@link JakeDirSet} from an array of {@link JakeDir}.
	 */
	public static final JakeDirSet of(JakeDir...dirViews) {
		return new JakeDirSet(Arrays.asList(dirViews));
	}

	/**
	 * Creates a {@link JakeDirSet} from an array of folder.
	 */
	public static final JakeDirSet of(File...folders) {
		final List<JakeDir> dirs = new ArrayList<JakeDir>(folders.length);
		for (final File folder : folders) {
			dirs.add(JakeDir.of(folder));
		}
		return new JakeDirSet(dirs);
	}

	/**
	 * Creates a {@link JakeDirSet} which is a concatenation of this {@link JakeDirSet} and
	 * the {@link JakeDir} array passed as parameter.
	 */
	public final JakeDirSet and(JakeDir ...dirViews) {
		final List<JakeDir> list = new LinkedList<JakeDir>(this.jakeDirs);
		list.addAll(Arrays.asList(dirViews));
		return new JakeDirSet(list);
	}

	/**
	 * Creates a {@link JakeDirSet} which is a concatenation of this {@link JakeDirSet} and
	 * the folder array passed as parameter.
	 */
	public final JakeDirSet and(File ...folders) {
		final List<JakeDir> dirs = new ArrayList<JakeDir>(folders.length);
		for (final File folder : folders) {
			dirs.add(JakeDir.of(folder));
		}
		return this.and(dirs.toArray(new JakeDir[folders.length]));
	}

	/**
	 * Creates a {@link JakeDirSet} which is a concatenation of this {@link JakeDirSet} and
	 * the {@link JakeDirSet} array passed as parameter.
	 */
	public final JakeDirSet and(JakeDirSet ...dirViewsList) {
		final List<JakeDir> list = new LinkedList<JakeDir>(this.jakeDirs);
		for (final JakeDirSet views : dirViewsList) {
			list.addAll(views.jakeDirs);
		}
		return new JakeDirSet(list);
	}


	@Override
	public Iterator<File> iterator() {
		return files().iterator();
	}

	/**
	 * Copies the files contained in this {@link JakeDirSet} to the specified directory.
	 */
	public int copyTo(File destinationDir) {
		if (destinationDir.exists()) {
			JakeUtilsFile.assertDir(destinationDir);
		} else {
			destinationDir.mkdirs();
		}
		int count = 0;
		for (final JakeDir dirView : jakeDirs) {
			if (dirView.exists()) {
				count += dirView.copyTo(destinationDir);
			}
		}
		return count;
	}

	/**
	 * Creates a {@link JakeDir} which is a copy of this {@link JakeDir} augmented
	 * with the specified {@link Filter#}
	 */
	public JakeDirSet andFilter(JakeFileFilter filter) {
		final List<JakeDir> list = new LinkedList<JakeDir>();
		for (final JakeDir dirView : this.jakeDirs) {
			list.add(dirView.andFilter(filter));
		}
		return new JakeDirSet(list);
	}

	/**
	 * Returns files contained in this {@link JakeDirSet} as a list of file.
	 */
	public List<File> files() {
		final LinkedList<File> result = new LinkedList<File>();
		for (final JakeDir dirView : this.jakeDirs) {
			if (dirView.root().exists()) {
				result.addAll(dirView.files());
			}
		}
		return result;
	}

	/**
	 * Returns path of each files file contained in this {@link JakeDirSet} relative to the
	 * root of their respective {@link JakeDir}.
	 */
	public List<String> relativePathes() {
		final LinkedList<String> result = new LinkedList<String>();
		for (final JakeDir dir : this.jakeDirs) {
			if (dir.root().exists()) {
				result.addAll(dir.relativePathes());
			}
		}
		return result;
	}

	/**
	 * Returns {@link JakeDir} instances constituting this {@link JakeDirSet}.
	 */
	public List<JakeDir> jakeDirs() {
		return jakeDirs;
	}

	/**
	 * Returns root of each {@link JakeDir} instances constituting this {@link JakeDirSet}.
	 */
	public List<File> roots() {
		final List<File> result = new LinkedList<File>();
		for(final JakeDir dirView : jakeDirs) {
			result.add(dirView.root());
		}
		return result;
	}

	/**
	 * Returns the number of files contained in this {@link JakeDirSet}.
	 */
	public int countFiles(boolean includeFolder) {
		int result = 0;
		for (final JakeDir dirView : jakeDirs) {
			result += dirView.fileCount(includeFolder);
		}
		return result;
	}

	/**
	 * Returns a {@link JakeZip} made of the files contained in this {@link JakeDirSet}.
	 */
	public JakeZip zip() {
		return JakeZip.of(this);
	}

	/**
	 * Returns <code>true</code> if each {@link JakeDir} constituting this {@link JakeDirSet} exist.
	 * 
	 * @see JakeDir#exists()
	 */
	public boolean allExists() {
		for (final JakeDir jakeDir : this.jakeDirs) {
			if (jakeDir.exists()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a the root of all {@link JakeDir} constituting this {@link JakeDirSet}.
	 */
	public List<File> rootDirs() {
		final List<File> result = new LinkedList<File>();
		for (final JakeDir dir : jakeDirs) {
			result.add(dir.root());
		}
		return result;
	}

	@Override
	public String toString() {
		return this.jakeDirs.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((jakeDirs == null) ? 0 : jakeDirs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final JakeDirSet other = (JakeDirSet) obj;
		if (jakeDirs == null) {
			if (other.jakeDirs != null) {
				return false;
			}
		} else if (!jakeDirs.equals(other.jakeDirs)) {
			return false;
		}
		return true;
	}



}
