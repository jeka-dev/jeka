package org.jerkar;

import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkDependencyResolver;

/**
 * A plugin base class to extend to alter {@link JkBuild} object.
 * 
 * @author Jerome Angibaud
 */
public abstract class JkBuildPlugin {

	public Class<? extends JkBuild> baseBuildClass() {
		return JkBuild.class;
	}

	public abstract void configure(JkBuild build);

	/**
	 * Override this method if you want your plugin do something while {@link JkBuild#verify} is invoked.
	 */
	protected void verify() {
		// Do nothing by default
	}



	/**
	 * Override this method if the plugin need to alter the dependency resolver.
	 * 
	 * @see JkBuild#dependencyResolver()
	 */
	protected JkDependencyResolver alterDependencyResolver(JkDependencyResolver original) {
		return original;
	}

	/**
	 * Override this method if the plugin need to alter the dependencies.
	 * 
	 * @see JkBuild#dependencies
	 */
	protected JkDependencies alterDependencies(JkDependencies original) {
		return original;
	}

	/**
	 * Override the method if the plugin need to enhance scaffolding
	 */
	protected JkScaffolder enhanceScaffold(JkScaffolder original) {
		return original;
	}

	static void applyVerify(Iterable<? extends JkBuildPlugin> plugins) {
		for (final JkBuildPlugin plugin : plugins) {
			plugin.verify();
		}
	}

	static JkDependencyResolver applyDependencyResolver(Iterable<? extends JkBuildPlugin> plugins, JkDependencyResolver original) {
		JkDependencyResolver result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = plugin.alterDependencyResolver(original);
		}
		return result;
	}

	static JkDependencies applyDependencies(Iterable<? extends JkBuildPlugin> plugins, JkDependencies original) {
		JkDependencies result = original;
		for (final JkBuildPlugin plugin : plugins) {
			result = plugin.alterDependencies(original);
		}
		return result;
	}

	static JkScaffolder enhanceScafforld(Iterable<? extends JkBuildPlugin> plugins, JkScaffolder scaffolder) {
		JkScaffolder result = scaffolder;
		for (final JkBuildPlugin plugin : plugins) {
			result = plugin.enhanceScaffold(result);
		}
		return result;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " : " + JkOptions.fieldOptionsToString(this);
	}



}
