package org.jake;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIterable;

public final class JakeBuildDependencies {

	public static JakeBuildDependencies of(List<JakeBuild> builds) {
		return new JakeBuildDependencies(new ArrayList<JakeBuild>(builds));
	}

	private final List<JakeBuild> buildDeps;

	private List<JakeBuild> resolvedTransitiveBuilds;

	private JakeBuildDependencies(List<JakeBuild> buildDeps) {
		super();
		this.buildDeps = Collections.unmodifiableList(buildDeps);
	}

	@SuppressWarnings("unchecked")
	public JakeBuildDependencies and(List<JakeBuild> builds) {
		return new JakeBuildDependencies(JakeUtilsIterable.concatLists(this.buildDeps, builds));
	}

	public List<JakeBuild> transitiveBuilds() {
		if (resolvedTransitiveBuilds == null) {
			resolvedTransitiveBuilds = resolveTransitiveBuilds(new HashSet<File>());
		}
		return resolvedTransitiveBuilds;
	}

	public void invokeBaseOnAllSubProjects() {
		this.executeOnAllTransitive(JakeUtilsIterable.listOf(BuildMethod.normal("base")));
	}

	public void invokeOnAllTransitive(String ...methods) {
		this.executeOnAllTransitive(BuildMethod.normals(methods));
	}

	void executeOnAllTransitive(Iterable<BuildMethod> methods) {
		for (final JakeBuild build : transitiveBuilds()) {
			build.execute(methods);
		}
	}

	void activatePlugin(Class<? extends JakeBuildPlugin> clazz, Map<String, String> options) {
		for (final JakeBuild build : this.transitiveBuilds()) {
			build.plugins.addActivated(clazz, options);
		}
	}

	void configurePlugin(Class<? extends JakeBuildPlugin> clazz, Map<String, String> options) {
		for (final JakeBuild build : this.transitiveBuilds()) {
			build.plugins.addConfigured(clazz, options);
		}
	}

	private List<JakeBuild> resolveTransitiveBuilds(Set<File> files) {
		final List<JakeBuild> result = new LinkedList<JakeBuild>();
		for (final JakeBuild build : buildDeps) {
			final File dir = JakeUtilsFile.canonicalFile(build.baseDir().root());
			if (!files.contains(dir)) {
				result.addAll(build.buildDependencies().resolveTransitiveBuilds(files));
				result.add(build);
				files.add(dir);
			}
		}
		return result;
	}


}
