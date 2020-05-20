package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;

/**
 * Contains parameters likely to impact module resolution behavior.
 */
public final class JkResolutionParameters<T> {

    /**
     * Strategy for resolving version conflict
     */
    public enum JkConflictResolver {

        /**
         * Default conflict resolver. By default, on Ivy it takes the greatest version, unless
         * a version is expressed explicitly in direct dependency.
         */
        DEFAULT,

        /**
         * Fail resolution if a there is a version conflict into the resolved tree. User has to
         * exclude unwanted version explicitly.
         */
        STRICT;
    }

    private JkScopeMapping scopeMapping = JkScope.DEFAULT_SCOPE_MAPPING;

    private boolean refreshed = true;

    private JkConflictResolver conflictResolver = JkConflictResolver.DEFAULT;

    /**
     * For parent chaining
     */
    public T __;

    private JkResolutionParameters(T parent) {
        __ = parent;
    }

    static <T> JkResolutionParameters ofParent(T parent) {
        return new JkResolutionParameters(parent);
    }

    /**
     * Returns the default scope mapping to use for the {@link JkDependencySet}
     * to be resolved. <code>null</code> means no default scope mapping.
     * 
     * @see <a href=
     *      "http://ant.apache.org/ivy/history/2.3.0/ivyfile/configurations.html">
     *      Ivy configuration doc</a>
     */
    public JkScopeMapping getScopeMapping() {
        return scopeMapping;
    }

    /**
     * @see #getScopeMapping()
     */
    public JkResolutionParameters<T> setScopeMapping(JkScopeMapping scopeMapping) {
        this.scopeMapping = scopeMapping;
        return this;
    }

    /**
     * Returns the conflict resolver to use.
     */
    public JkConflictResolver getConflictResolver() {
        return conflictResolver;
    }

    /**
     * Set the {@link JkConflictResolver} to use.
     */
    public JkResolutionParameters<T> setConflictResolver(JkConflictResolver conflictResolver) {
        JkUtilsAssert.argument(conflictResolver != null, "conflictResolver can not be null.");
        this.conflictResolver = conflictResolver;
        return this;
    }

    /**
     * Returns <code>true</code> if during the resolution phase, the dynamic
     * version must be resolved as well or the cache can be reused.
     */
    public boolean isRefreshed() {
        return refreshed;
    }

    /**
     * @see JkResolutionParameters#isRefreshed()
     */
    public JkResolutionParameters<T> setRefreshed(boolean refreshed) {
        this.refreshed = refreshed;
        return this;
    }

    @Override
    public String toString() {
        return "scope mapping : " + scopeMapping + ", isRefreshed : " + refreshed;
    }

}
