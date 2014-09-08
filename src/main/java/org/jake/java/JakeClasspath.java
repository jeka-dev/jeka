package org.jake.java;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;

import org.jake.file.JakeDir;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

public class JakeClasspath implements Iterable<File> {

	private static final String WILD_CARD = "*";

	private final List<File> files;

	private JakeClasspath(Iterable<File> files) {
		super();
		this.files = Collections.unmodifiableList(resolveWildCard(files));
	}

	public static JakeClasspath of(Iterable<File> files) {
		return new JakeClasspath(files);
	}

	public static JakeClasspath of(File file, Iterable<File> files) {
		return JakeClasspath.of(file).and(files);
	}

	public static JakeClasspath of(File...files) {
		return JakeClasspath.of(Arrays.asList(files));
	}

	public List<File> files() {
		return files;
	}

	public boolean isEmpty() {
		return files.isEmpty();
	}

	public JakeClasspath andAtFirst(File ...files) {
		return andAtFirst(JakeClasspath.of(files));
	}

	@SuppressWarnings("unchecked")
	public JakeClasspath andAtFirst(Iterable<File> otherFiles) {
		return new JakeClasspath(JakeUtilsIterable.chain(otherFiles, this.files));
	}

	public JakeClasspath and(File ...files) {
		return and(JakeClasspath.of(files));
	}

	@SuppressWarnings("unchecked")
	public JakeClasspath and(Iterable<File> otherFiles) {
		return new JakeClasspath(JakeUtilsIterable.chain(this.files, otherFiles));
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
					throw new IllegalArgumentException("Classpath element defined as "
							+ file.getName() + " is invalid : " + parent.getAbsolutePath() + " does not exist.");
				}
				result.addAll(JakeDir.of(parent).include("*.jar").listFiles());
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
		return files.iterator();
	}

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
