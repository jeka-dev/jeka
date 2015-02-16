package org.jake;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsString;

class CommandLine {

	public static CommandLine of(String[] words) {
		return new CommandLine(extractOptions(words), extractActions(words));
	}

	private static final String DEFAULT_METHOD = "base";

	public final Map<String, String> options;

	public final List<MethodInvocation> methods;

	private CommandLine(Map<String, String> options, List<MethodInvocation> methods) {
		super();
		this.options = options;
		this.methods = methods;
	}

	private static List<MethodInvocation> extractActions(String[] args) {
		final List<MethodInvocation> result = new LinkedList<MethodInvocation>();
		for (final String arg : args) {
			if (!arg.startsWith("-")) {
				result.add(MethodInvocation.parse(arg));
			}
		}
		if (result.isEmpty()) {
			result.add(MethodInvocation.normal(DEFAULT_METHOD));
		}
		return Collections.unmodifiableList(result);
	}

	private static Map<String, String> extractOptions(String[] args) {
		final Map<String, String> result = new HashMap<String, String>();
		for (final String arg : args) {
			if (arg.startsWith("-") && !arg.startsWith("-D")) {
				final int equalIndex = arg.indexOf("=");
				if (equalIndex <= -1) {
					result.put(arg.substring(1), null);
				} else {
					final String name = arg.substring(1, equalIndex);
					final String value = arg.substring(equalIndex+1);
					result.put(name, value);
				}
			}
		}
		return Collections.unmodifiableMap(result);
	}




	public static final class MethodInvocation {

		public static MethodInvocation parse(String word) {
			if (isPluginMethidInvokation(word)) {
				return pluginMethod(
						JakeUtilsString.substringBeforeFirst(word, "#"),
						JakeUtilsString.substringAfterLast(word, "#"));
			}
			return normal(word);
		}

		public static MethodInvocation normal(String name) {
			return new MethodInvocation(name, null);
		}

		public static MethodInvocation pluginMethod(String pluginName, String methodName) {
			JakeUtilsAssert.isTrue(pluginName != null && !pluginName.isEmpty(), "PluginName can' t ne null or empty");
			return new MethodInvocation(methodName, pluginName);
		}

		public final String methodName;

		public final String pluginName;

		private MethodInvocation(String methodName, String pluginName) {
			super();
			JakeUtilsAssert.isTrue(methodName != null && !methodName.isEmpty(), "PluginName can' t be null or empty");
			this.methodName = methodName;
			this.pluginName = pluginName;
		}

		private static boolean isPluginMethidInvokation(String word) {
			return JakeUtilsString.countOccurence(word, "#") == 1 && !word.startsWith("#");
		}

		public boolean isMethodPlugin() {
			return pluginName != null;
		}

		@Override
		public String toString() {
			if (pluginName == null) {
				return methodName;
			}
			return pluginName + "#" + methodName;
		}


	}

}
