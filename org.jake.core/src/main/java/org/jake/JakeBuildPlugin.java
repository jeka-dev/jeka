package org.jake;

import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeDependencyResolver;

/**
 * A plugin base class to extend to alter {@link JakeBuild} object.
 *
 * @param <T> The build class base the concrete class is made for.
 * 
 * @author Jerome Angibaud
 */
public abstract class JakeBuildPlugin {

	public Class<? extends JakeBuild> baseBuildClass() {
		return JakeBuild.class;
	}

	public abstract void configure(JakeBuild build);

	/**
	 * Override this method if you want your plugin do something while {@link JakeBuild#verify} is invoked.
	 */
	protected void verify() {
		// Do nothing by default
	}

	/**
	 * Override this method if the plugin need to alter the dependency resolver.
	 * 
	 * @see JakeBuild#dependencyResolver()
	 */
	protected JakeDependencyResolver alterDependencyResolver(JakeDependencyResolver original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the dependencies.
	 * 
	 * @see JakeBuild#dependencies
	 */
	protected JakeDependencies alterDependencies(JakeDependencies original) {
		return original;
	}

	static void applyVerify(Iterable<? extends JakeBuildPlugin> plugins) {
		for (final JakeBuildPlugin plugin : plugins) {
			plugin.verify();
		}
	}

	static JakeDependencyResolver applyDependencyResolver(Iterable<? extends JakeBuildPlugin> plugins, JakeDependencyResolver original) {
		JakeDependencyResolver result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = plugin.alterDependencyResolver(original);
		}
		return result;
	}

	static JakeDependencies applyDependencies(Iterable<? extends JakeBuildPlugin> plugins, JakeDependencies original) {
		JakeDependencies result = original;
		for (final JakeBuildPlugin plugin : plugins) {
			result = plugin.alterDependencies(original);
		}
		return result;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " : " + JakeOptions.fieldOptionsToString(this);
	}



}
