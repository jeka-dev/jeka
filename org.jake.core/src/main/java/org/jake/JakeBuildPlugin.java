package org.jake;

/**
 * A plugin base class to extend to alter {@link JakeBuild} object.
 *
 * @param <T> The build class base the concrete class is made for.
 * 
 * @author Jerome Angibaud
 */
public abstract class JakeBuildPlugin {

	public abstract void configure(JakeBuild build);

	/**
	 * Override this method if you want your plugin do something while {@link JakeBuild#verify} is invoked.
	 */
	protected void verify() {
		// Do nothing by default
	}

	static void applyVerify(Iterable<JakeBuildPlugin> plugins) {
		for (final JakeBuildPlugin plugin : plugins) {
			plugin.verify();
		}
	}

}
