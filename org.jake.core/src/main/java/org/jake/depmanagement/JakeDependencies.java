package org.jake.depmanagement;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.JakeBuild;
import org.jake.JakeLog;
import org.jake.depmanagement.JakeDependency.JakeFilesDependency;
import org.jake.depmanagement.JakeDependency.JakeProjectDependency;
import org.jake.depmanagement.JakeScopedDependency.ScopeType;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsTime;

/**
 * A set of {@link JakeScopedDependency} generally standing for the entire dependencies of a project/module.
 * 
 * @author Jerome Angibaud.
 */
public class JakeDependencies implements Iterable<JakeScopedDependency> {

	public static JakeDependencies on(JakeScopedDependency... scopedDependencies) {
		return new JakeDependencies(Arrays.asList(scopedDependencies));
	}

	public static JakeDependencies on(JakeScope scope, JakeDependency ... dependencies) {
		final List<JakeScopedDependency> list = new LinkedList<JakeScopedDependency>();
		for (final JakeDependency dependency : dependencies) {
			final JakeScopedDependency scopedDependency = JakeScopedDependency.of(dependency, scope);
			list.add(scopedDependency);
		}
		return new JakeDependencies(list);
	}

	public static JakeDependencies onProject(JakeScope scope, JakeBuild build, File...files) {
		return on(scope, JakeDependency.of(build, files));
	}

	private final List<JakeScopedDependency> dependencies;

	private JakeDependencies(List<JakeScopedDependency> dependencies) {
		super();
		this.dependencies = Collections.unmodifiableList(new LinkedList<JakeScopedDependency>(dependencies));
	}

	/**
	 * Returns <code>true</code> if this object contains no dependency.
	 */
	public boolean isEmpty() {
		return dependencies.isEmpty();
	}

	/**
	 * Returns a clone of this object minus the dependencies on the given {@link JakeModuleId}.
	 */
	public JakeDependencies without(JakeModuleId jakeModuleId) {
		final List<JakeScopedDependency> result = new LinkedList<JakeScopedDependency>(dependencies);
		for (final Iterator<JakeScopedDependency> it = result.iterator(); it.hasNext();) {
			final JakeDependency dependency = it.next().dependency();
			if (dependency instanceof JakeExternalModule) {
				final JakeExternalModule externalModule = (JakeExternalModule) dependency;
				if (externalModule.moduleId().equals(jakeModuleId)) {
					it.remove();
				}
			}
		}
		return new JakeDependencies(result);
	}

	/**
	 * Returns a clone of this object plus the specified {@link JakeScopedDependency}s.
	 */
	public JakeDependencies and(Iterable<JakeScopedDependency> others) {
		if (!others.iterator().hasNext()) {
			return this;
		}
		return JakeDependencies.builder().on(this).on(others).build();
	}



	/**
	 * Returns a clone of this object plus {@link JakeScopedDependency}s on the specified file.
	 */
	public JakeDependencies andFiles(JakeScope scope, File ...files) {
		final JakeScopedDependency scopedDependency = JakeScopedDependency.of(JakeDependency.of(files), scope);
		return and(scopedDependency);
	}

	/**
	 * Returns a clone of this object plus {@link JakeScopedDependency}s on the specified project.
	 */
	public JakeDependencies andProject(JakeScope scope, JakeBuild project, File ...files) {
		final JakeScopedDependency scopedDependency = JakeScopedDependency.of(JakeDependency.of(project, files), scope);
		return and(scopedDependency);
	}

	/**
	 * Returns a clone of this object plus {@link JakeScopedDependency}s on the specified external module.
	 * @param versionedModuleId something like "org.apache:commons:1.4"
	 */
	public JakeDependencies andExternal(JakeScope scope, String versionedModuleId) {
		final JakeDependency dependency = JakeDependency.of(versionedModuleId);
		final JakeScopedDependency scopedDependency = JakeScopedDependency.of(dependency, scope);
		return and(scopedDependency);
	}



	/**
	 * Returns a clone of this object plus the specified {@link JakeScopedDependency}s.
	 */
	public JakeDependencies and(JakeScopedDependency... others) {
		return and(Arrays.asList(others));
	}

