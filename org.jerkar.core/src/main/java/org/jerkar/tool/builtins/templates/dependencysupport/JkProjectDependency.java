package org.jerkar.tool.builtins.templates.dependencysupport;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependency;
import org.jerkar.api.depmanagement.JkDependency.JkLocalFileDependency;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsTime;

public final class JkProjectDependency extends JkDependency implements JkLocalFileDependency {

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

	public static JkProjectDependency of(JkBuildDependencySupport projectBuild, File... files) {
		return of(projectBuild, new HashSet<File>(Arrays.asList(files)));
	}

	public JkBuildDependencySupport projectBuild() {
		return projectBuild;
	}

	@Override
	public Set<File> files() {
		if (this.hasMissingFilesOrEmptyDirs()) {
			JkLog.delta(1);
			JkLog.infoHead("Building depending project " + this);
			final long time = System.nanoTime();
			this.projectBuild.doDefault();
			JkLog.infoHead("Project " + this + " built in " + JkUtilsTime.durationInSeconds(time) +" seconds.");
			JkLog.delta(-1);
		}
		final Set<File> missingFiles = this.missingFilesOrEmptyDirs();
		if (!missingFiles.isEmpty()) {
			throw new IllegalStateException("Project " + this + " does not generate " + missingFiles);
		}
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