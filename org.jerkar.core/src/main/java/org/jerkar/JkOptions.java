package org.jerkar;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsString;

public final class JkOptions {

	private static JkOptions INSTANCE = new JkOptions(loadJerkarAndUserProperties());

	private static boolean populated;

	static JkOptions instance() {
		return INSTANCE;
	}

	static synchronized void init(Map<String, String> options) {
		if (populated) {
			throw new IllegalStateException("The init method can be called only once.");
		}
		INSTANCE = new JkOptions(options);
		populated = true;
	}

	private final Map<String, String> freeOptions = new HashMap<String, String>();

	@JkOption("Set it to true to log more details.")
	private boolean verbose;

	@JkOption("Set it to true to turn off logs.")
	private boolean silent;

	@JkOption({"Set it to the full or short class name, to force the build class to use.",
	"Example : -buildClass=my.pack.FullBuild or -buildClass=FullBuild ."})
	private String buildClass;

	private JkOptions(Map<String, String> options) {

		// The string form options are stored in a static field of OptionStore class.
		// The static field is supposed to be populated prior this class is invoked.
		freeOptions.putAll(options);

		// Field are populated using reflection.
		populateFields(this, freeOptions);
	}

	@SuppressWarnings("unchecked")
	private JkOptions() {
		this(Collections.EMPTY_MAP);
	}

	public static void forceVerbose(boolean verbose) {
		INSTANCE.verbose = verbose;
	}

	public static void forceSilent(boolean silent) {
		INSTANCE.silent = silent;
	}

	public static boolean isVerbose() {
		return INSTANCE.verbose;
	}

	public static boolean isSilent() {
		if (INSTANCE == null) {
			return false;
		}
		return INSTANCE.silent;
	}

	static String buildClass() {
		return INSTANCE.buildClass;
	}


	public static boolean containsKey(String key) {
		return INSTANCE.freeOptions.containsKey(key);
	}

	public static String get(String key) {
		return INSTANCE.freeOptions.get(key);
	}

	public static Map<String, String> asMap() {
		return Collections.unmodifiableMap(INSTANCE.freeOptions);
	}

	public static Map<String, String> getAllStartingWith(String prefix) {
		final Map<String, String> result = new HashMap<String, String>();
		for (final String key : INSTANCE.freeOptions.keySet()) {
			if (key.startsWith(prefix)) {
				result.put(key, INSTANCE.freeOptions.get(key));
			}
		}
		return result;
	}


	/**
	 * Set the field values according to the target object according the string found in props arguments.
	 */
	static void populateFields(Object target, Map<String, String> props) {
		for (final Field field : optionField(target.getClass())) {
			final String name = field.getName();
			final Class<?> type = field.getType();
			final boolean present = props.containsKey(name);
			if (present) {
				String stringValue = props.get(name);

				// Special case for boolean : '-silent' is equivalent to '-silent=true'
				if ((type.equals(Boolean.class) || type.equals(boolean.class)) && present && stringValue == null) {
					stringValue = "true";
				}

				final Object value;
				if (stringValue == null || stringValue.equals("null")) {
					value = null;
				} else {
					try {
						value = JkUtilsString.parse(type, stringValue);
					} catch(final IllegalArgumentException e) {
						throw new JkException("Option " + name + "=" + stringValue
								+ " can't be parsed to type " + type.getName() + " : " + e.getMessage());
					}
				}
				JkUtilsReflect.setFieldValue(target, field, value);
			}
		}
	}

	static void populateFields(Object build) {
		populateFields(build, INSTANCE.freeOptions);
	}

	private static List<Field> optionField(Class<?> clazz) {
		return JkUtilsReflect.getAllDeclaredField(clazz, JkOption.class);
	}

	/**
	 * Returns a multi-line text standing the current option values.
	 */
	static String fieldOptionsToString(Object target) {
		final StringBuilder builder = new StringBuilder();
		boolean hasField = false;
		for (final Field field : optionField(target.getClass())) {
			hasField = true;
			final String name = field.getName();
			final String value = JkUtilsString.toString(JkUtilsReflect.getFieldValue(target, name));
			builder.append(name).append("=").append(value).append(", ");
		}
		if (hasField) {
			builder.delete(builder.length()-2, builder.length()-1);
		}
		return builder.toString();
	}

	static boolean hasFieldOptions(Class<?> clazz) {
		return ! optionField(clazz).isEmpty();
	}

	static String freeFormToString() {
		if (INSTANCE.freeOptions.isEmpty()) {
			return "none";
		}
		final StringBuilder builder = new StringBuilder();
		for (final Map.Entry<String, String> entry : INSTANCE.freeOptions.entrySet()) {
			final Object value = entry.getValue();
			if (value != null) {
				builder.append(entry.getKey()).append("=").append(value);
			} else {
				builder.append(entry.getKey());
			}
			builder.append(", ");

		}
		builder.delete(builder.length()-2, builder.length()-1);
		return builder.toString();
	}

	private static Map<String, String> loadJerkarAndUserProperties() {
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

}
