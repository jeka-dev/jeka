package org.jerkar;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIterable;
import org.jerkar.utils.JkUtilsObject;
import org.jerkar.utils.JkUtilsString;

public final class JkOptions {

	private static JkOptions INSTANCE = new JkOptions(loadSystemAndUserOptions());

	private static final String BUILD_CLASS = "buildClass";

	private static final String VERBOSE = "verbose";

	private static final String SILENT = "silent";

	private final Map<String, String> props = new HashMap<String, String>();

	private static boolean populated;

	private boolean silent;

	private boolean verbose;

	private final String buildClass;

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
		this.verbose = JkUtilsObject.firstNonNull(Boolean.getBoolean(options.get(VERBOSE)), false);
		this.silent = JkUtilsObject.firstNonNull(Boolean.getBoolean(options.get(SILENT)), false);
		this.buildClass = options.get(BUILD_CLASS);
	}

	@SuppressWarnings("unchecked")
	private JkOptions() {
		this(Collections.EMPTY_MAP);
	}

	/**
	 * Changes the 'verbose' option dynamically. Set it to true to log more details.
	 */
	public static void forceVerbose(boolean verbose) {
		INSTANCE.verbose = verbose;
	}

	/**
	 * Changes the 'silent' option dynamically. Set it to true to turn off logs.
	 */
	public static void forceSilent(boolean silent) {
		INSTANCE.silent = silent;
	}

	/**
	 * Returns if the log is in verbose mode.
	 */
	public static boolean isVerbose() {
		if (INSTANCE == null) {
			return false;
		}
		return INSTANCE.verbose;
	}

	/**
	 * Returns if the log is turn off.
	 */
	public static boolean isSilent() {
		if (INSTANCE == null) {
			return false;
		}
		return INSTANCE.silent;
	}

	static String standardOptions() {
		return "verbose=" + INSTANCE.verbose + ", silent= "
				+ INSTANCE.silent + ", buildClass=" + JkUtilsObject.toString(INSTANCE.buildClass);
	}

	static String buildClass() {
		return INSTANCE.buildClass;
	}

	public static boolean containsKey(String key) {
		return INSTANCE.props.containsKey(key);
	}

	public static String get(String key) {
		return INSTANCE.props.get(key);
	}

	public static Map<String, String> asMap() {
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