	/**
	 * Returns <code>true</code> if this object contains dependencies whose are {@link JakeExternalModule}.
	 */
	public boolean containsExternalModule() {
		for (final JakeScopedDependency scopedDependency : dependencies) {
			if (scopedDependency.dependency() instanceof JakeExternalModule) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<JakeScopedDependency> iterator() {
		return dependencies.iterator();
	}

	@Override
	public String toString() {
		return dependencies.toString();
	}

	/**
	 * Returns the set of {@link JakeDependency} involved for the specified {@link JakeScope}.
	 */
	public Set<JakeDependency> dependenciesDeclaredWith(JakeScope scope) {
		final Set<JakeDependency> dependencies = new HashSet<JakeDependency>();
		for (final JakeScopedDependency scopedDependency : this) {

			if (scopedDependency.scopeType().equals(ScopeType.SIMPLE) && scopedDependency.scopes().contains(scope)) {
				dependencies.add(scopedDependency.dependency());
			} else if (scopedDependency.scopeType().equals(ScopeType.MAPPED)
					&& scopedDependency.scopeMapping().entries().contains(scope)) {
				dependencies.add(scopedDependency.dependency());
			}

		}
		return dependencies;
	}

	/**
	 * Returns the {@link JakeScopedDependency} declared for the specified {@link JakeModuleId}.
	 * Returns <code>null</code> if no dependency on this module exists in this object.
	 */
	public JakeScopedDependency get(JakeModuleId moduleId) {
		for (final JakeScopedDependency scopedDependency : this) {
			final JakeDependency dependency = scopedDependency.dependency();
			if (dependency instanceof JakeExternalModule) {
				final JakeExternalModule externalModule = (JakeExternalModule) dependency;
				if (externalModule.moduleId().equals(moduleId)) {
					return scopedDependency;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the set of scopes involved in these dependencies.
	 */
	public Set<JakeScope> moduleScopes() {
		final Set<JakeScope> result = new HashSet<JakeScope>();
		for (final JakeScopedDependency dep : this.dependencies) {
			if (dep.scopeType() == ScopeType.MAPPED) {
				result.addAll(dep.scopeMapping().entries());
			} else if (dep.scopeType() == ScopeType.SIMPLE) {
				result.addAll(dep.scopes());
			}
		}
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Returns <code>true</code> if this object contains dependency on external module whose rely
	 * on dynamic version. It can be either dynamic version has defined by Ivy (as "1.3.+", "[1.0, 2.0[" ,...)
	 * or snapshot version as defined in Maven (as "1.0-SNAPSHOT).
	 */
	public boolean hasDynamicVersions() {
		for (final JakeScopedDependency scopedDependency : this) {
			if (scopedDependency.dependency() instanceof JakeExternalModule) {
				final JakeExternalModule externalModule = (JakeExternalModule) scopedDependency.dependency();
				final JakeVersionRange versionRange = externalModule.versionRange();
				if (versionRange.isDynamic()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if this object contains dependency on external module whose rely
	 * on dynamic version that are resolvable (Maven Snapshot versions are dynamic but not resolvable).
	 * It only stands for dynamic versions has defined by Ivy (as "1.3.+", "[1.0, 2.0[" ,...).
	 * If so, when resolving, dynamic versions are replaced by fixed resolved ones.
	 */
	public boolean hasDynamicAndResovableVersions() {
		for (final JakeScopedDependency scopedDependency : this) {
			if (scopedDependency.dependency() instanceof JakeExternalModule) {
				final JakeExternalModule externalModule = (JakeExternalModule) scopedDependency.dependency();
				final JakeVersionRange versionRange = externalModule.versionRange();
				if (versionRange.isDynamicAndResovable()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Convenient method to resolve using {@link JakeArtifact}s instead of {@link JakeVersionedModule}.
	 * 
	 * @see #resolvedWith(Iterable)
	 */
	public JakeDependencies resolvedWithArtifacts(Iterable<JakeArtifact> artifacts) {
		final List<JakeVersionedModule> list = new LinkedList<JakeVersionedModule>();
		for (final JakeArtifact artifact : artifacts) {
			list.add(artifact.versionedModule());
		}
		return resolvedWith(list);
	}

	/**
	 * Creates a clone of these dependencies replacing the dynamic versions by the static ones specified
	 * in the {@link JakeVersionedModule}s passed as argument. <br/>
	 */
	public JakeDependencies resolvedWith(Iterable<JakeVersionedModule> resolvedModules) {
		return resolvedWith(toModuleVersionMap(resolvedModules));
	}

	/**
	 * @see #toModuleVersionMap(Iterable)
	 */
	public JakeDependencies resolvedWith(Map<JakeModuleId, JakeVersion> map) {
		JakeDependencies result = this;
		for (final JakeModuleId moduleId : map.keySet()) {
			final JakeScopedDependency scopedDependency = this.get(moduleId);
			if (scopedDependency == null) {
				continue;
			}
			final JakeExternalModule externalModule = (JakeExternalModule) scopedDependency.dependency();
			if (externalModule.versionRange().isDynamicAndResovable()) {
				final JakeVersion resolvedVersion = map.get(moduleId);
				if (resolvedVersion != null) {
					final JakeExternalModule resolvedModule = externalModule.resolvedTo(resolvedVersion);
					final JakeScopedDependency resolvedScopedDep = scopedDependency.dependency(resolvedModule);
					result = result.without(moduleId).and(resolvedScopedDep);
				}
			}
		}
		return result;
	}

	/**
	 * Returns all files declared as {@link JakeFilesDependency} for the specified scope.
	 */
	public List<File> fileDependencies(JakeScope jakeScope) {
		final LinkedHashSet<File> set = new LinkedHashSet<File>();
		for (final JakeScopedDependency scopedDependency : this.dependencies) {
			if (scopedDependency.isInvolvedIn(jakeScope)
					&& scopedDependency.dependency() instanceof JakeFilesDependency) {
				final JakeFilesDependency fileDeps = (JakeFilesDependency) scopedDependency.dependency();
				set.addAll(fileDeps.files());
			}
		}
		return new LinkedList<File>(set);
	}

	public List<File> projectDependencies(JakeScope jakeScope) {
		final LinkedHashSet<File> set = new LinkedHashSet<File>();
		for (final JakeScopedDependency scopedDependency : this.dependencies) {
			if (scopedDependency.isInvolvedIn(jakeScope)
					&& scopedDependency.dependency() instanceof JakeProjectDependency) {
				final JakeProjectDependency projectDeps = (JakeProjectDependency) scopedDependency.dependency();
				if (projectDeps.hasMissingFilesOrEmptyDirs()) {
					JakeLog.shift(1);
					JakeLog.displayHead("Building depending project " + projectDeps);
					final long time = System.nanoTime();
					projectDeps.projectBuild().base();
					JakeLog.displayHead("Project " + projectDeps + " built in " + JakeUtilsTime.durationInSeconds(time) +" seconds.");
					JakeLog.shift(-1);
				}
				final Set<File> missingFiles = projectDeps.missingFilesOrEmptyDirs();
				if (!missingFiles.isEmpty()) {
					throw new IllegalStateException("Project " + projectDeps + " does not generate " + missingFiles);
				}
				set.addAll(projectDeps.files());
			}
		}
		return new LinkedList<File>(set);
	}

	private static Map<JakeModuleId, JakeVersion> toModuleVersionMap(Iterable<JakeVersionedModule> resolvedModules) {
		final Map<JakeModuleId, JakeVersion> result = new HashMap<JakeModuleId, JakeVersion>();
		for (final JakeVersionedModule versionedModule : resolvedModules) {
			result.put(versionedModule.moduleId(), versionedModule.version());
		}
		return result;
	}


	public static Builder builder() {
		return new Builder(new LinkedList<JakeScopedDependency>());
	}

	public static class Builder {

		protected final LinkedList<JakeScopedDependency> dependencies;

		protected Set<JakeScope> defaultScopes;

		protected JakeScopeMapping defaultMapping;

		protected Builder(LinkedList<JakeScopedDependency> dependencies) {
			super();
			this.dependencies = dependencies;
		}

		public Builder usingDefaultScopes(JakeScope ...scopes) {
			if (scopes.length == 0) {
				throw new IllegalArgumentException("You must specify at least one scope.");
			}
			this.defaultScopes = JakeUtilsIterable.setOf(scopes);
			this.defaultMapping = null;
			return this;
		}

		public Builder usingDefaultScopeMapping(JakeScopeMapping scopeMapping) {
			this.defaultMapping = scopeMapping;
			this.defaultScopes = null;
			return this;
		}

		public Builder resetDefaultScope() {
			defaultScopes = null;
			defaultMapping = null;
			return this;
		}

		public ScopeableBuilder on(JakeDependency dependency) {
			final JakeScopedDependency scopedDependency;
			if (defaultScopes != null) {
				scopedDependency = JakeScopedDependency.of(dependency, defaultScopes);
			} else if (defaultMapping != null) {
				scopedDependency = JakeScopedDependency.of(dependency, defaultMapping);
			} else {
				scopedDependency = JakeScopedDependency.of(dependency);
			}
			dependencies.add(scopedDependency);
			if (this instanceof ScopeableBuilder) {
				return (ScopeableBuilder) this;
			}
			return new ScopeableBuilder(this);
		}

		public Builder on(JakeScopedDependency dependency) {
			this.dependencies.add(dependency);
			return this;
		}

		public ScopeableBuilder onFile(File file) {
			return on(JakeFilesDependency.of(JakeUtilsIterable.listOf(file)));
		}

		public ScopeableBuilder onFiles(Iterable<File> files) {
			ScopeableBuilder builder = new ScopeableBuilder(this);
			for (final File file : files) {
				builder = builder.onFile(file);
			}
			return builder;
		}


		public ScopeableBuilder on(JakeModuleId module, JakeVersionRange version) {
			return on(module, version, true);
		}

		public ScopeableBuilder on(JakeModuleId module, JakeVersionRange version, boolean transitive) {
			return on(JakeExternalModule.of(module, version).transitive(transitive));
		}

		public ScopeableBuilder on(String organisation, String name, String version) {
			return on(organisation, name, version, true);
		}

		public ScopeableBuilder on(String organisation, String name, String version, boolean transitive) {
			return on(JakeExternalModule.of(organisation, name, version).transitive(transitive));
		}

		public ScopeableBuilder on(String description) {
			return on(description, true);
		}

		public ScopeableBuilder onProject(JakeBuild projectBuild, File ...files) {
			return on(JakeProjectDependency.of(projectBuild, JakeUtilsIterable.setOf(files)));
		}

		public ScopeableBuilder on(String description, boolean transitive) {
			return on(JakeExternalModule.of(description).transitive(transitive));
		}

		public Builder on(Iterable<JakeScopedDependency> dependencies) {
			if (!dependencies.iterator().hasNext()) {
				return this;
			}
			for (final JakeScopedDependency dependency : dependencies) {
				this.dependencies.add(dependency);
			}
			return this;
		}

		public JakeDependencies build() {
			return new JakeDependencies(dependencies);
		}

		public static class ScopeableBuilder extends Builder {

			private ScopeableBuilder(Builder builder) {
				super(builder.dependencies);
				this.defaultMapping = builder.defaultMapping;
				this.defaultScopes = builder.defaultScopes;
			}

			public Builder scope(JakeScopeMapping scopeMapping) {
				final JakeDependency dependency = dependencies.pollLast().dependency();
				dependencies.add(JakeScopedDependency.of(dependency, scopeMapping));
				return this;
			}

			public Builder scope(JakeScope ... scopes) {
				final JakeDependency dependency = dependencies.pollLast().dependency();
				dependencies.add(JakeScopedDependency.of(dependency, JakeUtilsIterable.setOf(scopes)));
				return this;
			}

			public AfterMapScopeBuilder mapScope(JakeScope ... scopes) {
				return new AfterMapScopeBuilder(dependencies, JakeUtilsIterable.setOf(scopes) );
			}

			public static class AfterMapScopeBuilder  {

				private final LinkedList<JakeScopedDependency> dependencies;

				private final Iterable<JakeScope> from;

				private AfterMapScopeBuilder(LinkedList<JakeScopedDependency> dependencies, Iterable<JakeScope> from) {
					this.dependencies = dependencies;
					this.from = from;
				}

				public AfterToBuilder to(JakeScope... jakeScopes) {
					final JakeScopedDependency dependency = dependencies.pollLast();
					final JakeScopeMapping mapping;
					if (dependency.scopeType() == JakeScopedDependency.ScopeType.UNSET) {
						mapping = JakeScopeMapping.of(from).to(jakeScopes);
					}  else {
						mapping = dependency.scopeMapping().and(from).to(jakeScopes);
					}
					dependencies.add(JakeScopedDependency.of(dependency.dependency(), mapping));
					return new AfterToBuilder(dependencies);
				}

				public AfterToBuilder to(String... scopeNames) {
					final JakeScope[] scopes = new JakeScope[scopeNames.length];
					for (int i = 0; i < scopeNames.length; i++) {
						scopes[i] = JakeScope.of(scopeNames[i]);
					}
					return to(scopes);
				}

			}

			public static class AfterToBuilder extends Builder {

				private AfterToBuilder(
						LinkedList<JakeScopedDependency> dependencies) {
					super(dependencies);
				}

				public AfterMapScopeBuilder and(JakeScope ...scopes) {
					return new AfterMapScopeBuilder(dependencies, Arrays.asList(scopes));
				}

			}

		}

	}

}
