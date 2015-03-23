package org.jake.java.eclipse;

import java.io.File;

import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeScope;
import org.jake.java.build.JakeJavaBuild;

class Lib {

	public final File file;

	public final JakeScope scope;

	public Lib(File file, JakeScope scope) {
		super();
		this.file = file;
		this.scope = scope;
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
			if (path.toLowerCase().contains("test")) {
				return JakeJavaBuild.TEST;
			}
			if (path.toLowerCase().contains("lombok.jar")) {
				return JakeJavaBuild.PROVIDED;
			}
			if (path.toLowerCase().contains("junit")) {
				return JakeJavaBuild.TEST;
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
