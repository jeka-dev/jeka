package org.jerkar.depmanagement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.utils.JkUtilsAssert;
import org.jerkar.utils.JkUtilsIterable;

/**
 * A dependency specifying in which scope it should be used. Scopes can be declared as simple scopes
 * or as a scope mapping.
 */
public final class JkScopedDependency {

	public static enum ScopeType {
		SIMPLE, MAPPED, UNSET
	}

	@SuppressWarnings("unchecked")
	public static JkScopedDependency of(JkDependency dependency, JkScopeMapping scopeMapping) {
		return new JkScopedDependency(dependency, Collections.EMPTY_SET, scopeMapping);
	}

	public static JkScopedDependency of(JkDependency dependency, JkScope ...scopes) {
		return JkScopedDependency.of(dependency, JkUtilsIterable.setOf(scopes));
	}

	public static JkScopedDependency of(JkDependency dependency, Set<JkScope> scopes) {
		return new JkScopedDependency(dependency, Collections.unmodifiableSet(new HashSet<JkScope>(scopes)), null);
	}

	// ------------------ Instance members --------------------

	private final JkDependency dependency;

	private final Set<JkScope> scopes;

	private final JkScopeMapping scopeMapping;

	private JkScopedDependency(JkDependency dependency, Set<JkScope> scopes, JkScopeMapping scopeMapping) {
		super();
		JkUtilsAssert.notNull(dependency, "Dependency can't be null.");
		this.dependency = dependency;
		this.scopes = scopes;
		this.scopeMapping = scopeMapping;
	}

	public JkDependency dependency() {
		return dependency;
	}


	public boolean isInvolvedIn(JkScope scope) {
		if (scopeMapping == null) {
			return scope.isInOrIsExtendingAnyOf(scopes);
		}
		return scope.isInOrIsExtendingAnyOf(scopeMapping.entries());
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

	public Set<JkScope> scopes() {
		JkUtilsAssert.isTrue(this.scopeType() == ScopeType.SIMPLE, "This dependency does not declare simple scopes.");
		return this.scopes;
	}



	public JkScopeMapping scopeMapping() {
		JkUtilsAssert.isTrue(this.scopeType() == ScopeType.MAPPED, "This dependency does not declare scope mappings.");
		return this.scopeMapping;
	}

	public JkScopedDependency dependency(JkDependency dependency) {
		return new JkScopedDependency(dependency, scopes, scopeMapping);
	}

	@Override
	public String toString() {
		return dependency.toString() + "[" + (scopeMapping != null ? scopeMapping.toString() : "")
				+ ((scopes== null || scopes.isEmpty()) ? "" : scopes.toString()) + "]";
	}


}
