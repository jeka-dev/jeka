package org.jerkar.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * Holds information about inter-project dependencies in a multi-project context.
 * 
 * @author Jerome Angibaud
 */
public final class JkMultiProjectDependencies {

	static JkMultiProjectDependencies of(JkBuildDependencySupport master, List<JkBuild> builds) {
		return new JkMultiProjectDependencies(master, new ArrayList<JkBuild>(builds));
	}

	private final List<JkBuild> buildDeps;

	private List<JkBuild> resolvedTransitiveBuilds;

	private final JkBuildDependencySupport master;

	private JkMultiProjectDependencies(JkBuildDependencySupport master, List<JkBuild> buildDeps) {
		super();
		this.master = master;
		this.buildDeps = Collections.unmodifiableList(buildDeps);
	}

	@SuppressWarnings("unchecked")
	public JkMultiProjectDependencies and(List<JkBuild> builds) {
		return new JkMultiProjectDependencies(this.master, JkUtilsIterable.concatLists(this.buildDeps, builds));
	}

	public List<JkBuild> directProjectBuilds() {
		return Collections.unmodifiableList(buildDeps);
	}

	public List<JkBuild> transitiveProjectBuilds() {
		if (resolvedTransitiveBuilds == null) {
			resolvedTransitiveBuilds = resolveTransitiveBuilds(new HashSet<File>());
		}
		return resolvedTransitiveBuilds;
	}

	public void invokeDoDefaultMethodOnAllSubProjects() {
		this.invokeOnAllTransitive(JkConstants.DEFAULT_METHOD);
	}

	public void invokeOnAllTransitive(String ...methods) {
		this.executeOnAllTransitive(JkModelMethod.normals(methods));
	}

	private void executeOnAllTransitive(Iterable<JkModelMethod> methods) {
		JkLog.startln("Invoke " + methods + " on all dependents projects");
		for (final JkBuild build : transitiveProjectBuilds()) {
			build.execute(methods, this.master.baseDir().root());
		}
		JkLog.done("invoking " + methods + " on all dependents projects");
	}


	private List<JkBuild> resolveTransitiveBuilds(Set<File> files) {
		final List<JkBuild> result = new LinkedList<JkBuild>();
		for (final JkBuild build : buildDeps) {
			final File dir = JkUtilsFile.canonicalFile(build.baseDir().root());
			if (!files.contains(dir)) {
				if (build instanceof JkBuildDependencySupport) {
					final JkBuildDependencySupport buildDependencySupport =
							(JkBuildDependencySupport) build;
					result.addAll(buildDependencySupport.jerkarBuildDependencies()
							.resolveTransitiveBuilds(files));
				}
				result.add(build);
				files.add(dir);
			}
		}
		return result;
	}


}
