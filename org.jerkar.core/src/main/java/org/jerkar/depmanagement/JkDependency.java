package org.jerkar.depmanagement;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jerkar.JkBuild;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIterable;
import org.jerkar.utils.JkUtilsString;

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
public abstract class JkDependency {

	public static boolean isGroupNameAndVersion(String candidate) {
		return JkUtilsString.countOccurence(candidate, ':') == 2;
	}

	/**
	 * Creates a {@link JkExternalModule} dependency with the specified version.
	 */
	public static JkExternalModule of(String groupAndNameAndVersion) {
		return JkExternalModule.of(groupAndNameAndVersion);
	}

	public static JkFilesDependency ofFile(File baseDir, String relativePath) {
		final File file = new File(relativePath);
		if (!file.isAbsolute()) {
			return JkFilesDependency.of(new File(baseDir, relativePath));
		}
		return JkFilesDependency.of(file);
	}

	public static JkFilesDependency of(Iterable<File> files) {
		return new JkFilesDependency(files);
	}

	public static JkFilesDependency of(File ... files) {
		return new JkFilesDependency(Arrays.asList(files));
	}

	public static JkProjectDependency of(JkBuild build, File...files) {
		return JkProjectDependency.of(build, JkUtilsIterable.setOf(files));
	}


	/**
	 * A dependency on files located on file system.
	 */
	public static final class JkFilesDependency extends JkDependency {

		private final List<File> files;

		private JkFilesDependency(Iterable<File> files) {
			this.files = Collections.unmodifiableList(JkUtilsIterable.toList(files));
		}

		public final List<File> files() {
			return files;
		}

		@Override
		public String toString() {
			return "Files=" + files.toString();
		}

	}

	public static final class JkProjectDependency extends JkDependency {

		private final JkBuild projectBuild;

		private final Set<File> files;

		private JkProjectDependency(JkBuild projectBuild, Set<File> files) {
			super();
			this.projectBuild = projectBuild;
			this.files = Collections.unmodifiableSet(files);
		}

		public static JkProjectDependency of(JkBuild projectBuild, Set<File> files) {
			return new JkProjectDependency(projectBuild, new HashSet<File>(files));
		}

		public JkBuild projectBuild() {
			return projectBuild;
		}

		public Set<File> files() {
			return files;
		}

		public boolean hasMissingFilesOrEmptyDirs() {
			return !missingFilesOrEmptyDirs().isEmpty();
		}

		public Set<File> missingFilesOrEmptyDirs() {
			final Set<File> files = new HashSet<File>();
			for (final File file : this.files) {
				if (!file.exists() || (file.isDirectory() && JkUtilsFile.filesOf(file, true).isEmpty())) {
					files.add(file);
				}
			}
			return files;
		}

		@Override
		public String toString() {
			return projectBuild.projectId() + " (" + this.projectBuild.getClass().getName() + ")";
		}

	}

}
