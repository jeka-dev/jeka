package org.jake.depmanagement;

import org.jake.depmanagement.JakeScope.JakeScopeMapping;
import org.jake.utils.JakeUtilsAssert;

public final class JakeScopedDependency {

	private final JakeDependency dependency;

	private final JakeScopeMapping scopeMapping;

	private JakeScopedDependency(JakeDependency dependency, JakeScopeMapping scopeMapping) {
		super();
		JakeUtilsAssert.notNull(dependency, "Dependency can't be null.");
		JakeUtilsAssert.notNull(scopeMapping, "ScopeMapping can't be null.");
		this.dependency = dependency;
		this.scopeMapping = scopeMapping;
	}

	public static JakeScopedDependency of(JakeDependency dependency, JakeScopeMapping scopeMapping) {
		return new JakeScopedDependency(dependency, scopeMapping);
	}

	public static JakeScopedDependency of(JakeDependency dependency, JakeScope scope) {
		return new JakeScopedDependency(dependency, JakeScopeMapping.of(scope, scope));
	}

	public JakeScopedDependency scope(JakeScope...scopes) {
		JakeScopeMapping mapping = scopeMapping;
		for (final JakeScope scope : scopes) {
			mapping = mapping.and(scope, scope);
		}
		return new JakeScopedDependency(dependency, mapping);
	}

	public JakeDependency dependency() {
		return dependency;
	}

	public JakeScopeMapping scope() {
		return scopeMapping;
	}




}
