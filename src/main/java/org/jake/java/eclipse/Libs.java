package org.jake.java.eclipse;

import java.io.File;

class Lib {

	public static final ScopeSegregator ALL_COMPILE = new ScopeSegregator() {

		@Override
		public Scope scopeOf(String path) {
			return Scope.COMPILE;
		}
	};

	public static final ScopeSegregator SMART_LIB = new ScopeSegregator() {

		@Override
		public Scope scopeOf(String path) {
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
	};



	public enum Scope {
		COMPILE, TEST, RUNTIME, PROVIDED
	}

	public final File file;

	public final Scope scope;

	public Lib(File file, Scope scope) {
		super();
		this.file = file;
		this.scope = scope;
	}

	public static interface ScopeSegregator {
		Scope scopeOf(String path);
	}


}
