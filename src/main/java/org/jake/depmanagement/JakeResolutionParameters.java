package org.jake.depmanagement;


public class JakeResolutionParameters {

	public static JakeResolutionParameters of() {
		return new JakeResolutionParameters(null, null, false);
	}

	public static JakeResolutionParameters of(JakeScopeMapping defaultScopeMapping) {
		return new JakeResolutionParameters(null, defaultScopeMapping, false);
	}

	private final JakeScope defaultScope;

	private final JakeScopeMapping defaultMapping;

	private final boolean refreshed;



	/**
	 * Returns the default scope to use for the {@link JakeDependencies} to be resolved. <code>null</code> means
	 * no default scope.
	 * @see <a href="http://ant.apache.org/ivy/history/2.3.0/ivyfile/configurations.html">Ivy configuration doc</a>
	 */
	public JakeScope defaultScope() {
		return defaultScope;
	}

	/**
	 * Returns the default scope mapping to use for the {@link JakeDependencies} to be resolved. <code>null</code> means
	 * no default scope mapping.
	 *  @see <a href="http://ant.apache.org/ivy/history/2.3.0/ivyfile/configurations.html">Ivy configuration doc</a>
	 */
	public JakeScopeMapping defaultMapping() {
		return defaultMapping;
	}

	/**
	 * Returns <code>true</code> if during the resolution phase, the changing dependencies must be down-loaded.
	 */
	public boolean refreshed() {
		return refreshed;
	}

	public JakeResolutionParameters refreshed(boolean refreshed) {
		return new JakeResolutionParameters(defaultScope, defaultMapping, refreshed);
	}

	public JakeResolutionParameters withDefault(JakeScope defaultScope) {
		return new JakeResolutionParameters(defaultScope, defaultMapping, refreshed);
	}

	public JakeResolutionParameters withDefault(JakeScopeMapping defaultMapping) {
		return new JakeResolutionParameters(defaultScope, defaultMapping, refreshed);
	}


	private JakeResolutionParameters(JakeScope defaultScope, JakeScopeMapping defaultMapping, boolean refreshed) {
		super();
		this.defaultScope = defaultScope;
		this.defaultMapping = defaultMapping;
		this.refreshed = refreshed;
	}






}
