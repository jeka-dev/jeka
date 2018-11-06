package org.jerkar.api.depmanagement;

import org.jerkar.api.utils.JkUtilsAssert;

import java.io.Serializable;

/**
 * Contains parameters likely to impact module resolution behavior.
 */
public final class JkResolutionParameters implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates resolution parameters without default mapping and no dynamic
     * version resolving refresh.
     * 
     * @see JkResolutionParameters#getScopeMapping()
     * @see #isRefreshed()
     */
    public static JkResolutionParameters of() {
        return new JkResolutionParameters(JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, true);
    }

    /**
     * Creates resolution parameters with the specified default scope mapping
     * and no dynamic version resolving refresh.
     * 
     * @see JkResolutionParameters#getScopeMapping()
     * @see #isRefreshed()
     */
    public static JkResolutionParameters of(JkScopeMapping scopeMapping) {
        JkUtilsAssert.notNull(scopeMapping,"Scope mapping cannot be null.");
        return new JkResolutionParameters(scopeMapping, true);
    }

    private final JkScopeMapping scopeMapping;

    private final boolean refreshed;

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
     * Returns <code>true</code> if during the resolution phase, the dynamic
     * version must be resolved as well or the cache can be reused.
     */
    public boolean isRefreshed() {
        return refreshed;
    }

    /**
     * @see JkResolutionParameters#isRefreshed()
     */
    public JkResolutionParameters isRefreshed(boolean refreshed) {
        return new JkResolutionParameters(scopeMapping, refreshed);
    }

    /**
     * @see #getScopeMapping()
     */
    public JkResolutionParameters withScopeMapping(JkScopeMapping defaultMapping) {
        return new JkResolutionParameters(defaultMapping, refreshed);
    }

    private JkResolutionParameters(JkScopeMapping defaultMapping, boolean refreshed) {
        super();
        this.scopeMapping = defaultMapping;
        this.refreshed = refreshed;
    }

    @Override
    public String toString() {
        return "scope mapping : " + scopeMapping + ", isRefreshed : " + refreshed;
    }

}
