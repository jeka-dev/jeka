package org.jake;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jake.utils.JakeUtilsIterable;

/**
 * A sequence of file to be used as a <code>path</code>.<br/>
 * Each file is called an <code>entry</code>.<br/>
 * Instances of this class are immutable.
 * 
 * @author Djeang
 */
public final class JakePath implements Iterable<File> {

	private final List<File> entries;

	private JakePath(Iterable<File> entries) {
		super();
		this.entries = Collections.unmodifiableList(JakeUtilsIterable.toList(entries));
	}

	/**
	 * Creates a path from a sequence of files.
	 */
	public static JakePath of(Iterable<File> entries) {
		return new JakePath(entries);
	}

	/**
	 * Creates a path from aa array of files.
	 */
	public static JakePath of(File...entries) {
		return JakePath.of(Arrays.asList(entries));
	}

	/**
	 * Throws an {@link IllegalStateException} if at least one entry does not exist.
	 */
	public JakePath assertAllEntriesExist() throws IllegalStateException {
		for (final File file : entries) {
			if (!file.exists()) {
				throw new IllegalStateException("File " + file.getAbsolutePath() + " does not exist.");
			}
		}
		return this;
	}

	/**
	 * Returns the sequence of files as a list.
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
	public JakePath andHead(File ...entries) {
		return andHead(JakePath.of(entries));
	}

	/**
	 * Returns a <code>JakePath</code> made of, in the order, the specified entries plus the entries of this one.
	 */
	@SuppressWarnings("unchecked")
	public JakePath andHead(Iterable<File> otherEntries) {
		return new JakePath(JakeUtilsIterable.chain(otherEntries, this.entries));
	}

	/**
	 * @see #and(Iterable).
	 */
	public JakePath and(File ...files) {
		return and(JakePath.of(files));
	}

	/**
	 * Returns a <code>JakePath</code> made of, in the order,  the entries of this one plus the specified ones.
	 */
	@SuppressWarnings("unchecked")
	public JakePath and(Iterable<File> otherFiles) {
		return new JakePath(JakeUtilsIterable.chain(this.entries, otherFiles));
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

	@Override
	public Iterator<File> iterator() {
		return entries.iterator();
	}

}


