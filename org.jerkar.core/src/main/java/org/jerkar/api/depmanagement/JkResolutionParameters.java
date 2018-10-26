package org.jerkar.api.depmanagement;

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
     * @see JkResolutionParameters#getDefaultMapping()
     * @see #isRefreshed()
     */
    public static JkResolutionParameters of() {
        return new JkResolutionParameters(null, true);
    }

    /**
     * Creates resolution parameters with the specified default scope mapping
     * and no dynamic version resolving refresh.
     * 
     * @see JkResolutionParameters#getDefaultMapping()
     * @see #isRefreshed()
     */
    public static JkResolutionParameters of(JkScopeMapping scopeMapping) {
        return new JkResolutionParameters(scopeMapping, true);
    }

    private final JkScopeMapping defaultMapping;

    private final boolean refreshed;

    /**
     * Returns the default scope mapping to use for the {@link JkDependencySet}
     * to be resolved. <code>null</code> means no default scope mapping.
     * 
     * @see <a href=
     *      "http://ant.apache.org/ivy/history/2.3.0/ivyfile/configurations.html">
     *      Ivy configuration doc</a>
     */
    public JkScopeMapping getDefaultMapping() {
        return defaultMapping;
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
        return new JkResolutionParameters(defaultMapping, refreshed);
    }

    /**
     * @see #getDefaultMapping()
     */
    public JkResolutionParameters withDefault(JkScopeMapping defaultMapping) {
        return new JkResolutionParameters(defaultMapping, refreshed);
    }

    private JkResolutionParameters(JkScopeMapping defaultMapping, boolean refreshed) {
        super();
        this.defaultMapping = defaultMapping;
        this.refreshed = refreshed;
    }

    @Override
    public String toString() {
        return "default mapping : " + defaultMapping + ", isRefreshed : " + refreshed;
    }

}
