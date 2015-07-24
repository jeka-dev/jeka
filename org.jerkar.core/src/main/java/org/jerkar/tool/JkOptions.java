package org.jerkar.tool;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

public final class JkOptions {

	private static JkOptions INSTANCE = new JkOptions(loadSystemAndUserOptions());

	private final Map<String, String> props = new HashMap<String, String>();

	private static boolean populated;

	static JkOptions instance() {
		return INSTANCE;
	}

	static synchronized void init(Map<String, String> options) {
		if (populated) {
			throw new IllegalStateException("The init method can be called only once.");
		}
		final Map<String, String> map = new HashMap<String, String>();
		map.putAll(options);
		INSTANCE = new JkOptions(map);
		populated = true;
	}

	private JkOptions(Map<String, String> options) {
		props.putAll(options);
	}

	@SuppressWarnings("unchecked")
	private JkOptions() {
		this(Collections.EMPTY_MAP);
	}

	public static boolean containsKey(String key) {
		return INSTANCE.props.containsKey(key);
	}

	public static String get(String key) {
		return INSTANCE.props.get(key);
	}

	public static Map<String, String> getAll() {
		return Collections.unmodifiableMap(INSTANCE.props);
	}

	public static Map<String, String> getAllStartingWith(String prefix) {
		final Map<String, String> result = new HashMap<String, String>();
		for (final String key : INSTANCE.props.keySet()) {
			if (key.startsWith(prefix)) {
				result.put(key, INSTANCE.props.get(key));
			}
		}
		return result;
	}


	/**
	 * Set the field values according to the target object according the string found in props arguments.
	 */
	static void populateFields(Object target, Map<String, String> props) {
		OptionInjector.inject(target, props);
	}

	static void populateFields(Object build) {
		populateFields(build, INSTANCE.props);
	}


	static Map<String, String> toDisplayedMap(Map<String, String> props) {
		final Map<String, String> result = new TreeMap<String, String>();
		for (final Map.Entry<String, String> entry : props.entrySet()) {
			final String value;
			if (JkUtilsString.firstMatching(entry.getKey().toLowerCase(), "password", "pwd") != null
					&& entry.getValue() != null) {
				value = "*****";
			} else {
				value = entry.getValue();
			}
			result.put(entry.getKey(), value);

		}
		return result;
	}

	private static Map<String, String> loadSystemAndUserOptions() {
		final File propFile = new File(JkLocator.jerkarHome(), "options.properties");
		final Map<String, String> result = new HashMap<String, String>();
		if (propFile.exists()) {
			result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
		}
		final File userPropFile = new File(JkLocator.jerkarUserHome(), "options.properties");
		if (userPropFile.exists()) {
			result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
		}
		return result;
	}

	static String fieldOptionsToString(Object object) {
		final Map<String, String> map = JkOptions.toDisplayedMap(OptionInjector.injectedFields(object));
		return JkUtilsIterable.toString(map);
	}

}
