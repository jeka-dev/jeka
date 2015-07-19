package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.tool.JkLocator;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

class Lib {

	private static final String CONTAINERS_PATH = "eclipse/containers";

	static final File CONTAINER_DIR = new File(JkLocator.jerkarHome(), CONTAINERS_PATH);

	static final File CONTAINER_USER_DIR = new File(JkLocator.jerkarUserHome(), CONTAINERS_PATH);

	public static Lib file(File file, JkScope scope, boolean exported) {
		return new Lib(file, null, scope, exported);
	}

	public static Lib project(String project, JkScope scope, boolean exported) {
		return new Lib(null, project, scope, exported);
	}

	public final File file;

	public final String projectRelativePath;

	public final JkScope scope;

	public final boolean exported;

	private Lib(File file, String projectRelativePath, JkScope scope, boolean exported) {
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

	public static JkDependencies toDependencies(JkJavaBuild masterBuild, Iterable<Lib> libs
			, ScopeResolver scopeSegregator) {
		final JkDependencies.Builder builder = JkDependencies.builder();
		for (final Lib lib : libs) {
			if (lib.projectRelativePath == null) {
				builder.on(lib.file).scope(lib.scope);

			} else {  // This is project dependency
				final JkJavaBuild slaveBuild = (JkJavaBuild) masterBuild.relativeProject(lib.projectRelativePath);
				final JkComputedDependency projectDependency = slaveBuild.asComputedDependency(slaveBuild.packer().jarFile());
				builder.on(projectDependency).scope(lib.scope);

				// Get the exported entry as well
				final JkBuildPluginEclipse pluginEclipse = slaveBuild.pluginOf(JkBuildPluginEclipse.class);
				if (pluginEclipse != null) {
					final File dotClasspathFile = slaveBuild.baseDir(".classpath");
					final DotClasspath dotClasspath = DotClasspath.from(dotClasspathFile);
					final List<Lib> sublibs = new ArrayList<Lib>();
					for (final Lib sublib : dotClasspath.libs(slaveBuild.baseDir().root(), scopeSegregator)) {
						if (sublib.exported) {
							sublibs.add(sublib);
						}
					}
					builder.on(Lib.toDependencies(slaveBuild, sublibs, scopeSegregator));
				}
			}
		}
		return builder.build();
	}




}
