package org.jerkar;

import java.util.LinkedList;
import java.util.List;

import org.jerkar.utils.JkUtilsAssert;

public final class JkModelMethod {

	public static JkModelMethod normal(String name) {
		return new JkModelMethod(name, null);
	}

	public static List<JkModelMethod> normals(String ... names) {
		final List<JkModelMethod> result = new LinkedList<JkModelMethod>();
		for (final String name : names) {
			result.add(JkModelMethod.normal(name));
		}
		return result;
	}

	public static JkModelMethod pluginMethod(Class<? extends JkBuildPlugin> pluginClass, String methodName) {
		return new JkModelMethod(methodName, pluginClass);
	}

	private final String methodName;

	private final Class<? extends JkBuildPlugin> pluginClass;

	public String name() {
		return methodName;
	}

	public Class<? extends JkBuildPlugin> pluginClass() {
		return pluginClass;
	}

	private JkModelMethod(String methodName, Class<? extends JkBuildPlugin> pluginClass) {
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