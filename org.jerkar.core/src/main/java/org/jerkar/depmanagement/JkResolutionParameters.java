package org.jerkar.depmanagement;

import java.io.Serializable;


public class JkResolutionParameters implements Serializable {

	private static final long serialVersionUID = 1L;

	public static JkResolutionParameters of() {
		return new JkResolutionParameters(null, null, false);
	}

	public static JkResolutionParameters of(JkScopeMapping defaultScopeMapping) {
		return new JkResolutionParameters(null, defaultScopeMapping, false);
	}

	private final JkScope defaultScope;

	private final JkScopeMapping defaultMapping;

	private final boolean refreshed;



	/**
	 * Returns the default scope to use for the {@link JkDependencies} to be resolved. <code>null</code> means
	 * no default scope.
	 * @see <a href="http://ant.apache.org/ivy/history/2.3.0/ivyfile/configurations.html">Ivy configuration doc</a>
	 */
	public JkScope defaultScope() {
		return defaultScope;
	}

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

	public JkResolutionParameters refreshed(boolean refreshed) {
		return new JkResolutionParameters(defaultScope, defaultMapping, refreshed);
	}

	public JkResolutionParameters withDefault(JkScope defaultScope) {
		return new JkResolutionParameters(defaultScope, defaultMapping, refreshed);
	}

	public JkResolutionParameters withDefault(JkScopeMapping defaultMapping) {
		return new JkResolutionParameters(defaultScope, defaultMapping, refreshed);
	}


	private JkResolutionParameters(JkScope defaultScope, JkScopeMapping defaultMapping, boolean refreshed) {
		super();
		this.defaultScope = defaultScope;
		this.defaultMapping = defaultMapping;
		this.refreshed = refreshed;
	}


}
