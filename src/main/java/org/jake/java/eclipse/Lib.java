package org.jake.java.eclipse;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.depmanagement.JakeDependencies;
import org.jake.java.build.JakeBuildJava;

class Lib {

	public final File file;

	public final Scope scope;

	public Lib(File file, Scope scope) {
		super();
		this.file = file;
		this.scope = scope;
	}

	@Override
	public String toString() {
		return scope + ":" + file.getPath();
	}

	public static JakeDependencies toDependencies(Iterable<Lib> libs) {
		final List<File> compileAndRuntimes = new LinkedList<File>();
		final List<File> compileOnlys = new LinkedList<File>();
		final List<File> runtimeOnlys = new LinkedList<File>();
		final List<File> testOnlys = new LinkedList<File>();
		for (final Lib lib : libs) {
			switch (lib.scope) {
			case COMPILE : compileAndRuntimes.add(lib.file); break;
			case PROVIDED : compileOnlys.add(lib.file); break;
			case RUNTIME : runtimeOnlys.add(lib.file); break;
			case TEST : testOnlys.add(lib.file); break;
			}
		}
		return JakeDependencies.builder()
				.forScopes(JakeBuildJava.COMPILE).onFiles(compileAndRuntimes)
				.forScopes(JakeBuildJava.RUNTIME).onFiles(runtimeOnlys)
				.forScopes(JakeBuildJava.TEST).onFiles(testOnlys)
				.forScopes(JakeBuildJava.PROVIDED).onFiles(compileOnlys).build();
	}


	public static final ScopeSegregator ALL_COMPILE = new ScopeSegregator() {

		@Override
		public Scope scopeOfLib(String path) {
			return Scope.COMPILE;
		}

		@Override
		public Scope scoprOfCon(String path) {
			return Scope.COMPILE;
		}

	};

	public static final ScopeSegregator SMART_LIB = new ScopeSegregator() {

		@Override
		public Scope scopeOfLib(String path) {
			if (path.toLowerCase().contains("test")) {
				return Scope.TEST;
			}
			if (path.toLowerCase().contains("lombok.jar")) {
				return Scope.PROVIDED;
			}
			if (path.toLowerCase().contains("junit")) {
				return Scope.TEST;
			}
			return Scope.COMPILE;
		}

		@Override
		public Scope scoprOfCon(String path) {
			if (path.startsWith("org.eclipse.jdt.junit.JUNIT_CONTAINER")) {
				return Scope.TEST;
			}
			return Scope.COMPILE;
		}
	};


	public enum Scope {
		COMPILE, TEST, RUNTIME, PROVIDED
	}


	public static interface ScopeSegregator {

		Scope scopeOfLib(String path);

		Scope scoprOfCon(String path);
	}


}
