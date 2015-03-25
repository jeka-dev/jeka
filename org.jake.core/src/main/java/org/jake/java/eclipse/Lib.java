package org.jake.java.eclipse;

import java.io.File;

import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeScope;
import org.jake.java.build.JakeJavaBuild;

class Lib {

	public static Lib file(File file, JakeScope scope) {
		return new Lib(file, null, scope);
	}

	public static Lib project(String project, JakeScope scope) {
		return new Lib(null, project, scope);
	}



	public final File file;

	public final String projectRelativePath;

	public final JakeScope scope;

	private Lib(File file, String projectRelativePath, JakeScope scope) {
		super();
		this.file = file;
		this.scope = scope;
		this.projectRelativePath = projectRelativePath;
	}


	@Override
	public String toString() {
		return scope + ":" + file.getPath();
	}

	public static JakeDependencies toDependencies(JakeEclipseBuild masterBuild, Iterable<Lib> libs) {
		final JakeDependencies.Builder builder = JakeDependencies.builder();
		for (final Lib lib : libs) {
			if (lib.projectRelativePath == null) {
				builder.onFile(lib.file).scope(lib.scope);
			} else {
				final JakeJavaBuild slaveBuild = (JakeJavaBuild) masterBuild.relativeProject(lib.projectRelativePath);
				builder.onProject(slaveBuild, slaveBuild.packer().jarFile()).scope(lib.scope);
			}
		}
		return builder.build();
	}


	public static final ScopeSegregator ALL_COMPILE = new ScopeSegregator() {

		@Override
		public JakeScope scopeOfLib(String path) {
			return JakeJavaBuild.COMPILE;
		}

		@Override
		public JakeScope scopeOfCon(String path) {
			return JakeJavaBuild.COMPILE;
		}

	};

	public static final ScopeSegregator SMART_LIB = new ScopeSegregator() {

		@Override
		public JakeScope scopeOfLib(String path) {
			final File filePath = new File(path);
			final String parent = filePath.getParentFile().getName();
			if (parent.equalsIgnoreCase(JakeJavaBuild.COMPILE.name())) {
				return JakeJavaBuild.COMPILE;
			}
			if (parent.equalsIgnoreCase(JakeJavaBuild.TEST.name())) {
				return JakeJavaBuild.TEST;
			}
			if (parent.equalsIgnoreCase(JakeJavaBuild.PROVIDED.name())) {
				return JakeJavaBuild.PROVIDED;
			}
			if (parent.equalsIgnoreCase(JakeJavaBuild.RUNTIME.name())) {
				return JakeJavaBuild.RUNTIME;
			}
			return JakeJavaBuild.COMPILE;
		}

		@Override
		public JakeScope scopeOfCon(String path) {
			if (path.startsWith("org.eclipse.jdt.junit.JUNIT_CONTAINER")) {
				return JakeJavaBuild.TEST;
			}
			return JakeJavaBuild.COMPILE;
		}
	};


	public static interface ScopeSegregator {

		JakeScope scopeOfLib(String path);

		JakeScope scopeOfCon(String path);
	}


}
