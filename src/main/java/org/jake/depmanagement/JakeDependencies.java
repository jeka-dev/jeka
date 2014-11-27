package org.jake.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.depmanagement.JakeScopedDependency.ScopeType;
import org.jake.utils.JakeUtilsIterable;

public class JakeDependencies implements Iterable<JakeScopedDependency>{

	private final List<JakeScopedDependency> dependencies;

	private JakeDependencies(List<JakeScopedDependency> dependencies) {
		super();
		this.dependencies = Collections.unmodifiableList(new LinkedList<JakeScopedDependency>(dependencies));
	}

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

	@Override
	public Iterator<JakeScopedDependency> iterator() {
		return dependencies.iterator();
	}

	@Override
	public String toString() {
		return dependencies.toString();
	}

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

	public boolean hasSimpleScope() {
		for (final JakeScopedDependency dep : this.dependencies) {
			if (dep.scopeType() == ScopeType.SIMPLE) {
				return true;
			}
		}
		return false;
	}

	public static Builder builder() {
		return new Builder(new LinkedList<JakeScopedDependency>());
	}

	public static class Builder {

		protected final LinkedList<JakeScopedDependency> dependencies;

		private Set<JakeScope> defaultScopes;

		private JakeScopeMapping defaultMapping;

		protected Builder(LinkedList<JakeScopedDependency> dependencies) {
			super();
			this.dependencies = dependencies;
		}

		public Builder defaultScope(JakeScope ...scopes) {
			if (scopes.length == 0) {
				return resetDefaultScope();
			}
			defaultScopes = JakeUtilsIterable.setOf(scopes);
			defaultMapping = null;
			return this;
		}

		public Builder defaultScope(JakeScopeMapping scopeMapping) {
			defaultMapping = scopeMapping;
			defaultScopes = null;
			return this;
		}

		public Builder resetDefaultScope() {
			defaultScopes = null;
			defaultMapping = null;
			return this;
		}

		public ScopebleBuilder on(JakeDependency dependency) {
			final JakeScopedDependency scopedDependency;
			if (defaultScopes != null) {
				scopedDependency = JakeScopedDependency.of(dependency, defaultScopes);
			} else if (defaultMapping != null) {
				scopedDependency = JakeScopedDependency.of(dependency, defaultMapping);
			} else {
				scopedDependency = JakeScopedDependency.of(dependency);
			}
			dependencies.add(scopedDependency);
			if (this instanceof ScopebleBuilder) {
				return (ScopebleBuilder) this;
			}
			return new ScopebleBuilder(dependencies);
		}

		public Builder on(JakeScopedDependency dependency) {
			this.dependencies.add(dependency);
			return this;
		}

		public ScopebleBuilder on(JakeModuleId module, JakeVersionRange version) {
			return on(JakeExternalModule.of(module, version));
		}

		public ScopebleBuilder on(String organisation, String name, String version) {
			return on(JakeExternalModule.of(organisation, name, version));
		}

		public ScopebleBuilder on(String description) {
			return on(JakeExternalModule.of(description));
		}

		public Builder on(Iterable<JakeScopedDependency> dependencies) {
			for (final JakeScopedDependency dependency : dependencies) {
				this.dependencies.add(dependency);
			}
			return this;
		}

		public JakeDependencies build() {
			return new JakeDependencies(dependencies);
		}

		public static class ScopebleBuilder extends Builder {

			private ScopebleBuilder(LinkedList<JakeScopedDependency> dependencies) {
				super(dependencies);
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
