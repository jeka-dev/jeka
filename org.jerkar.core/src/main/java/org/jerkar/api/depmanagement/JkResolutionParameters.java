package org.jerkar.api.depmanagement;

import java.io.Serializable;

/**
 * Carries some parameters about dependencies resolution.
 *
 */
public class JkResolutionParameters implements Serializable {

	private static final long serialVersionUID = 1L;

	public static JkResolutionParameters of() {
		return new JkResolutionParameters(null, false);
	}

	public static JkResolutionParameters defaultScopeMapping(JkScopeMapping scopeMapping) {
		return new JkResolutionParameters(scopeMapping, false);
	}



	private final JkScopeMapping defaultMapping;

	private final boolean refreshed;




	/**
	 * Returns the default scope mapping to use for the {@link JkDependencies} to be resolved. <code>null</code> means
	 * no default scope mapping.
	 *  @see <a href="http://ant.apache.org/ivy/history/2.3.0/ivyfile/configurations.html">Ivy configuration doc</a>
	 */
	public JkScopeMapping defaultMapping() {
		return defaultMapping;
	}

	/**
	 * Returns <code>true</code> if during the resolution phase, the changing dependencies must be down-loaded.
	 */
	public boolean refreshed() {
		return refreshed;
	}

	/**
	 * @see JkResolutionParameters#refreshed()
	 */
	public JkResolutionParameters refreshed(boolean refreshed) {
		return new JkResolutionParameters(defaultMapping, refreshed);
	}

	/**
	 * @see #defaultMapping()
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
		return "default mapping : " + defaultMapping + ", refreshed : " + refreshed;
	}


}
