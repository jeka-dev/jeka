package org.jake.depmanagement;

import java.util.HashSet;
import java.util.Set;

import org.jake.depmanagement.JakeScope.JakeScopeMapping;

public class Dependencies {

	private final Set<Dependency> dependencies;

	private Dependencies(Set<Dependency> dependencies) {
		super();
		this.dependencies = dependencies;
	}

	public static Builder builder() {
		return new Builder(new HashSet<Dependency>());
	}

	public static class Builder {

		private final Set<Dependency> dependencies;

		private Builder(Set<Dependency> dependencies) {
			super();
			this.dependencies = dependencies;
		}

		public Builder on(Dependency dependency) {
			dependencies.add(dependency);
			return this;
		}

		public Builder on(JakeModuleId module, JakeVersionRange version, JakeScopeMapping mapping) {
			return on(Dependency.of(module, version, mapping));
		}

		public Builder on(JakeModuleId module, JakeVersionRange version) {
			return on(Dependency.of(module, version));
		}

		public Builder on(String organisation, String name, String version) {
			return on(Dependency.of(organisation, name, version));
		}

		public Builder on(String description) {
			return on(Dependency.of(description));
		}

		public Builder on(Dependencies dependencies) {
			for (final Dependency dependency : dependencies.dependencies) {
				this.dependencies.add(dependency);
			}
			return this;
		}

		public Dependencies build() {
			return new Dependencies(dependencies);
		}






	}






}
