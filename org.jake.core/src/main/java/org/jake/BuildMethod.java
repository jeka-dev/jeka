package org.jake;

import org.jake.utils.JakeUtilsAssert;

final class BuildMethod {

	public static BuildMethod normal(String name) {
		return new BuildMethod(name, null);
	}

	public static BuildMethod[] normals(String ... names) {
		final BuildMethod[] result = new BuildMethod[names.length];
		for (int i=0; i<names.length; i++) {
			result[i] = BuildMethod.normal(names[i]);
		}
		return result;
	}

	public static BuildMethod pluginMethod(Class<? extends JakeBuildPlugin> pluginClass, String methodName) {
		return new BuildMethod(methodName, pluginClass);
	}

	public final String methodName;

	public final Class<? extends JakeBuildPlugin> pluginClass;

	private BuildMethod(String methodName, Class<? extends JakeBuildPlugin> pluginClass) {
		super();
		JakeUtilsAssert.isTrue(methodName != null && !methodName.isEmpty(), "PluginName can' t be null or empty");
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