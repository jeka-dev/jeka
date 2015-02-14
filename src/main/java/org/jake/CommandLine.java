package org.jake;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.utils.JakeUtilsString;

class CommandLine {

	public static CommandLine of(String[] words) {
		return new CommandLine(extractOptions(words), extractActions(words), extractPluginRefs(words));
	}

	private static final String DEFAULT_METHOD = "base";

	public final Map<String, String> options;

	public final List<String> methods;

	public final List<PluginRef> pluginRefs;

	private CommandLine(Map<String, String> options, List<String> methods,
			List<PluginRef> pluginRefs) {
		super();
		this.options = options;
		this.methods = methods;
		this.pluginRefs = pluginRefs;
	}

	private static List<String> extractActions(String[] args) {
		final List<String> result = new LinkedList<String>();
		for (final String arg : args) {
			if (!arg.startsWith("-")) {
				result.add(arg);
			}
		}
		if (result.isEmpty()) {
			result.add(DEFAULT_METHOD);
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

	private static List<PluginRef> extractPluginRefs(String[] words) {
		final List<PluginRef> result = new LinkedList<CommandLine.PluginRef>();
		for (final String word : words) {
			if (PluginRef.isPluginRef(word)) {
				result.add(PluginRef.parse(word));
			}
		}
		return Collections.unmodifiableList(result);
	}

	public static final class PluginRef {

		public static PluginRef parse(String word) {
			return new PluginRef(
					JakeUtilsString.substringBeforeFirst(word, "#"),
					JakeUtilsString.substringAfterLast(word, "#"));
		}

		public static boolean isPluginRef(String word) {
			return JakeUtilsString.countOccurence(word, "#") == 1 && !word.startsWith("#");
		}

		public final String name;

		public final String method;

		public PluginRef(String name, String method) {
			super();
			this.name = name;
			this.method = method;
		}





	}

}
