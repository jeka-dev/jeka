package org.jerkar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIterable;

/**
 * A set of {@link JkDir}.
 * 
 * @author Jerome Angibaud
 */
public final class JkDirSet implements Iterable<File> {

	private final List<JkDir> jkDirs;

	private JkDirSet(List<JkDir> dirs) {
		if (dirs == null) {
			throw new IllegalArgumentException("dirs can't be null.");
		}
		this.jkDirs = Collections.unmodifiableList(dirs);
	}

	/**
	 * Creates a {@link JkDirSet} from a sequence of {@link JkDir}.
	 */
	public static final JkDirSet of(Iterable<JkDir> dirs) {
		return new JkDirSet(JkUtilsIterable.toList(dirs));
	}

	/**
	 * Creates an empty {@link JkDirSet}.
	 */
	@SuppressWarnings("unchecked")
	public static final JkDirSet empty() {
		return new JkDirSet(Collections.EMPTY_LIST);
	}

	/**
	 * Creates a {@link JkDirSet} from an array of {@link JkDir}.
	 */
	public static final JkDirSet of(JkDir...dirViews) {
		return new JkDirSet(Arrays.asList(dirViews));
	}

	/**
	 * Creates a {@link JkDirSet} from an array of folder.
	 */
	public static final JkDirSet of(File...folders) {
		final List<JkDir> dirs = new ArrayList<JkDir>(folders.length);
		for (final File folder : folders) {
			dirs.add(JkDir.of(folder));
		}
		return new JkDirSet(dirs);
	}

	/**
	 * Creates a {@link JkDirSet} which is a concatenation of this {@link JkDirSet} and
	 * the {@link JkDir} array passed as parameter.
	 */
	public final JkDirSet and(JkDir ...dirViews) {
		final List<JkDir> list = new LinkedList<JkDir>(this.jkDirs);
		list.addAll(Arrays.asList(dirViews));
		return new JkDirSet(list);
	}

	/**
	 * Creates a {@link JkDirSet} which is a concatenation of this {@link JkDirSet} and
	 * the folder array passed as parameter.
	 */
	public final JkDirSet and(File ...folders) {
		final List<JkDir> dirs = new ArrayList<JkDir>(folders.length);
		for (final File folder : folders) {
			dirs.add(JkDir.of(folder));
		}
		return this.and(dirs.toArray(new JkDir[folders.length]));
	}

	/**
	 * Creates a {@link JkDirSet} which is a concatenation of this {@link JkDirSet} and
	 * the {@link JkDirSet} array passed as parameter.
	 */
	public final JkDirSet and(JkDirSet ...otherDirSets) {
		final List<JkDir> list = new LinkedList<JkDir>(this.jkDirs);
		for (final JkDirSet otherDirSet : otherDirSets) {
			list.addAll(otherDirSet.jkDirs);
		}
		return new JkDirSet(list);
	}


	@Override
	public Iterator<File> iterator() {
		return files(false).iterator();
	}

	/**
	 * Copies the files contained in this {@link JkDirSet} to the specified directory.
	 */
	public int copyTo(File destinationDir) {
		if (destinationDir.exists()) {
			JkUtilsFile.assertDir(destinationDir);
		} else {
			destinationDir.mkdirs();
		}
		int count = 0;
		for (final JkDir dir : jkDirs) {
			if (dir.exists()) {
				count += dir.copyTo(destinationDir);
			}
		}
		return count;
	}

	/**
	 * Copies the files contained in this {@link JkDirSet} to the specified directory.
	 * While copying, tokens located between <code>${</code> and <code>}</code> are replaced by
	 * the specified value. <br/> For example, <code>my name is ${name}.</code> will be replaced
	 * by <code>my name is Jerome</code>, if the tokenValues passed as parameter holds an entry
	 * such <code>name=Jerome</code>.
	 * 
	 */
	public int copyRepacingTokens(File destinationDir, Map<String, String> tokenValues) {
		if (destinationDir.exists()) {
			JkUtilsFile.assertDir(destinationDir);
		} else {
			destinationDir.mkdirs();
		}
		int count = 0;
		for (final JkDir dir : jkDirs) {
			if (dir.exists()) {
				count += dir.copyReplacingTokens(destinationDir, tokenValues);
			}
		}
		return count;
	}

	/**
	 * Creates a {@link JkDir} which is a copy of this {@link JkDir} augmented
	 * with the specified {@link JkFileFilter}
	 */
	public JkDirSet andFilter(JkFileFilter filter) {
		final List<JkDir> list = new LinkedList<JkDir>();
		for (final JkDir dirView : this.jkDirs) {
			list.add(dirView.andFilter(filter));
		}
		return new JkDirSet(list);
	}

	/**
	 * Returns files contained in this {@link JkDirSet} as a list of file.
	 */
	public List<File> files(boolean includeFolders) {
		final LinkedList<File> result = new LinkedList<File>();
		for (final JkDir dirView : this.jkDirs) {
			if (dirView.root().exists()) {
				result.addAll(dirView.files(includeFolders));
			}
		}
		return result;
	}

	/**
	 * Returns path of each files file contained in this {@link JkDirSet} relative to the
	 * root of their respective {@link JkDir}.
	 */
	public List<String> relativePathes() {
		final LinkedList<String> result = new LinkedList<String>();
		for (final JkDir dir : this.jkDirs) {
			if (dir.root().exists()) {
				result.addAll(dir.relativePathes());
			}
		}
		return result;
	}

	/**
	 * Returns {@link JkDir} instances constituting this {@link JkDirSet}.
	 */
	public List<JkDir> jkDirs() {
		return jkDirs;
	}

	/**
	 * Returns root of each {@link JkDir} instances constituting this {@link JkDirSet}.
	 */
	public List<File> roots() {
		final List<File> result = new LinkedList<File>();
		for(final JkDir dirView : jkDirs) {
			result.add(dirView.root());
		}
		return result;
	}

	/**
	 * Returns the number of files contained in this {@link JkDirSet}.
	 */
	public int countFiles(boolean includeFolder) {
		int result = 0;
		for (final JkDir dirView : jkDirs) {
			result += dirView.fileCount(includeFolder);
		}
		return result;
	}

	/**
	 * Returns a {@link JkZipper} made of the files contained in this {@link JkDirSet}.
	 */
	public JkZipper zip() {
		return JkZipper.of(this);
	}

	/**
	 * Returns <code>true</code> if each {@link JkDir} constituting this {@link JkDirSet} exist.
	 * 
	 * @see JkDir#exists()
	 */
	public boolean allExists() {
		for (final JkDir jkDir : this.jkDirs) {
			if (jkDir.exists()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a the root of all {@link JkDir} constituting this {@link JkDirSet}.
	 */
	public List<File> rootDirs() {
		final List<File> result = new LinkedList<File>();
		for (final JkDir dir : jkDirs) {
			result.add(dir.root());
		}
		return result;
	}

	@Override
	public String toString() {
		return this.jkDirs.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((jkDirs == null) ? 0 : jkDirs.hashCode());
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
		final JkDirSet other = (JkDirSet) obj;
		if (jkDirs == null) {
			if (other.jkDirs != null) {
				return false;
			}
		} else if (!jkDirs.equals(other.jkDirs)) {
			return false;
		}
		return true;
	}



}
