package org.jake.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsIterable;

public class JakeResolutionParameters {

	public static NotDefaulted resolvedScopes(JakeScope ...resolvedScopes) {
		return resolvedScopes(Arrays.asList(resolvedScopes));
	}

	public static NotDefaulted resolvedScopes(Iterable<JakeScope> resolvedScopes) {
		JakeUtilsAssert.notNull(resolvedScopes, "Resolved scope can not be null.");
		final Set<JakeScope> set = JakeUtilsIterable.setOf(resolvedScopes);
		return new NotDefaulted(set, null, null, false);
	}

	private final Set<JakeScope> resolvedScopes;

	private final JakeScope defaultScope;

	private final JakeScopeMapping defaultMapping;

	private final boolean refreshed;

	public Set<JakeScope> resolvedScopes() {
		return resolvedScopes;
	}

	/**
	 * Returns the default scope to use for the {@link JakeDependencies} to be resolved. <code>null</code> means
	 * no default scope.
	 * @see <a href="http://ant.apache.org/ivy/history/2.3.0/ivyfile/configurations.html">Ivy configuration doc</a>.
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
		return new JakeResolutionParameters(resolvedScopes, defaultScope, defaultMapping, refreshed);
	}

	private JakeResolutionParameters(Set<JakeScope> resolvedScopes,
			JakeScope defaultScope, JakeScopeMapping defaultMapping, boolean refreshed) {
		super();
		this.resolvedScopes = Collections.unmodifiableSet(resolvedScopes);
		this.defaultScope = defaultScope;
		this.defaultMapping = defaultMapping;
		this.refreshed = refreshed;
	}

	public static class NotDefaulted extends JakeResolutionParameters {

		private NotDefaulted(Set<JakeScope> scopes, JakeScope defaultScope, JakeScopeMapping defaultMapping, boolean refresh) {
			super(scopes, defaultScope, defaultMapping, refresh);
		}

		public JakeResolutionParameters withDefault(JakeScope jakeScope) {
			return new JakeResolutionParameters(resolvedScopes(), jakeScope, null, refreshed());
		}

		public JakeResolutionParameters withDefault(JakeScopeMapping jakeScopeMapping) {
			return new JakeResolutionParameters(resolvedScopes(), null, jakeScopeMapping, refreshed());
		}

	}




}
