package org.jake.depmanagement;

public class JakeResolutionScope {

	public static NotDefaulted of(JakeScope scope) {
		return new NotDefaulted(scope, null, null);
	}

	private final JakeScope dependencyScope;

	private final JakeScope defaultScope;

	private final JakeScopeMapping defaultMapping;

	public JakeScope dependencyScope() {
		return dependencyScope;
	}

	public JakeScope defaultScope() {
		return defaultScope;
	}

	public JakeScopeMapping defaultMapping() {
		return defaultMapping;
	}

	private JakeResolutionScope(JakeScope dependencyScope,
			JakeScope defaultScope, JakeScopeMapping defaultMapping) {
		super();
		this.dependencyScope = dependencyScope;
		this.defaultScope = defaultScope;
		this.defaultMapping = defaultMapping;
	}

	public static class NotDefaulted extends JakeResolutionScope {

		private NotDefaulted(JakeScope scope, JakeScope defaultScope, JakeScopeMapping defaultMapping) {
			super(scope, defaultScope, defaultMapping);
		}

		public JakeResolutionScope withDefault(JakeScope jakeScope) {
			return new JakeResolutionScope(dependencyScope(), jakeScope, null);
		}

		public JakeResolutionScope withDefault(JakeScopeMapping jakeScopeMapping) {
			return new JakeResolutionScope(dependencyScope(), null, jakeScopeMapping);
		}

	}




}
