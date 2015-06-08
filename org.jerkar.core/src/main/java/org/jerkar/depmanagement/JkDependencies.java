package org.jerkar.depmanagement;

import java.io.File;
import java.io.Serializable;
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

import org.jerkar.JkLog;
import org.jerkar.depmanagement.JkDependency.JkFilesDependency;
import org.jerkar.depmanagement.JkDependency.JkProjectDependency;
import org.jerkar.depmanagement.JkScopedDependency.ScopeType;
import org.jerkar.file.JkPath;
import org.jerkar.utils.JkUtilsIterable;
import org.jerkar.utils.JkUtilsTime;

/**
 * A set of {@link JkScopedDependency} generally standing for the entire dependencies of a project/module.
 * 
 * @author Jerome Angibaud.
 */
public class JkDependencies implements Iterable<JkScopedDependency>, Serializable {

	private static final long serialVersionUID = 1L;

	public static JkDependencies on(JkScopedDependency... scopedDependencies) {
		return new JkDependencies(Arrays.asList(scopedDependencies));
	}

	public static JkDependencies on(JkScope scope, JkDependency ... dependencies) {
		final List<JkScopedDependency> list = new LinkedList<JkScopedDependency>();
		for (final JkDependency dependency : dependencies) {
			final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, scope);
			list.add(scopedDependency);
		}
		return new JkDependencies(list);
	}

	public static JkDependencies onProject(JkScope scope, JkBuildDependencySupport build, File...files) {
		return on(scope, JkDependency.of(build, files));
	}

	private final List<JkScopedDependency> dependencies;

	private JkDependencies(List<JkScopedDependency> dependencies) {
		super();
		this.dependencies = Collections.unmodifiableList(new LinkedList<JkScopedDependency>(dependencies));
	}

	/**
	 * Returns <code>true</code> if this object contains no dependency.
	 */
	public boolean isEmpty() {
		return dependencies.isEmpty();
	}

	/**
	 * Returns a clone of this object minus the dependencies on the given {@link JkModuleId}.
	 */
	public JkDependencies without(JkModuleId jkModuleId) {
		final List<JkScopedDependency> result = new LinkedList<JkScopedDependency>(dependencies);
		for (final Iterator<JkScopedDependency> it = result.iterator(); it.hasNext();) {
			final JkDependency dependency = it.next().dependency();
			if (dependency instanceof JkExternalModule) {
				final JkExternalModule externalModule = (JkExternalModule) dependency;
				if (externalModule.moduleId().equals(jkModuleId)) {
					it.remove();
				}
			}
		}
		return new JkDependencies(result);
	}

	public JkDependencies withDefaultScope(JkScope ...scopes) {
		final List<JkScopedDependency> list = new LinkedList<JkScopedDependency>();
		for (JkScopedDependency dep : this) {
			if (dep.scopeType().equals(ScopeType.UNSET) ||
					(dep.scopeType().equals(ScopeType.SIMPLE) && dep.scopes().isEmpty())) {
				dep = dep.withScopes(scopes);
			}
			list.add(dep);
		}
		return new JkDependencies(list);
	}

	/**
	 * Returns a clone of this object plus the specified {@link JkScopedDependency}s.
	 */
	public JkDependencies and(Iterable<JkScopedDependency> others) {
		if (!others.iterator().hasNext()) {
			return this;
		}
		return JkDependencies.builder().on(this).on(others).build();
	}



	/**
	 * Returns a clone of this object plus {@link JkScopedDependency}s on the specified file.
	 */
	public JkDependencies andFiles(JkScope scope, File ...files) {
		final JkScopedDependency scopedDependency = JkScopedDependency.of(JkDependency.of(files), scope);
		return and(scopedDependency);
	}

	/**
	 * Returns a clone of this object plus {@link JkScopedDependency}s on the specified project.
	 */
	public JkDependencies andProject(JkScope scope, JkBuildDependencySupport project, File ...files) {
		final JkScopedDependency scopedDependency = JkScopedDependency.of(JkDependency.of(project, files), scope);
		return and(scopedDependency);
	}

	/**
	 * Returns a clone of this object plus {@link JkScopedDependency}s on the specified external module.
	 * @param versionedModuleId something like "org.apache:commons:1.4"
	 */
	public JkDependencies andExternal(JkScope scope, String versionedModuleId) {
		final JkDependency dependency = JkDependency.of(versionedModuleId);
		final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, scope);
		return and(scopedDependency);
	}



	/**
	 * Returns a clone of this object plus the specified {@link JkScopedDependency}s.
	 */
	public JkDependencies and(JkScopedDependency... others) {
		return and(Arrays.asList(others));
	}

	/**
	 * Returns <code>true</code> if this object contains dependencies whose are {@link JkExternalModule}.
	 */
	public boolean containsExternalModule() {
		for (final JkScopedDependency scopedDependency : dependencies) {
			if (scopedDependency.dependency() instanceof JkExternalModule) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<JkScopedDependency> iterator() {
		return dependencies.iterator();
	}

	@Override
	public String toString() {
		return dependencies.toString();
	}

	/**
	 * Returns the set of {@link JkDependency} involved for the specified {@link JkScope}.
	 */
	public Set<JkDependency> dependenciesDeclaredWith(JkScope scope) {
		final Set<JkDependency> dependencies = new HashSet<JkDependency>();
		for (final JkScopedDependency scopedDependency : this) {

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
	 * Returns the {@link JkScopedDependency} declared for the specified {@link JkModuleId}.
	 * Returns <code>null</code> if no dependency on this module exists in this object.
	 */
	public JkScopedDependency get(JkModuleId moduleId) {
		for (final JkScopedDependency scopedDependency : this) {
			final JkDependency dependency = scopedDependency.dependency();
			if (dependency instanceof JkExternalModule) {
				final JkExternalModule externalModule = (JkExternalModule) dependency;
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
	public Set<JkScope> declaredScopes() {
		final Set<JkScope> result = new HashSet<JkScope>();
		for (final JkScopedDependency dep : this.dependencies) {
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
		for (final JkScopedDependency scopedDependency : this) {
			if (scopedDependency.dependency() instanceof JkExternalModule) {
				final JkExternalModule externalModule = (JkExternalModule) scopedDependency.dependency();
				final JkVersionRange versionRange = externalModule.versionRange();
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
		for (final JkScopedDependency scopedDependency : this) {
			if (scopedDependency.dependency() instanceof JkExternalModule) {
				final JkExternalModule externalModule = (JkExternalModule) scopedDependency.dependency();
				final JkVersionRange versionRange = externalModule.versionRange();
				if (versionRange.isDynamicAndResovable()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Convenient method to resolve using {@link JkArtifact}s instead of {@link JkVersionedModule}.
	 * 
	 * @see #resolvedWith(Iterable)
	 */
	public JkDependencies resolvedWithArtifacts(Iterable<JkArtifact> artifacts) {
		final List<JkVersionedModule> list = new LinkedList<JkVersionedModule>();
		for (final JkArtifact artifact : artifacts) {
			list.add(artifact.versionedModule());
		}
		return resolvedWith(list);
	}

	/**
	 * Creates a clone of these dependencies replacing the dynamic versions by the static ones specified
	 * in the {@link JkVersionedModule}s passed as argument. <br/>
	 */
	public JkDependencies resolvedWith(Iterable<JkVersionedModule> resolvedModules) {
		return resolvedWith(toModuleVersionMap(resolvedModules));
	}

	/**
	 * @see #toModuleVersionMap(Iterable)
	 */
	public JkDependencies resolvedWith(Map<JkModuleId, JkVersion> map) {
		JkDependencies result = this;
		for (final JkModuleId moduleId : map.keySet()) {
			final JkScopedDependency scopedDependency = this.get(moduleId);
			if (scopedDependency == null) {
				continue;
			}
			final JkExternalModule externalModule = (JkExternalModule) scopedDependency.dependency();
			if (externalModule.versionRange().isDynamicAndResovable()) {
				final JkVersion resolvedVersion = map.get(moduleId);
				if (resolvedVersion != null) {
					final JkExternalModule resolvedModule = externalModule.resolvedTo(resolvedVersion);
					final JkScopedDependency resolvedScopedDep = scopedDependency.dependency(resolvedModule);
					result = result.without(moduleId).and(resolvedScopedDep);
				}
			}
		}
		return result;
	}

	/**
	 * Returns all files declared as {@link JkFilesDependency} for any of the specified scopes.
	 */
	public JkPath fileDependencies(JkScope ...scopes) {
		final LinkedHashSet<File> set = new LinkedHashSet<File>();
		for (final JkScopedDependency scopedDependency : this.dependencies) {
			if (scopedDependency.isInvolvedInAnyOf(scopes)
					&& scopedDependency.dependency() instanceof JkFilesDependency) {
				final JkFilesDependency fileDeps = (JkFilesDependency) scopedDependency.dependency();
				set.addAll(fileDeps.files());
			}
		}
		return JkPath.of(set);
	}


	public List<File> projectDependencies(JkScope jkScope) {
		final LinkedHashSet<File> set = new LinkedHashSet<File>();
		for (final JkScopedDependency scopedDependency : this.dependencies) {
			if (scopedDependency.isInvolvedIn(jkScope)
					&& scopedDependency.dependency() instanceof JkProjectDependency) {
				final JkProjectDependency projectDeps = (JkProjectDependency) scopedDependency.dependency();
				if (projectDeps.hasMissingFilesOrEmptyDirs()) {
					JkLog.offset(1);
					JkLog.infoHead("Building depending project " + projectDeps);
					final long time = System.nanoTime();
					projectDeps.projectBuild().doDefault();
					JkLog.infoHead("Project " + projectDeps + " built in " + JkUtilsTime.durationInSeconds(time) +" seconds.");
					JkLog.offset(-1);
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

	private static Map<JkModuleId, JkVersion> toModuleVersionMap(Iterable<JkVersionedModule> resolvedModules) {
		final Map<JkModuleId, JkVersion> result = new HashMap<JkModuleId, JkVersion>();
		for (final JkVersionedModule versionedModule : resolvedModules) {
			result.put(versionedModule.moduleId(), versionedModule.version());
		}
		return result;
	}

	/**
	 * Returns all build included in these dependencies.
	 * The builds are coming from {@link JkProjectDependency}.
	 */
	public List<JkBuildDependencySupport> projectDependencies() {
		final List<JkBuildDependencySupport> result = new LinkedList<JkBuildDependencySupport>();
		for (final JkScopedDependency scopedDependency : this.dependencies) {
			if (scopedDependency.dependency() instanceof JkProjectDependency) {
				final JkProjectDependency projectDependency = (JkProjectDependency) scopedDependency.dependency();
				result.add(projectDependency.projectBuild());
			}
		}
		return result;
	}


	public static Builder builder() {
		return new Builder(new LinkedList<JkScopedDependency>());
	}

	public static class Builder {

		protected final LinkedList<JkScopedDependency> dependencies;

		protected Set<JkScope> defaultScopes;

		protected JkScopeMapping defaultMapping;

		protected Builder(LinkedList<JkScopedDependency> dependencies) {
			super();
			this.dependencies = dependencies;
		}

		public Builder usingDefaultScopes(JkScope ...scopes) {
			if (scopes.length == 0) {
				throw new IllegalArgumentException("You must specify at least one scope.");
			}
			this.defaultScopes = JkUtilsIterable.setOf(scopes);
			this.defaultMapping = null;
			return this;
		}

		public Builder usingDefaultScopeMapping(JkScopeMapping scopeMapping) {
			this.defaultMapping = scopeMapping;
			this.defaultScopes = null;
			return this;
		}

		public Builder resetDefaultScope() {
			defaultScopes = null;
			defaultMapping = null;
			return this;
		}

		public ScopeableBuilder on(JkDependency dependency) {
			if (dependency instanceof JkFilesDependency) {
				final JkFilesDependency fileDeps = (JkFilesDependency) dependency;
				if (fileDeps.files().isEmpty()) {
					return new ScopeableBuilder(this);
				}
			}
			final JkScopedDependency scopedDependency;
			if (defaultScopes != null) {
				scopedDependency = JkScopedDependency.of(dependency, defaultScopes);
			} else if (defaultMapping != null) {
				scopedDependency = JkScopedDependency.of(dependency, defaultMapping);
			} else {
				scopedDependency = JkScopedDependency.of(dependency);
			}
			dependencies.add(scopedDependency);
			if (this instanceof ScopeableBuilder) {
				return (ScopeableBuilder) this;
			}
			return new ScopeableBuilder(this);
		}

		public Builder on(JkScopedDependency dependency) {
			this.dependencies.add(dependency);
			return this;
		}

		public ScopeableBuilder onFile(File file) {
			return on(JkFilesDependency.of(JkUtilsIterable.listOf(file)));
		}

		public ScopeableBuilder onFiles(Iterable<File> files) {
			return on(JkFilesDependency.of(files));
		}

		public ScopeableBuilder on(JkModuleId module, JkVersionRange version) {
			return on(module, version, true);
		}

		public ScopeableBuilder on(JkModuleId module, String version) {
			return on(module, JkVersionRange.of(version));
		}


		public ScopeableBuilder on(JkModuleId module, JkVersionRange version, boolean transitive) {
			return on(JkExternalModule.of(module, version).transitive(transitive));
		}

		public ScopeableBuilder on(String organisation, String name, String version) {
			return on(organisation, name, version, true);
		}

		public ScopeableBuilder on(String organisation, String name, String version, boolean transitive) {
			return on(JkExternalModule.of(organisation, name, version).transitive(transitive));
		}

		public ScopeableBuilder on(String description) {
			return on(description, true);
		}

		public ScopeableBuilder onProject(JkBuildDependencySupport projectBuild, File ...files) {
			return on(JkProjectDependency.of(projectBuild, JkUtilsIterable.setOf(files)));
		}

		public ScopeableBuilder on(String description, boolean transitive) {
			return on(JkExternalModule.of(description).transitive(transitive));
		}

		public Builder on(Iterable<JkScopedDependency> dependencies) {
			if (!dependencies.iterator().hasNext()) {
				return this;
			}
			for (final JkScopedDependency dependency : dependencies) {
				this.dependencies.add(dependency);
			}
			return this;
		}

		public JkDependencies build() {
			return new JkDependencies(dependencies);
		}

		public static class ScopeableBuilder extends Builder {

			private ScopeableBuilder(Builder builder) {
				super(builder.dependencies);
				this.defaultMapping = builder.defaultMapping;
				this.defaultScopes = builder.defaultScopes;
			}

			private ScopeableBuilder(Builder builder, List<JkScopedDependency> current) {
				super(builder.dependencies);
				this.defaultMapping = builder.defaultMapping;
				this.defaultScopes = builder.defaultScopes;
			}

			public Builder scope(JkScopeMapping scopeMapping) {
				final JkDependency dependency = dependencies.pollLast().dependency();
				dependencies.add(JkScopedDependency.of(dependency, scopeMapping));
				return this;
			}

			public Builder scope(JkScope ... scopes) {
				final JkDependency dependency = dependencies.pollLast().dependency();
				dependencies.add(JkScopedDependency.of(dependency, JkUtilsIterable.setOf(scopes)));
				return this;
			}

			public AfterMapScopeBuilder mapScope(JkScope ... scopes) {
				return new AfterMapScopeBuilder(dependencies, JkUtilsIterable.setOf(scopes) );
			}

			public static class AfterMapScopeBuilder  {

				private final LinkedList<JkScopedDependency> dependencies;

				private final Iterable<JkScope> from;

				private AfterMapScopeBuilder(LinkedList<JkScopedDependency> dependencies, Iterable<JkScope> from) {
					this.dependencies = dependencies;
					this.from = from;
				}

				public AfterToBuilder to(JkScope... jkScopes) {
					final JkScopedDependency dependency = dependencies.pollLast();
					final JkScopeMapping mapping;
					if (dependency.scopeType() == JkScopedDependency.ScopeType.UNSET) {
						mapping = JkScopeMapping.of(from).to(jkScopes);
					}  else {
						mapping = dependency.scopeMapping().and(from).to(jkScopes);
					}
					dependencies.add(JkScopedDependency.of(dependency.dependency(), mapping));
					return new AfterToBuilder(dependencies);
				}

				public AfterToBuilder to(String... scopeNames) {
					final JkScope[] scopes = new JkScope[scopeNames.length];
					for (int i = 0; i < scopeNames.length; i++) {
						scopes[i] = JkScope.of(scopeNames[i]);
					}
					return to(scopes);
				}

			}

			public static class AfterToBuilder extends Builder {

				private AfterToBuilder(
						LinkedList<JkScopedDependency> dependencies) {
					super(dependencies);
				}

				public AfterMapScopeBuilder and(JkScope ...scopes) {
					return new AfterMapScopeBuilder(dependencies, Arrays.asList(scopes));
				}

			}

		}

	}

}
