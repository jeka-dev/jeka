package org.jake.depmanagement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsIterable;

/**
 * A dependency specifying in which scope it should be used. Scopes can be declared as simple scopes
 * or as a scope mapping.
 */
public final class JakeScopedDependency {

	@SuppressWarnings("unchecked")
	public static JakeScopedDependency of(JakeDependency dependency, JakeScopeMapping scopeMapping) {
		return new JakeScopedDependency(dependency, Collections.EMPTY_SET, scopeMapping);
	}

	public static JakeScopedDependency of(JakeDependency dependency, JakeScope ...scopes) {
		return JakeScopedDependency.of(dependency, JakeUtilsIterable.setOf(scopes));
	}

	public static JakeScopedDependency of(JakeDependency dependency, Set<JakeScope> scopes) {
		return new JakeScopedDependency(dependency, Collections.unmodifiableSet(new HashSet<JakeScope>(scopes)), null);
	}

	// ------------------ Instance menbers --------------------

	private final JakeDependency dependency;

	private final Set<JakeScope> scopes;

	private final JakeScopeMapping scopeMapping;

	private JakeScopedDependency(JakeDependency dependency, Set<JakeScope> scopes, JakeScopeMapping scopeMapping) {
		super();
		JakeUtilsAssert.notNull(dependency, "Dependency can't be null.");
		this.dependency = dependency;
		this.scopes = scopes;
		this.scopeMapping = scopeMapping;
	}

	public JakeDependency dependency() {
		return dependency;
	}

	public boolean isInvolving(JakeScope scope) {
		if (scopeMapping == null) {
			return scope.isInOrIsInheritedByAnyOf(scopes);
		}
		return scope.isInOrIsInheritedByAnyOf(scopeMapping.entries());
	}

	public boolean isInvolving(JakeScope scope, Set<JakeScope> defaultScopes) {
		if (scopes.contains(scope) || scope.isInOrIsInheritedByAnyOf(scopes)) {
			return true;
		}
		final boolean mapped = scopeMapping.entries().contains(scope) || scope.isInOrIsInheritedByAnyOf(scopeMapping.entries());
		if (!mapped) {
			return scope.isInOrIsInheritedByAnyOf(defaultScopes);
		}
		return true;
	}

	public Set<JakeScope> mappedScopes(JakeScope scope) {
		final Set<JakeScope> result;
		if (this.scopeMapping != null) {
			if (!scope.isInOrIsInheritedByAnyOf(this.scopeMapping.entries())) {
				throw new IllegalArgumentException(scope.toString() + " is not declared in this dependency. Declared scopes are " + this.scopeMapping.entries());
			}
			result = this.scopeMapping.targetScopes(scope);
		} else {
			if (!scope.isInOrIsInheritedByAnyOf(scopes)) {
				throw new IllegalArgumentException(scope.toString() + " is not declared in this dependency. Declared scopes are " + scopes);
			}
			result = JakeUtilsIterable.setOf(scope);
		}
		return result;
	}

	public Set<JakeScope> mappedScopes(JakeScope scope, JakeScopeMapping defaultMapping) {
		Set<JakeScope> result = null;
		if (this.scopeMapping != null) {
			result = this.scopeMapping.targetScopes(scope);
		}
		if (result == null) {
			result = defaultMapping.targetScopes(scope);
		}
		if (result == null) {
			if (!scopes.contains(scope)) {
				throw new IllegalArgumentException(scope.toString() + " is not declared in this dependency. Declared scopes are " + scopes);
			}
			result = JakeUtilsIterable.setOf(scope);
		}
		return result;
	}

	Set<JakeScope> scopes() {
		return this.scopes;
	}

	JakeScopeMapping scopeMapping() {
		return this.scopeMapping;
	}


}
