package org.jake;

import org.jake.utils.JakeUtilsAssert;

final class BuildMethod {

	public static BuildMethod normal(String name) {
		return new BuildMethod(name, null);
	}

	public static BuildMethod pluginMethod(Class<JakeBuildPlugin> pluginClass, String methodName) {
		return new BuildMethod(methodName, pluginClass);
	}

	public final String methodName;

	public final Class<JakeBuildPlugin> pluginClass;

	private BuildMethod(String methodName, Class<JakeBuildPlugin> pluginClass) {
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