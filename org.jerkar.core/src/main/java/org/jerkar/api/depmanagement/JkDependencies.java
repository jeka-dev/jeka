package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependency.JkFileDependency;
import org.jerkar.api.depmanagement.JkScopedDependency.ScopeType;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * A set of {@link JkScopedDependency} generally standing for the entire dependencies of a project/module.
 * 
 * @author Jerome Angibaud.
 */
public class JkDependencies implements Iterable<JkScopedDependency>, Serializable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public static JkDependencies on(JkScopedDependency... scopedDependencies) {
		return new JkDependencies(Arrays.asList(scopedDependencies), Collections.EMPTY_SET);
	}

	@SuppressWarnings("unchecked")
	public static JkDependencies on(JkScope scope, JkDependency ... dependencies) {
		final List<JkScopedDependency> list = new LinkedList<JkScopedDependency>();
		for (final JkDependency dependency : dependencies) {
			final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, scope);
			list.add(scopedDependency);
		}
		return new JkDependencies(list, Collections.EMPTY_SET);
	}

	private final List<JkScopedDependency> dependencies;

	private final Set<JkDepExclude> depExcludes;

	private JkDependencies(List<JkScopedDependency> dependencies, Set<JkDepExclude> excludes) {
		super();
		this.dependencies = Collections.unmodifiableList(dependencies);
		this.depExcludes = Collections.unmodifiableSet(excludes);
	}

	/**
	 * Returns <code>true</code> if this object contains no dependency.
	 */
	public boolean isEmpty() {
		return dependencies.isEmpty();
	}

	/**
	 * Returns a clone of this object minus the dependencies on the given {@link JkModuleId}.
	 * This is used to exclude a given module from all scope.
	 */
	private JkDependencies without(JkModuleId jkModuleId) {
		final List<JkScopedDependency> result = new LinkedList<JkScopedDependency>(dependencies);
		for (final Iterator<JkScopedDependency> it = result.iterator(); it.hasNext();) {
			final JkDependency dependency = it.next().dependency();
			if (dependency instanceof JkModuleDependency) {
				final JkModuleDependency externalModule = (JkModuleDependency) dependency;
				if (externalModule.moduleId().equals(jkModuleId)) {
					it.remove();
				}
			}
		}
		return new JkDependencies(result, this.depExcludes);
	}

	/**
	 * Returns a clone of this dependencies but replacing the unscoped dependencies with the scoped ones.
	 */
	public JkDependencies withDefaultScope(JkScope ...scopes) {
		final List<JkScopedDependency> list = new LinkedList<JkScopedDependency>();
		for (JkScopedDependency dep : this) {
			if (dep.scopeType().equals(ScopeType.UNSET) ||
					(dep.scopeType().equals(ScopeType.SIMPLE) && dep.scopes().isEmpty())) {
				dep = dep.withScopes(scopes);
			}
			list.add(dep);
		}
		return new JkDependencies(list, this.depExcludes);
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
		final JkScopedDependency scopedDependency = JkScopedDependency.of(JkFileSystemDependency.of(files), scope);
		return and(scopedDependency);
	}

	/**
	 * Returns a clone of this object plus {@link JkScopedDependency}s on the specified external module.
	 * @param versionedModuleId something like "org.apache:commons:1.4"
	 */
	public JkDependencies andExternal(JkScope scope, String versionedModuleId) {
		final JkDependency dependency = JkModuleDependency.of(versionedModuleId);
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
	 * Returns <code>true</code> if this object contains dependencies whose are {@link JkModuleDependency}.
	 */
	public boolean containsExternalModule() {
		for (final JkScopedDependency scopedDependency : dependencies) {
			if (scopedDependency.dependency() instanceof JkModuleDependency) {
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
			if (dependency instanceof JkModuleDependency) {
				final JkModuleDependency externalModule = (JkModuleDependency) dependency;
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

	public Set<JkScope> involvedScopes() {
		return JkScope.involvedScopes(declaredScopes());
	}

	/**
	 * Returns <code>true</code> if this object contains dependency on external module whose rely
	 * on dynamic version. It can be either dynamic version as "1.3.+", "[1.0, 2.0[" ,... or snapshot
	 * version as defined in Maven (as "1.0-SNAPSHOT).
	 */
	public boolean hasDynamicVersions() {
		for (final JkScopedDependency scopedDependency : this) {
			if (scopedDependency.dependency() instanceof JkModuleDependency) {
				final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency.dependency();
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
	 * It only stands for dynamic versions as "1.3.+", "[1.0, 2.0[" ,...
	 * If so, when resolving, dynamic versions are replaced by fixed resolved ones.
	 */
	public boolean hasDynamicAndResovableVersions() {
		for (final JkScopedDependency scopedDependency : this) {
			if (scopedDependency.dependency() instanceof JkModuleDependency) {
				final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency.dependency();
				final JkVersionRange versionRange = externalModule.versionRange();
				if (versionRange.isDynamicAndResovable()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Convenient method to resolve using {@link JkModuleDepFile}s instead of {@link JkVersionedModule}.
	 * 
	 * @see #resolvedWith(Iterable)
	 */
	public JkDependencies resolvedWithArtifacts(Iterable<JkModuleDepFile> artifacts) {
		final List<JkVersionedModule> list = new LinkedList<JkVersionedModule>();
		for (final JkModuleDepFile artifact : artifacts) {
			list.add(artifact.versionedModule());
		}
		return resolvedWith(list);
	}

	/**
	 * Creates a clone of these dependencies replacing the dynamic versions by the static ones specified
	 * in the {@link JkVersionedModule}s passed as argument. <br/>
	 */
	public JkDependencies resolvedWith(Iterable<JkVersionedModule> resolvedModules) {
		return resolvedWith(JkVersionProvider.of(resolvedModules));
	}

	/**
	 * @see #resolvedWith(Iterable)
	 */
	public JkDependencies resolvedWith(JkVersionProvider provider) {
		JkDependencies result = this;
		for (final JkModuleId moduleId : provider.moduleIds()) {
			final JkScopedDependency scopedDependency = this.get(moduleId);
			if (scopedDependency == null) {
				continue;
			}
			final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency.dependency();
			if (externalModule.versionRange().isDynamicAndResovable()) {
				final JkVersion resolvedVersion = provider.versionOf(moduleId);
				if (resolvedVersion != null) {
					final JkModuleDependency resolvedModule = externalModule.resolvedTo(resolvedVersion);
					final JkScopedDependency resolvedScopedDep = scopedDependency.dependency(resolvedModule);
					result = result.without(moduleId).and(resolvedScopedDep);
				}
			}
		}
		return result;
	}

	/**
	 * Returns all files declared as {@link JkFileSystemDependency} for any of the specified scopes.
	 */
	public JkPath localFileDependencies(JkScope ...scopes) {
		final LinkedHashSet<File> set = new LinkedHashSet<File>();
		for (final JkScopedDependency scopedDependency : this.dependencies) {
			if (scopedDependency.isInvolvedInAnyOf(scopes)
					&& scopedDependency.dependency() instanceof JkDependency) {
				final JkFileDependency fileDeps = (JkFileDependency) scopedDependency.dependency();
				set.addAll(fileDeps.files());
			}
		}
		return JkPath.of(set);
	}

	public static Builder builder() {
		return new Builder(new LinkedList<JkScopedDependency>());
	}

	public static class Builder {

		protected final LinkedList<JkScopedDependency> dependencies;

		protected final Set<JkDepExclude> depExcludes;

		protected Set<JkScope> defaultScopes;

		protected JkScopeMapping defaultMapping;

		protected Builder(LinkedList<JkScopedDependency> dependencies) {
			super();
			this.dependencies = dependencies;
			this.depExcludes = Collections.emptySet();
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

		public JkFluentScopeableBuilder on(JkDependency dependency) {
			if (dependency instanceof JkFileSystemDependency) {
				final JkFileSystemDependency fileDeps = (JkFileSystemDependency) dependency;
				if (fileDeps.files().isEmpty()) {
					return new JkFluentScopeableBuilder(this);
				}
			}
			final JkScopedDependency scopedDependency;
			if (defaultScopes != null) {
				scopedDependency = JkScopedDependency.of(dependency, defaultScopes);
			} else if (defaultMapping != null && dependency instanceof JkModuleDependency) {
				scopedDependency = JkScopedDependency.of((JkModuleDependency) dependency, defaultMapping);
			} else {
				scopedDependency = JkScopedDependency.of(dependency);
			}
			dependencies.add(scopedDependency);
			if (this instanceof JkFluentScopeableBuilder) {
				return (JkFluentScopeableBuilder) this;
			}
			return new JkFluentScopeableBuilder(this);
		}

		public JkFluentScopeMapableBuilder onExternalModule(JkModuleDependency dependency) {
			final JkScopedDependency scopedDependency;
			if (defaultScopes != null) {
				scopedDependency = JkScopedDependency.of(dependency, defaultScopes);
			} else if (defaultMapping != null) {
				scopedDependency = JkScopedDependency.of(dependency, defaultMapping);
			} else {
				scopedDependency = JkScopedDependency.of(dependency);
			}
			dependencies.add(scopedDependency);
			if (this instanceof JkFluentScopeMapableBuilder) {
				return (JkFluentScopeMapableBuilder) this;
			}
			return new JkFluentScopeMapableBuilder(this);
		}

		public Builder on(JkScopedDependency dependency) {
			this.dependencies.add(dependency);
			return this;
		}

		public JkFluentScopeableBuilder on(File ... files) {
			return on(JkFileSystemDependency.of(Arrays.asList(files)));
		}

		public JkFluentScopeableBuilder onFiles(Iterable<File> files) {
			return on(JkFileSystemDependency.of(files));
		}

		public JkFluentScopeMapableBuilder on(JkModuleId module, JkVersionRange version) {
			return on(module, version, true);
		}

		public JkFluentScopeMapableBuilder on(JkModuleId module, String version) {
			return on(module, JkVersionRange.of(version));
		}


		public JkFluentScopeMapableBuilder on(JkModuleId module, JkVersionRange version, boolean transitive) {
			return onExternalModule(JkModuleDependency.of(module, version).transitive(transitive));
		}

		public JkFluentScopeMapableBuilder on(String organisation, String name, String version) {
			return on(organisation, name, version, true);
		}

		public JkFluentScopeMapableBuilder on(String organisation, String name, String version, boolean transitive) {
			return onExternalModule(JkModuleDependency.of(organisation, name, version).transitive(transitive));
		}

		public JkFluentScopeableBuilder on(String description) {
			return on(description, true);
		}

		public JkFluentScopeableBuilder on(String description, boolean transitive) {
			return on(JkModuleDependency.of(description).transitive(transitive));
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

		public Builder exclude(JkDepExclude exclude) {
			this.depExcludes.add(exclude);
			return this;
		}

		public Builder exclude(String group, String name) {
			return exclude(JkDepExclude.of(group, name));
		}

		public Builder exclude(String groupAndName) {
			return exclude(JkDepExclude.of(groupAndName));
		}

		public JkDependencies build() {
			return new JkDependencies(dependencies, depExcludes);
		}

		public static class JkFluentScopeableBuilder extends Builder {

			protected JkFluentScopeableBuilder(Builder builder) {
				super(builder.dependencies);
				this.defaultMapping = builder.defaultMapping;
				this.defaultScopes = builder.defaultScopes;
			}

			private JkFluentScopeableBuilder(Builder builder, List<JkScopedDependency> current) {
				super(builder.dependencies);
				this.defaultMapping = builder.defaultMapping;
				this.defaultScopes = builder.defaultScopes;
			}


			public Builder scope(JkScope ... scopes) {
				final JkDependency dependency = dependencies.pollLast().dependency();
				dependencies.add(JkScopedDependency.of(dependency, JkUtilsIterable.setOf(scopes)));
				return this;
			}

		}

		public static final class JkFluentScopeMapableBuilder extends JkFluentScopeableBuilder {

			private JkFluentScopeMapableBuilder(Builder builder) {
				super(builder);
			}

			public Builder scope(JkScopeMapping scopeMapping) {
				final JkModuleDependency dependency =
						(JkModuleDependency) dependencies.pollLast().dependency();
				dependencies.add(JkScopedDependency.of(dependency, scopeMapping));
				return this;
			}

			public JkFluentAfterMapScopeBuilder mapScope(JkScope ... scopes) {
				return new JkFluentAfterMapScopeBuilder(dependencies, JkUtilsIterable.setOf(scopes) );
			}


		}

		public static class JkFluentAfterMapScopeBuilder  {

			private final LinkedList<JkScopedDependency> dependencies;

			private final Iterable<JkScope> from;

			private JkFluentAfterMapScopeBuilder(LinkedList<JkScopedDependency> dependencies, Iterable<JkScope> from) {
				this.dependencies = dependencies;
				this.from = from;
			}

			public JkFluentAfterToBuilder to(JkScope... jkScopes) {
				final JkScopedDependency scopedDependency = dependencies.pollLast();
				final JkScopeMapping mapping;
				final JkModuleDependency dependency = (JkModuleDependency) scopedDependency.dependency();
				if (scopedDependency.scopeType() == JkScopedDependency.ScopeType.UNSET) {
					mapping = JkScopeMapping.of(from).to(jkScopes);
				}  else {
					mapping = scopedDependency.scopeMapping().and(from).to(jkScopes);
				}
				dependencies.add(JkScopedDependency.of(dependency, mapping));
				return new JkFluentAfterToBuilder(dependencies);
			}

			public JkFluentAfterToBuilder to(String... scopeNames) {
				final JkScope[] scopes = new JkScope[scopeNames.length];
				for (int i = 0; i < scopeNames.length; i++) {
					scopes[i] = JkScope.of(scopeNames[i]);
				}
				return to(scopes);
			}

		}

		public static class JkFluentAfterToBuilder extends Builder {

			private JkFluentAfterToBuilder(
					LinkedList<JkScopedDependency> dependencies) {
				super(dependencies);
			}

			public JkFluentAfterMapScopeBuilder and(JkScope ...scopes) {
				return new JkFluentAfterMapScopeBuilder(dependencies, Arrays.asList(scopes));
			}

		}


	}

}
