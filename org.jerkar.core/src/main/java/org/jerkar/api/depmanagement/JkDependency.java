package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.builtins.templates.dependencysupport.JkBuildDependencySupport;
import org.jerkar.tool.builtins.templates.dependencysupport.JkProjectDependency;

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

	public static JkProjectDependency of(JkBuildDependencySupport build, File...files) {
		return JkProjectDependency.of(build, JkUtilsIterable.setOf(files));
	}


	/**
	 * A dependency on files located on file system.
	 */
	public static final class JkFilesDependency extends JkDependency {

		private static final long serialVersionUID = 1079527121988214989L;

		private final List<File> files;

		private JkFilesDependency(Iterable<File> files) {
			this.files = Collections.unmodifiableList(JkUtilsIterable.listOf(files));
		}

		public final List<File> files() {
			return files;
		}

		@Override
		public String toString() {
			return "Files=" + files.toString();
		}

	}

}
