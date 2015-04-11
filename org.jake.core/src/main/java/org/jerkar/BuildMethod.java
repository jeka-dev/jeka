package org.jerkar;

import java.util.LinkedList;
import java.util.List;

import org.jerkar.utils.JkUtilsAssert;

final class BuildMethod {

	public static BuildMethod normal(String name) {
		return new BuildMethod(name, null);
	}

	public static List<BuildMethod> normals(String ... names) {
		final List<BuildMethod> result = new LinkedList<BuildMethod>();
		for (final String name : names) {
			result.add(BuildMethod.normal(name));
		}
		return result;
	}

	public static BuildMethod pluginMethod(Class<? extends JkBuildPlugin> pluginClass, String methodName) {
		return new BuildMethod(methodName, pluginClass);
	}

	public final String methodName;

	public final Class<? extends JkBuildPlugin> pluginClass;

	private BuildMethod(String methodName, Class<? extends JkBuildPlugin> pluginClass) {
		super();
		JkUtilsAssert.isTrue(methodName != null && !methodName.isEmpty(), "PluginName can' t be null or empty");
		this.methodName = methodName;
		this.pluginClass = pluginClass;
	}

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