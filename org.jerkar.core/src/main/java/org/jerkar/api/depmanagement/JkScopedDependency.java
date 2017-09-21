package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * A dependency along a scope information to specify for wich purpose it should be used.
 * A scoped dependency can be declared either with {@link JkScope}s nor {@link JkScopeMapping}.
 *
 * Jerkar uses Ivy under the hood for dependency resolution. Internally {@link JkScope} are turned to Ivy 'configuration'
 * and {@link JkScopeMapping} are turned to Ivy 'configurationMapping'.
 *
 * To understand how scope and mapping scope influence resolution, you can visit <a href="http://ant.apache.org/ivy/history/latest-milestone/ivyfile/configurations.html">this page</a>.
 */
public final class JkScopedDependency implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Type for the scope.<ul>
     * <li>SIMPLE means that the the .</li>
     * <li>MAPPED means that the scoped dependency is declared with a {@link JkScopeMapping}.</li>
     * <li>UNSET means that the scoped dependency has been declared with no scope and no scope mapping.</li>
     * </ul>
     */
    public enum ScopeType {

        /** scoped dependency is declared with 1 or several {@link JkScope} */
        SIMPLE,

        /** scoped dependency is declared with 1 or several {@link JkScope} */
        MAPPED,

        /** scoped dependency is declared with no scope and no mapping scope */
        UNSET
    }

    /**
     * Creates a {@link JkScopedDependency} to the specified dependency and scope mapping.
     */
    @SuppressWarnings("unchecked")
    public static JkScopedDependency of(JkModuleDependency dependency, JkScopeMapping scopeMapping) {
        return new JkScopedDependency(dependency, Collections.EMPTY_SET, scopeMapping);
    }

    /**
     * Creates a {@link JkScopedDependency} to the specified dependency and scopes.
     */
    public static JkScopedDependency of(JkDependency dependency, JkScope... scopes) {
        return JkScopedDependency.of(dependency, JkUtilsIterable.setOf(scopes));
    }

    /**
     * Creates a {@link JkScopedDependency} to the specified dependency and scopes.
     */
    public static JkScopedDependency of(JkDependency dependency, Set<JkScope> scopes) {
        return new JkScopedDependency(dependency, Collections.unmodifiableSet(new HashSet<>(
                scopes)), null);
    }

    // ------------------ Instance members --------------------

    private final JkDependency dependency;

    private final Set<JkScope> scopes;

    private final JkScopeMapping scopeMapping;

    private JkScopedDependency(JkDependency dependency, Set<JkScope> scopes,
            JkScopeMapping scopeMapping) {
        super();
        JkUtilsAssert.notNull(dependency, "Dependency can't be null.");
        this.dependency = dependency;
        this.scopes = scopes;
        this.scopeMapping = scopeMapping;
    }

    /**
     * Returns the dependency object of this scoped dependency.
     */
    public JkDependency dependency() {
        return dependency;
    }

    /**
     * Returns <code>true</code> if this scoped dependency should be taken in account when one grabs the dependencies for
     * the specified scope.
     */
    public boolean isInvolvedIn(JkScope scope) {
        if (scopeMapping == null) {
            return scope.isInOrIsExtendingAnyOf(scopes);
        }
        return scope.isInOrIsExtendingAnyOf(scopeMapping.entries());
    }

    /**
     * Returns <code>true</code> if this scoped dependency should be taken in account when one grabs the dependencies for
     * any of the specified scopes.
     */
    public boolean isInvolvedInAnyOf(Iterable<JkScope> scopes) {
        for (final JkScope scope : scopes) {
            if (isInvolvedIn(scope)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if this scoped dependency should be taken in account when one grabs the dependencies for
     * any of the specified scopes.
     */
    public boolean isInvolvedInAnyOf(JkScope... scopes) {
        return isInvolvedInAnyOf(Arrays.asList(scopes));
    }

    /**
     * Return wether this scoped dependency is declared with either scope nor scope mapping.
     * If no scopes and scope mapping is declared, this method returns {@link ScopeType#UNSET}.
     */
    public ScopeType scopeType() {
        if (this.scopes != null && !this.scopes.isEmpty()) {
            return ScopeType.SIMPLE;
        }
        if (this.scopeMapping != null && !this.scopeMapping.entries().isEmpty()) {
            return ScopeType.MAPPED;
        }
        return ScopeType.UNSET;
    }

    /**
     * Returns a the scopes this scoped dependency. If this scoped dependency declares either a mapping scope
     * nor no scope at all then this method returns an empty set.
     */
    public Set<JkScope> scopes() {
        return this.scopes;
    }

    /**
     * Returns a scoped dependency identical to this one but with the specified scopes.
     */
    public JkScopedDependency withScopes(Set<JkScope> scopes) {
        return JkScopedDependency.of(dependency, scopes);
    }

    /**
     * Returns a scoped dependency identical to this one but with the specified scope mapping and no scopes.
     * This method should be invoked only when this dependency is type of {@link JkModuleDependency}.
     * If it is not the case, an {@link IllegalStateException} is thrown.
     */
    public JkScopedDependency withScopeMapping(JkScopeMapping scopeMapping) {
        if (! (this.dependency instanceof JkModuleDependency)) {
            throw new IllegalStateException("This dependency is type of "
                    + this.dependency.getClass().getName() + ". Expecting JkModuleDependency.");
        }
        return JkScopedDependency.of((JkModuleDependency) this.dependency, scopeMapping);
    }

    /**
     * Returns a scoped dependency identical to this one but with the specified scopes.
     */
    public JkScopedDependency withScopes(JkScope... scopes) {
        return withScopes(JkUtilsIterable.setOf(scopes));
    }

    /**
     * Returns the scope mapping this scoped dependency is declared with. It returns null, if no scope mapping
     * is declared with this scoped dependency (dependency declared with 0,1 or many scopes).
     */
    public JkScopeMapping scopeMapping() {
        JkUtilsAssert.isTrue(this.scopeType() == ScopeType.MAPPED,
                "This dependency does not declare scope mappings.");
        return this.scopeMapping;
    }

    /**
     * Returns a scoped dependency formed of the scope/scopeMapping of this scoped dependency
     * and the specified dependency.
     */
    public JkScopedDependency dependency(JkDependency dependency) {
        return new JkScopedDependency(dependency, scopes, scopeMapping);
    }

    @Override
    public String toString() {
        return dependency.toString() + (scopeMapping != null ? scopeMapping.toString() : "")
                + ((scopes == null || scopes.isEmpty()) ? "[]" : scopes.toString());
    }

}
