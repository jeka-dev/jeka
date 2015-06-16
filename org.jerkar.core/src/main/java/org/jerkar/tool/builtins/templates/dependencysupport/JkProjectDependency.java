package org.jerkar.tool.builtins.templates.dependencysupport;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependency;
import org.jerkar.api.utils.JkUtilsFile;

public final class JkProjectDependency extends JkDependency {

	private static final long serialVersionUID = 1L;

	private final JkBuildDependencySupport projectBuild;

	private final Set<File> files;

	private JkProjectDependency(JkBuildDependencySupport projectBuild, Set<File> files) {
		super();
		this.projectBuild = projectBuild;
		this.files = Collections.unmodifiableSet(files);
	}

	public static JkProjectDependency of(JkBuildDependencySupport projectBuild, Set<File> files) {
		return new JkProjectDependency(projectBuild, new HashSet<File>(files));
	}

	public JkBuildDependencySupport projectBuild() {
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
		return projectBuild.moduleId() + " (" + this.projectBuild.getClass().getName() + ")";
	}

}