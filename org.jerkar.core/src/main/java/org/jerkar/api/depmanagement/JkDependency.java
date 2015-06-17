package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Identifier for a dependency of a project. It can be a either : <ul>
 * <li>An external module as <code>org.hibernate:hibernate-core:3.0.+</code>,</li>
 * <li>A project inside a multi-project build,</li>
 * <li>Some files on the file system.</li>
 * </ul>
 * Each dependency is associated with a scope mapping to determine precisely in which scenario
 * the dependency is necessary.
 * 
 * @author Jerome Angibaud
 */
public abstract class JkDependency implements Serializable {

	private static final long serialVersionUID = 1L;

	public static boolean isGroupNameAndVersion(String candidate) {
		return JkUtilsString.countOccurence(candidate, ':') == 2;
	}

	/**
	 * Creates a {@link JkExternalModule} dependency with the specified version.
	 */
	public static JkExternalModule of(String groupAndNameAndVersion) {
		return JkExternalModule.of(groupAndNameAndVersion);
	}

	public static JkLocalDependency ofFile(File baseDir, String relativePath) {
		final File file = new File(relativePath);
		if (!file.isAbsolute()) {
			return JkLocalDependency.of(new File(baseDir, relativePath));
		}
		return JkLocalDependency.of(file);
	}

	public static JkLocalDependency of(Iterable<File> files) {
		return new JkLocalDependency(files);
	}

	public static JkLocalDependency of(File ... files) {
		return new JkLocalDependency(Arrays.asList(files));
	}

	/**
	 * A dependency on files located on file system.
	 */
	public static final class JkLocalDependency extends JkDependency implements JkLocalFileDependency {

		private static final long serialVersionUID = 1079527121988214989L;

		private final Set<File> files;

		private JkLocalDependency(Iterable<File> files) {
			this.files = Collections.unmodifiableSet(JkUtilsIterable.setOf(files));
		}

		@Override
		public final Set<File> files() {
			return files;
		}

		@Override
		public String toString() {
			return "Files=" + files.toString();
		}

	}

	public interface JkLocalFileDependency {

		Set<File> files();

	}

}
