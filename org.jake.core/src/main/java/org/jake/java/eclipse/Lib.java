package org.jake.java.eclipse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jake.JakeLocator;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeScope;
import org.jake.java.build.JakeJavaBuild;

class Lib {

	private static final String CONTAINERS_PATH = "eclipse/containers";

	static final File CONTAINER_DIR = new File(JakeLocator.jakeHome(), CONTAINERS_PATH);

	static final File CONTAINER_USER_DIR = new File(JakeLocator.jakeUserHome(), CONTAINERS_PATH);

	public static Lib file(File file, JakeScope scope, boolean exported) {
		return new Lib(file, null, scope, exported);
	}

	public static Lib project(String project, JakeScope scope, boolean exported) {
		return new Lib(null, project, scope, exported);
	}

	public final File file;

	public final String projectRelativePath;

	public final JakeScope scope;

	public final boolean exported;

	private Lib(File file, String projectRelativePath, JakeScope scope, boolean exported) {
		super();
		this.file = file;
		this.scope = scope;
		this.projectRelativePath = projectRelativePath;
		this.exported = exported;
	}


	@Override
	public String toString() {
		return scope + ":" + file.getPath();
	}

	public static JakeDependencies toDependencies(JakeEclipseBuild masterBuild, Iterable<Lib> libs
			, ScopeSegregator scopeSegregator) {
		final JakeDependencies.Builder builder = JakeDependencies.builder();
		for (final Lib lib : libs) {
			if (lib.projectRelativePath == null) {
				builder.onFile(lib.file).scope(lib.scope);

			} else {  // This is project dependency
				final JakeJavaBuild slaveBuild = (JakeJavaBuild) masterBuild.relativeProject(lib.projectRelativePath);
				builder.onProject(slaveBuild, slaveBuild.packer().jarFile()).scope(lib.scope);

				// Get the exported entry as well
				if (slaveBuild instanceof JakeEclipseBuild) {
					final JakeEclipseBuild eclipseBuild = (JakeEclipseBuild) slaveBuild;
					final File dotClasspathFile = slaveBuild.baseDir(".classpath");
					final DotClasspath dotClasspath = DotClasspath.from(dotClasspathFile);
					final List<Lib> sublibs = new ArrayList<Lib>();
					for (final Lib sublib : dotClasspath.libs(slaveBuild.baseDir().root(), scopeSegregator)) {
						if (sublib.exported) {
							sublibs.add(sublib);
						}
					}
					builder.on(Lib.toDependencies(eclipseBuild, sublibs, scopeSegregator));
				}
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
			if (path.contains("org.eclipse.jdt.junit.JUNIT_CONTAINER")) {
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
