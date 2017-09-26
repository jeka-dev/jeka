package org.jerkar.tool;

import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsAssert;

/**
 * Reference to a build method (callable Jerkar from command line). A build
 * method can be defined on a build class or on a plug-in.
 *
 * @author Jerome Angibaud
 */
final class BuildMethod {

    /**
     * Creates a build method defined on the build class.
     */
    public static BuildMethod normal(String name) {
        return new BuildMethod(name, null);
    }

    /**
     * Creates a list of build method reference defined on the build class.
     */
    public static List<BuildMethod> normals(String... names) {
        final List<BuildMethod> result = new LinkedList<>();
        for (final String name : names) {
            result.add(BuildMethod.normal(name));
        }
        return result;
    }

    /**
     * Creates a build method reference that is defined on a plugin.
     */
    public static BuildMethod pluginMethod(Class<? extends JkBuildPlugin2> pluginClass,
                                           String methodName) {
        return new BuildMethod(methodName, pluginClass);
    }

    private final String methodName;

    private final Class<? extends JkBuildPlugin2> pluginClass;

    /**
     * Returns the name of the method.
     */
    public String name() {
        return methodName;
    }

    /**
     * Returns the plugin on which this method is defined. Returns
     * <code>null</code> if defined on a build class.
     */
    public Class<? extends JkBuildPlugin2> pluginClass() {
        return pluginClass;
    }

    private BuildMethod(String methodName, Class<? extends JkBuildPlugin2> pluginClass) {
        super();
        JkUtilsAssert.isTrue(methodName != null && !methodName.isEmpty(),
                "PluginName can' t be null or empty");
        this.methodName = methodName;
        this.pluginClass = pluginClass;
    }

    /**
     * Returns true if this method is defined on a plug-in.
     */
    public boolean isMethodPlugin() {
        return pluginClass != null;
    }

    @Override
    public String toString() {
        if (pluginClass == null) {
            return methodName;
        }
        return pluginClass.getName() + "#" + methodName;
    }

}