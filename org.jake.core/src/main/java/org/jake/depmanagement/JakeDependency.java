package org.jake.depmanagement;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jake.JakeBuild;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsString;

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
public abstract class JakeDependency {

	public static boolean isGroupNameAndVersion(String candidate) {
		return JakeUtilsString.countOccurence(candidate, ':') == 2;
	}

	/**
	 * Creates a {@link JakeExternalModule} dependency with the specified version.
	 */
	public static JakeExternalModule of(String groupAndNameAndVersion) {
		return JakeExternalModule.of(groupAndNameAndVersion);
	}

	public static JakeFilesDependency ofFile(File baseDir, String relativePath) {
		final File file = new File(relativePath);
		if (!file.isAbsolute()) {
			return JakeFilesDependency.of(new File(baseDir, relativePath));
		}
		return JakeFilesDependency.of(file);
	}

	public static JakeFilesDependency of(Iterable<File> files) {
		return new JakeFilesDependency(files);
	}

	public static JakeFilesDependency of(File ... files) {
		return new JakeFilesDependency(Arrays.asList(files));
	}

	public static JakeProjectDependency of(JakeBuild build, File...files) {
		return JakeProjectDependency.of(build, JakeUtilsIterable.setOf(files));
	}


	/**
	 * A dependency on files located on file system.
	 */
	public static final class JakeFilesDependency extends JakeDependency {

		private final List<File> files;

		private JakeFilesDependency(Iterable<File> files) {
			this.files = Collections.unmodifiableList(JakeUtilsIterable.toList(files));
		}

		public final List<File> files() {
			return files;
		}

		@Override
		public String toString() {
			return "Files=" + files.toString();
		}

	}

	public static final class JakeProjectDependency extends JakeDependency {

		private final JakeBuild projectBuild;

		private final Set<File> files;

		private JakeProjectDependency(JakeBuild projectBuild, Set<File> files) {
			super();
			this.projectBuild = projectBuild;
			this.files = Collections.unmodifiableSet(files);
		}

		public static JakeProjectDependency of(JakeBuild projectBuild, Set<File> files) {
			return new JakeProjectDependency(projectBuild, new HashSet<File>(files));
		}

		public JakeBuild projectBuild() {
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
				if (!file.exists() || (file.isDirectory() && JakeUtilsFile.filesOf(file, true).isEmpty())) {
					files.add(file);
				}
			}
			return files;
		}

		@Override
		public String toString() {
			return projectBuild.projectName() + " (" + this.projectBuild.getClass().getName() + ")";
		}

	}

}
