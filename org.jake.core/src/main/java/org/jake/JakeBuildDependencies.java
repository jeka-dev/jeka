package org.jake;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public List<JakeBuild> transitiveBuilds() {
		if (resolvedTransitiveBuilds == null) {
			resolvedTransitiveBuilds = resolveTransitiveBuilds(new HashSet<BuildKey>());
		}
		return resolvedTransitiveBuilds;
	}

	public void invokeOnAllTransitiveBase() {
		this.invokeOnAllTransitive(BuildMethod.normal("base"));
	}

	public void invokeOnAllTransitive(String ...methods) {
		this.invokeOnAllTransitive(BuildMethod.normals(methods));
	}

	void invokeOnAllTransitive(BuildMethod... methods) {
		JakeLog.startln("Invoking methods " + Arrays.toString(methods) + " on " + buildNames(transitiveBuilds()));
		for (final JakeBuild build : transitiveBuilds()) {
			build.execute(Arrays.asList(methods));
		}
		JakeLog.done();
	}

	void activatePlugin(Class<? extends JakeBuildPlugin> clazz, Map<String, String> options) {
		for (final JakeBuild build : this.transitiveBuilds()) {
			build.plugins.addActivated(clazz, options);
		}
	}

	private static List<String> buildNames(List<JakeBuild> builds) {
		final List<String> result = new LinkedList<String>();
		for (final JakeBuild build : builds) {
			result.add(build.baseDir().root().getName());
		}
		return result;
	}

	private List<JakeBuild> resolveTransitiveBuilds(Set<BuildKey> keys) {
		final List<JakeBuild> result = new LinkedList<JakeBuild>();
		for (final JakeBuild build : buildDeps) {
			final BuildKey key = new BuildKey(build);
			if (!keys.contains(key)) {
				result.addAll(build.buildDependencies().resolveTransitiveBuilds(keys));
				result.add(build);
				keys.add(key);
			}
		}
		return result;
	}

	private static class BuildKey {

		private final Class<?> clazz;

		private final File file;

		public BuildKey(JakeBuild build) {
			super();
			this.clazz = build.getClass();
			this.file = build.baseDir().root();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final BuildKey other = (BuildKey) obj;
			if (clazz == null) {
				if (other.clazz != null) {
					return false;
				}
			} else if (!clazz.equals(other.clazz)) {
				return false;
			}
			if (file == null) {
				if (other.file != null) {
					return false;
				}
			} else if (!file.equals(other.file)) {
				return false;
			}
			return true;
		}

	}

}
