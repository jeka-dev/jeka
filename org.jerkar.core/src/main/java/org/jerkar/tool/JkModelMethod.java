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
public final class JkModelMethod {

    /**
     * Creates a build method defined on the build class.
     */
    public static JkModelMethod normal(String name) {
	return new JkModelMethod(name, null);
    }

    /**
     * Creates a list of build method reference defined on the build class.
     */
    public static List<JkModelMethod> normals(String... names) {
	final List<JkModelMethod> result = new LinkedList<JkModelMethod>();
	for (final String name : names) {
	    result.add(JkModelMethod.normal(name));
	}
	return result;
    }

    /**
     * Creates a build method reference that is defined on a plugin.
     */
    public static JkModelMethod pluginMethod(Class<? extends JkBuildPlugin> pluginClass, String methodName) {
	return new JkModelMethod(methodName, pluginClass);
    }

    private final String methodName;

    private final Class<? extends JkBuildPlugin> pluginClass;

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
    public Class<? extends JkBuildPlugin> pluginClass() {
	return pluginClass;
    }

    private JkModelMethod(String methodName, Class<? extends JkBuildPlugin> pluginClass) {
	super();
	JkUtilsAssert.isTrue(methodName != null && !methodName.isEmpty(), "PluginName can' t be null or empty");
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