package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependency.JkFileDependency;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsTime;

public class JkComputedDependency extends JkFileDependency {

	public static final JkComputedDependency of(final JkProcess process, File ...files) {
		final Set<File> fileSet = JkUtilsIterable.setOf(files);
		final Runnable runnable = new Runnable() {

			@Override
			public void run() {
				process.runSync();
			}
		};
		return new JkComputedDependency(runnable, fileSet);
	}

	public static final JkComputedDependency of(File file,  final JkJavaProcess process,
			final String className, final String ...args) {
		return of(JkUtilsIterable.setOf(file), process, className, args);
	}

	public static final JkComputedDependency of(Runnable runnable, File ...files) {
		final Set<File> fileSet = JkUtilsIterable.setOf(files);
		return new JkComputedDependency(runnable, fileSet);
	}


	public static final JkComputedDependency of(Set<File> files, final JkJavaProcess process,
			final String className, final String ...args) {
		final Set<File> fileSet = JkUtilsIterable.setOf(files);
		final Runnable runnable = new Runnable() {

			@Override
			public void run() {
				process.runClassSync(className, args);
			}
		};
		return new JkComputedDependency(runnable, fileSet);
	}

	private static final long serialVersionUID = 1L;

	private final Runnable runnable;

	private final Set<File> files;

	protected JkComputedDependency(Runnable runnable, Set<File> files) {
		super();
		this.runnable = runnable;
		this.files = files;
	}

	public final boolean hasMissingFilesOrEmptyDirs() {
		return !missingFilesOrEmptyDirs().isEmpty();
	}

	public final Set<File> missingFilesOrEmptyDirs() {
		final Set<File> files = new HashSet<File>();
		for (final File file : this.files) {
			if (!file.exists() || (file.isDirectory() && JkUtilsFile.filesOf(file, true).isEmpty())) {
				files.add(file);
			}
		}
		return files;
	}

	@Override
	public Set<File> files() {
		if (this.hasMissingFilesOrEmptyDirs()) {
			JkLog.delta(1);
			JkLog.infoHead("Building depending project " + this);
			final long time = System.nanoTime();
			runnable.run();
			JkLog.infoHead("Project " + this + " built in " + JkUtilsTime.durationInSeconds(time) +" seconds.");
			JkLog.delta(-1);
		}
		final Set<File> missingFiles = this.missingFilesOrEmptyDirs();
		if (!missingFiles.isEmpty()) {
			throw new IllegalStateException("Project " + this + " does not generate " + missingFiles);
		}
		return files;
	}

}
