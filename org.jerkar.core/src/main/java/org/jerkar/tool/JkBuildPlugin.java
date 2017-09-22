package org.jerkar.tool;

import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;

/**
 * A plugin base class to extend to alter {@link JkBuild} object.
 *
 * @author Jerome Angibaud
 */
@Deprecated
public abstract class JkBuildPlugin {

    /**
     * Returns the class this plugin is made for.
     */
    public Class<? extends JkBuild> baseBuildClass() {
        return JkBuild.class;
    }

    /**
     * Configure this plugin according the build instance it is applied on.
     */
    public abstract void configure(JkBuild build);

    /**
     * Override this method if this plugin does something while
     * {@link JkBuild#verify} is invoked.
     */
    protected void verify() {
        // Do nothing by default
    }

    /**
     * Override this method if this plugin does something while {@link JkBuild#scaffold()}
     * is invoked.
     */
    protected void scaffold() {
        // Do nothing by default
    }

    /**
     * Override this method if this plugin add/remove some slave projects. The original importedBuilds
     * are passed as parameter so plugins are aware of already declared importedBuilds.
     * Note that you are not supposed to modify the list passed as parameters and that
     * the returned list is supposed to be the exhaustive list of slave projects, so if the plugin do not remove
     * any slave from the original list, the returned list should contains the list passed as parameter.
     */
    protected List<JkBuild> slaves(List<JkBuild> originalSlaves) {
        return originalSlaves;
    }

    /**
     * Override this method if this plugin needs to alter the dependency resolver.
     *
     * @see JkBuildDependencySupport#dependencyResolver()
     */
    protected JkDependencyResolver alterDependencyResolver(JkDependencyResolver original) {
        return original;
    }

    /**
     * Override this method if the plugin need to alter the dependencies.
     *
     * @see JkBuildDependencySupport#dependencies
     */
    protected JkDependencies alterDependencies(JkDependencies original) {
        return original;
    }


    static void applyVerify(Iterable<? extends JkBuildPlugin> plugins) {
        for (final JkBuildPlugin plugin : plugins) {
            plugin.verify();
        }
    }

    static JkDependencyResolver applyDependencyResolver(
            Iterable<? extends JkBuildPlugin> plugins, JkDependencyResolver original) {
        JkDependencyResolver result = original;
        for (final JkBuildPlugin plugin : plugins) {
            result = plugin.alterDependencyResolver(original);
        }
        return result;
    }

    static JkDependencies applyDependencies(Iterable<? extends JkBuildPlugin> plugins,
            JkDependencies original) {
        JkDependencies result = original;
        for (final JkBuildPlugin plugin : plugins) {
            result = plugin.alterDependencies(original);
        }
        return result;
    }

    static void applyScaffold(Iterable<? extends JkBuildPlugin> plugins) {
        for (final JkBuildPlugin plugin : plugins) {
            plugin.scaffold();
        }
    }

    static List<JkBuild> applyPluginsToImportedBuilds(Iterable<? extends JkBuildPlugin> plugins, List<JkBuild> importedBuilds) {
        List<JkBuild> result = importedBuilds;
        for (final JkBuildPlugin plugin : plugins) {
            result = plugin.slaves(result);
        }
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " : " + JkOptions.fieldOptionsToString(this);
    }

}
