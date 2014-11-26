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

	public static enum ScopeType {
		SIMPLE, MAPPED, UNSET
	}

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
			return scope.isInOrIsExtendingAnyOf(scopes);
		}
		return scope.isInOrIsExtendingAnyOf(scopeMapping.entries());
	}

	// ??????
	public boolean isInvolving(JakeScope scope, Set<JakeScope> defaultScopes) {
		if (scopes.contains(scope) || scope.isInOrIsExtendingAnyOf(scopes)) {
			return true;
		}
		final boolean mapped = scopeMapping.entries().contains(scope) || scope.isInOrIsExtendingAnyOf(scopeMapping.entries());
		if (!mapped) {
			return scope.isInOrIsExtendingAnyOf(defaultScopes);
		}
		return true;
	}



	public ScopeType scopeType() {
		if (this.scopes != null && !this.scopes.isEmpty()) {
			return ScopeType.SIMPLE;
		}
		if (this.scopeMapping != null && !this.scopeMapping.entries().isEmpty()) {
			return ScopeType.MAPPED;
		}
		return ScopeType.UNSET;
	}

	public Set<JakeScope> scopes() {
		JakeUtilsAssert.isTrue(this.scopeType() == ScopeType.SIMPLE, "This dependency does not declare simple scopes.");
		return this.scopes;
	}

	public JakeScopeMapping scopeMapping() {
		JakeUtilsAssert.isTrue(this.scopeType() == ScopeType.MAPPED, "This dependency does not declare scope mappings.");
		return this.scopeMapping;
	}

	@Override
	public String toString() {
		return dependency.toString() + "[" + (scopeMapping != null ? scopeMapping.toString() : "")
				+ ((scopes== null || scopes.isEmpty()) ? "" : scopes.toString()) + "]";
	}


}
