package dev.jeka.core.api.depmanagement;

/**
 * Contains parameters likely to impact module resolution behavior.
 */
public final class JkResolutionParameters<T> {

    private JkScopeMapping scopeMapping = JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;

    private boolean refreshed = true;

    /**
     * For parent chaining
     */
    public T __;

    private JkResolutionParameters(T parent) {
        __ = parent;
    }

    static <T> JkResolutionParameters of(T parent) {
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
