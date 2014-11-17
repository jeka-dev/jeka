package org.jake.depmanagement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jake.depmanagement.JakeScope.JakeScopeMapping;

public class Dependencies implements Iterable<JakeScopedDependency>{

	private final Set<JakeScopedDependency> dependencies;

	private Dependencies(Set<JakeScopedDependency> dependencies) {
		super();
		this.dependencies = Collections.unmodifiableSet(new HashSet<JakeScopedDependency>(dependencies));
	}

	public Dependencies without(JakeModuleId jakeModuleId) {
		final Set<JakeScopedDependency> result = new HashSet<JakeScopedDependency>(dependencies);
		remove(result, jakeModuleId);
		return new Dependencies(result);
	}

	private static void remove(Set<JakeScopedDependency> dependencies, JakeModuleId jakeModuleId) {
		for (final Iterator<JakeScopedDependency> it = dependencies.iterator(); it.hasNext();) {
			final JakeDependency dependency = it.next().dependency();
			if (dependency instanceof JakeExternalModule) {
				final JakeExternalModule externalModule = (JakeExternalModule) dependency;
				if (externalModule.moduleId().equals(jakeModuleId)) {
					it.remove();
				}
			}
		}
	}

	@Override
	public Iterator<JakeScopedDependency> iterator() {
		return dependencies.iterator();
	}

	public static Builder builder() {
		return new Builder(new HashSet<JakeScopedDependency>(), null);
	}

	public static class Builder {

		protected JakeDependency lastDependency;

		protected final Set<JakeScopedDependency> dependencies;

		private JakeScopeMapping defaultScopeMapping = JakeScopeMapping.compile();

		protected Builder(Set<JakeScopedDependency> dependencies, JakeDependency lastDependency) {
			super();
			this.dependencies = dependencies;
			this.lastDependency = lastDependency;
		}

		public Builder defaultScope(JakeScopeMapping scope) {
			defaultScopeMapping = scope;
			return this;
		}

		public Builder defaultScope(JakeScope scope) {
			defaultScopeMapping = JakeScopeMapping.of(scope, scope);
			return this;
		}

		public ScopebleBuilder on(JakeDependency dependency) {
			dependencies.add(dependency.scope(defaultScopeMapping));
			lastDependency = dependency;
			if (this instanceof ScopebleBuilder) {
				return (ScopebleBuilder) this;
			}
			return new ScopebleBuilder(dependencies, dependency);
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

		public Dependencies build() {
			return new Dependencies(dependencies);
		}

		public static class ScopebleBuilder extends Builder {

			protected ScopebleBuilder(Set<JakeScopedDependency> dependencies, JakeDependency dependency) {
				super(dependencies, dependency);
			}

			public Builder scope(JakeScopeMapping scopeMapping) {
				final JakeModuleId moduleId = lastDependency.moduleId();
				Dependencies.remove(dependencies, moduleId);
				dependencies.add(lastDependency.withScope(scopeMapping));
				return this;
			}

			public Builder scope(JakeScope scope) {
				return scope(JakeScopeMapping.of(scope, scope));
			}

		}

	}

}
