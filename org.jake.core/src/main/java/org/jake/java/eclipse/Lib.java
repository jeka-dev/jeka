package org.jake.java.eclipse;

import java.io.File;

import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeScope;
import org.jake.java.build.JakeJavaBuild;

class Lib {

	public static Lib file(File file, JakeScope scope) {
		return new Lib(file, null, scope);
	}

	public static Lib project(File project, JakeScope scope) {
		return new Lib(null, project, scope);
	}

	public final File file;

	public final File project;

	public final JakeScope scope;

	private Lib(File file, File project, JakeScope scope) {
		super();
		this.file = file;
		this.scope = scope;
		this.project = project;
	}


	@Override
	public String toString() {
		return scope + ":" + file.getPath();
	}

	public static JakeDependencies toDependencies(Iterable<Lib> libs) {
		final JakeDependencies.Builder builder = JakeDependencies.builder();
		for (final Lib lib : libs) {
			builder.onFile(lib.file).scope(lib.scope);
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
			final String parent = filePath.getParent();
			if (parent.equals(JakeJavaBuild.COMPILE.name())) {
				return JakeJavaBuild.COMPILE;
			}
			if (parent.equals(JakeJavaBuild.TEST.name())) {
				return JakeJavaBuild.TEST;
			}
			if (parent.equals(JakeJavaBuild.PROVIDED.name())) {
				return JakeJavaBuild.PROVIDED;
			}
			if (parent.equals(JakeJavaBuild.RUNTIME.name())) {
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
