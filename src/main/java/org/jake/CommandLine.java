package org.jake;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.JakePlugins.JakePluginSetup;
import org.jake.utils.JakeUtilsAssert;
import org.jake.utils.JakeUtilsString;

class CommandLine {

	public static CommandLine of(String[] words) {
		return new CommandLine(extractOptions(words), extractMethods(words), extractPluginSetup(words) );
	}

	private static final String DEFAULT_METHOD = "base";

	private final Map<String, String> options;

	private final List<MethodInvocation> methods;

	private final Collection<JakePluginSetup> pluginSetups;

	private CommandLine(Map<String, String> options, List<MethodInvocation> methods, Collection<JakePluginSetup> pluginSetups) {
		super();
		this.options = options;
		this.methods = methods;
		this.pluginSetups = pluginSetups;
	}

	public Map<String, String> options() {
		return this.options;
	}

	public List<MethodInvocation> methods() {
		return methods;
	}

	public Collection<JakePluginSetup> pluginSetups() {
		return this.pluginSetups;
	}

	private static List<MethodInvocation> extractMethods(String[] args) {
		final List<MethodInvocation> result = new LinkedList<MethodInvocation>();
		for (final String arg : args) {
			if (!arg.startsWith("-") && !arg.endsWith("#")) {
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
					final String key = arg.substring(1);
					if (!key.contains("#")) {
						result.put(arg.substring(1), null);
					}

				} else {
					final String key = arg.substring(1, equalIndex);
					if (!key.contains("#")) {
						final String value = arg.substring(equalIndex+1);
						result.put(key, value);
					}
				}
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static Collection<JakePluginSetup> extractPluginSetup(String args[]) {
		final Map<String, JakePluginSetup> setups = new HashMap<String, JakePlugins.JakePluginSetup>();
		for (final String word : args) {
			if (MethodInvocation.isPluginMethodInvokation(word)) {
				final String pluginName = JakeUtilsString.substringBeforeFirst(word, "#");
				if (!setups.containsKey(pluginName)) {
					setups.put(pluginName, JakePluginSetup.of(pluginName));
				}
			} else if (isPluginOption(word)) {
				final String pluginName = JakeUtilsString.substringBeforeFirst(word, "#").substring(1);
				JakePluginSetup setup = setups.get(pluginName);
				if (setup == null) {
					setup = JakePluginSetup.of(pluginName);
					setups.put(pluginName, setup);
				}
				final int equalIndex = word.indexOf("=");
				if (equalIndex <= -1) {
					final String key = JakeUtilsString.substringAfterFirst(word, "#");
					setups.put(pluginName, setup.with(key, null));
				} else {
					final String key = JakeUtilsString.substringBeforeFirst( JakeUtilsString.substringAfterFirst(word, "#"), "=");
					final String value = word.substring(equalIndex+1);
					setups.put(pluginName, setup.with(key, value));
				}
			} else {
				final String pluginName = JakeUtilsString.substringBeforeFirst(word, "#");
				setups.put(pluginName, JakePluginSetup.of(pluginName));
			}
		}
		return setups.values();
	}

	private static boolean isPluginOption(String word) {
		return word.startsWith("-") && word.indexOf("#") > 2;
	}




	public static final class MethodInvocation {

		public static MethodInvocation parse(String word) {
			if (isPluginMethodInvokation(word)) {
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

		private static boolean isPluginMethodInvokation(String word) {
			return JakeUtilsString.countOccurence(word, '#') == 1 && !word.startsWith("#") && !word.endsWith("#");
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
