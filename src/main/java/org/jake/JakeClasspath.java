package org.jake;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;

import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

/**
 * A sequence of file to be used as a <code>class path</code>.<br/>
 * Each file is called an <code>entry</code>.<br/>
 * Each entry is supposed to be either a <code>jar</code> file either a <code>folder</code>.<br/>
 * Non existing files are accepted as valid <code>entry</code>, though they won't contain any classes.
 * 
 * @author Djeang
 */
public final class JakeClasspath implements Iterable<File> {

	private static final String WILD_CARD = "*";

	private final List<File> entries;

	private JakeClasspath(Iterable<File> entries) {
		super();
		this.entries = Collections.unmodifiableList(resolveWildCard(entries));
	}

	public static JakeClasspath of(Iterable<File> entries) {
		return new JakeClasspath(entries);
	}

	/**
	 * Convenient method to create a <code>JakeClassLoader</code> from a given entry plus an sequence of other ones.
	 * This scheme is often used for launching some test suites.
	 */
	public static JakeClasspath of(File entry, Iterable<File> otherEntries) {
		return JakeClasspath.of(entry).and(otherEntries);
	}

	public static JakeClasspath of(File...entries) {
		return JakeClasspath.of(Arrays.asList(entries));
	}

	public JakeClasspath assertAllEntriesExist() {
		for (final File file : entries) {
			if (!file.exists()) {
				throw new IllegalStateException("File " + file.getAbsolutePath() + " does not exist.");
			}
		}
		return this;
	}

	/**
	 * Returns each entries making this <code>classpath</code>.
	 */
	public List<File> entries() {
		return entries;
	}

	/**
	 * Short hand for <code>entries().isEmpty()</code>.
	 */
	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/**
	 * @see #andAtHead(Iterable).
	 */
	public JakeClasspath andHead(File ...entries) {
		return andHead(JakeClasspath.of(entries));
	}

	/**
	 * Returns a <code>JakeClasspath</code> made of, in the order, the specified entries plus the entries of this one.
	 */
	@SuppressWarnings("unchecked")
	public JakeClasspath andHead(Iterable<File> otherEntries) {
		return new JakeClasspath(JakeUtilsIterable.chain(otherEntries, this.entries));
	}

	/**
	 * @see #and(Iterable).
	 */
	public JakeClasspath and(File ...files) {
		return and(JakeClasspath.of(files));
	}

	/**
	 * Returns a <code>JakeClasspath</code> made of, in the order,  the entries of this one plus the specified ones.
	 */
	@SuppressWarnings("unchecked")
	public JakeClasspath and(Iterable<File> otherFiles) {
		return new JakeClasspath(JakeUtilsIterable.chain(this.entries, otherFiles));
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		for (final Iterator<File> it = this.iterator(); it.hasNext() ;) {
			builder.append(it.next().getAbsolutePath());
			if (it.hasNext()) {
				builder.append(";");
			}
		}
		return builder.toString();
	}

	private static List<File> resolveWildCard(Iterable<File> files) {
		final List<File> result = new LinkedList<File>();
		for (final File file : files) {
			if (file.getName().equals(WILD_CARD)) {
				final File parent = file.getParentFile();
				if (!parent.exists()) {
					JakeLog.warn("File " + parent.getAbsolutePath() + " does not exist : classpath entry " + file.getAbsolutePath() + " will be ignored." );
				} else {
					result.addAll(JakeDir.of(parent).include("*.jar").files());
				}
			} else if (!file.exists()) {
				throw new IllegalArgumentException("Classpath element " + file.getAbsolutePath() + " does not exist.");
			} else if (file.isFile()) {
				if (!JakeUtilsString.endsWithAny(file.getName().toLowerCase(), ".jar", ".zip")) {
					throw new IllegalArgumentException("Classpath file element " + file.getAbsolutePath() + " is invalid. It must be either a folder either a jar or zip file.");
				}
				result.add(file);
			} else {
				result.add(file);
			}
		}
		return result;
	}

	@Override
	public Iterator<File> iterator() {
		return entries.iterator();
	}

	/**
	 * Returns the first entry of this <code>classpath</code> containing the given class.
	 */
	public File getEntryContainingClass(String className) {
		final String path = toFilePath(className);
		for (final File file : this) {
			if (file.isDirectory()) {
				if (new File(file, path).exists()) {
					return file;
				}
			} else {
				final ZipFile zipFile = JakeUtilsIO.newZipFile(file);
				if (zipFile.getEntry(path) != null) {
					JakeUtilsIO.closeQietly(zipFile);
					return file;
				}
				JakeUtilsIO.closeQietly(zipFile);
			}
		}
		return null;
	}

	static String toFilePath(String className) {
		return className.replace('.', '/').concat(".class");
	}



}


