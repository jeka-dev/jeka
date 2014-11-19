package org.jake.depmanagement;

import java.io.File;

import org.jake.utils.JakeUtilsIterable;

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

	/**
	 * Creates a {@link JakeExternalModule} dependency with the specified version.
	 */
	public static JakeExternalModule of(String groupAndNameAndVersion) {
		return JakeExternalModule.of(groupAndNameAndVersion);
	}

	/**
	 * A dependency on files located on file system.
	 */
	public static final class Files extends JakeDependency {

		private final Iterable<File> files;

		private Files(Iterable<File> files, JakeScopeMapping mapping) {
			this.files = JakeUtilsIterable.toList(files);
		}

	}

}
