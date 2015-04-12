package org.jerkar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIterable;

public final class JkBuildDependencies {

	public static JkBuildDependencies of(List<JkBuild> builds) {
		return new JkBuildDependencies(new ArrayList<JkBuild>(builds));
	}

	private final List<JkBuild> buildDeps;

	private List<JkBuild> resolvedTransitiveBuilds;

	private JkBuildDependencies(List<JkBuild> buildDeps) {
		super();
		this.buildDeps = Collections.unmodifiableList(buildDeps);
	}

	@SuppressWarnings("unchecked")
	public JkBuildDependencies and(List<JkBuild> builds) {
		return new JkBuildDependencies(JkUtilsIterable.concatLists(this.buildDeps, builds));
	}

	public List<JkBuild> transitiveBuilds() {
		if (resolvedTransitiveBuilds == null) {
			resolvedTransitiveBuilds = resolveTransitiveBuilds(new HashSet<File>());
		}
		return resolvedTransitiveBuilds;
	}

	public void invokeDoDefaultMethodOnAllSubProjects() {
		this.executeOnAllTransitive(JkUtilsIterable.listOf(BuildMethod.normal(CommandLine.DEFAULT_METHOD)));
	}

	public void invokeOnAllTransitive(String ...methods) {
		this.executeOnAllTransitive(BuildMethod.normals(methods));
	}

	void executeOnAllTransitive(Iterable<BuildMethod> methods) {
		for (final JkBuild build : transitiveBuilds()) {
			build.execute(methods);
		}
	}

	void activatePlugin(Class<? extends JkBuildPlugin> clazz, Map<String, String> options) {
		for (final JkBuild build : this.transitiveBuilds()) {
			build.plugins.addActivated(clazz, options);
		}
	}

	void configurePlugin(Class<? extends JkBuildPlugin> clazz, Map<String, String> options) {
		for (final JkBuild build : this.transitiveBuilds()) {
			build.plugins.addConfigured(clazz, options);
		}
	}

	private List<JkBuild> resolveTransitiveBuilds(Set<File> files) {
		final List<JkBuild> result = new LinkedList<JkBuild>();
		for (final JkBuild build : buildDeps) {
			final File dir = JkUtilsFile.canonicalFile(build.baseDir().root());
			if (!files.contains(dir)) {
				result.addAll(build.buildDependencies().resolveTransitiveBuilds(files));
				result.add(build);
				files.add(dir);
			}
		}
		return result;
	}


}
