package org.jake;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

public final class JakeOptions {

	@SuppressWarnings("unchecked")
	private static JakeOptions INSTANCE = new JakeOptions(Collections.EMPTY_MAP);

	static JakeOptions instance() {
		return INSTANCE;
	}

	static synchronized void populate(Map<String, String> options) {
		INSTANCE = new JakeOptions(options);
	}

	private final Map<String, String> freeOptions = new HashMap<String, String>();

	@JakeOption("Set it to true to log more details.")
	private boolean verbose;

	@JakeOption("Set it to true to turn off logs.")
	private boolean silent;

	@JakeOption({"Set it to the full or short class name, to force the build class to use.",
	"Example : -buildClass=my.pack.FullBuild or -buildClass=FullBuild ."})
	private String buildClass;

	@JakeOption({"All jar files under [JAKE HOME]/ext, are part of Jake classpath. you can add libs to this folder or ",
		"use this option to augment Jake classpath.",
		"If it is a folder, then all jar files under this folders will be added to the classpath. If it is a single jar file, ",
		"only this jar will be added. You can specify many folder/jar using ';'.",
	"Exemple : -extraJakeLibs=/usr/java/libs/sonar;/usr/java/libs/apache/lucene.jar ."})
	private String extraJakeLibs;

	private JakeOptions(Map<String, String> options) {

		// The string form options are stored in a static field of OptionStore class.
		// The static field is supposed to be populated prior this class is invoked.
		freeOptions.putAll(options);

		// Field are populated using reflection.
		populateFields(this, freeOptions);
	}

	@SuppressWarnings("unchecked")
	private JakeOptions() {
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

	public static List<File> extraJakeClasspath() {
		if (INSTANCE.extraJakeLibs == null || INSTANCE.extraJakeLibs.trim().isEmpty()) {
			return Collections.emptyList();
		}
		final String[] items = JakeUtilsString.split(INSTANCE.extraJakeLibs, ";");
		final List<File> result = new LinkedList<File>();
		for (final String item : items) {
			final File file = new File(item.trim());
			if (!file.exists()) {
				throw new IllegalStateException("Extra jake libs " + file.getPath() + " can't be found.");
			}
			if (file.isDirectory()) {
				result.addAll(JakeDir.of(file).include("**/*.jar", "**/*.zip").files());
			} else {
				result.add(file);
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
						value = JakeUtilsString.parse(type, stringValue);
					} catch(final IllegalArgumentException e) {
						throw new JakeException("Option " + name + "=" + stringValue
								+ " can't be parsed to type " + type.getName() + " : " + e.getMessage());
					}
				}
				JakeUtilsReflect.setFieldValue(target, field, value);
			}
		}
	}

	static void populateFields(JakeBuild build) {
		populateFields(build, INSTANCE.freeOptions);
	}

	private static List<Field> optionField(Class<?> clazz) {
		return JakeUtilsReflect.getAllDeclaredField(clazz, JakeOption.class);
	}

	/**
	 * Returns a multi-line text standing the current option values.
	 */
	static String fieldOptionsToString(Object target) {
		final StringBuilder builder = new StringBuilder("");
		for (final Field field : optionField(target.getClass())) {
			final String name = field.getName();
			final String value = JakeUtilsString.toString(JakeUtilsReflect.getFieldValue(target, name));
			builder.append(name).append("=").append(value).append(", ");
		}
		builder.delete(builder.length()-2, builder.length()-1);
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

	/**
	 * Returns a multi-line text standing for the descriptions of the available options.
	 */
	@SuppressWarnings("unchecked")
	static List<String> help(Class<?> clazz) {
		return JakeUtilsIterable.concatLists(doHelp(JakeOptions.class), doHelp(clazz));
	}

	static List<String> helpClassOnly(Class<?> clazz) {
		return doHelp(clazz);
	}

	private static List<String> doHelp(Class<?> clazz) {
		final List<String> result = new LinkedList<String>();
		final Object defaultObject = JakeUtilsReflect.newInstance(clazz);
		for (final Field field : optionField(clazz)) {
			final String name = field.getName();
			final JakeOption annotation = field.getAnnotation(JakeOption.class);
			final Class<?> type = field.getType();
			final Object defaultValue = JakeUtilsReflect.getFieldValue(defaultObject, field);
			final String string = name + " (" + type.getSimpleName() + ", default="+ stringOrNull(defaultValue) + ") : ";
			result.add( string + annotation.value()[0] );
			final String margin = JakeUtilsString.repeat(" ", string.length());
			if (annotation.value().length > 1) {
				for (int i=1; i < annotation.value().length; i++) {
					result.add(margin + annotation.value()[i]);
				}
			}
			if (type.isEnum()) {
				result.add(margin + "Valid values are : " + enumValues(type) + ".");
			}
			result.add("");
		}
		return result;
	}

	private static String stringOrNull(Object object) {
		if (object == null) {
			return null;
		}
		return object.toString();
	}

	private static String enumValues(Class<?> enumClass) {
		final Object[] values = enumClass.getEnumConstants();
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			result.append(values[i].toString());
			if (i+1 < values.length) {
				result.append(", ");
			}
		}
		return result.toString();
	}

}
