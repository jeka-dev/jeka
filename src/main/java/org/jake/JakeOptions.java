package org.jake;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

public class JakeOptions {

	protected static JakeOptions INSTANCE = new JakeOptions();

	private final Map<String, String> freeOptions = new HashMap<String, String>();

	@JakeOption(defaultValue="false", value="Set it to true to log more details.")
	private boolean verbose;

	@JakeOption(defaultValue="false", value="Set it to true to turn off logs.")
	private boolean silent;

	protected JakeOptions() {

		// The string form options are stored in a static field of OptionStore class.
		// The static field is supposed to be populated prior this class is
		// invoked.

		// To make it possible to pass options without populating OptionStore first,
		// we can use system properties. Only system property prefixed with 'jake.'
		// will be taken in account. Example: -Djake.verbose=true will set the option verbose=true.
		final Map<String, String> props = augmentWithSystemProps(OptionStore.options);

		// Field are populated using reflection.
		final List<Field> fields = JakeUtilsReflect.getAllDeclaredField(this.getClass(), JakeOption.class);
		final Set<String> names = new HashSet<String>();
		for (final Field field : fields) {
			final String name = field.getName();
			names.add(name);
			final JakeOption option = field.getAnnotation(JakeOption.class);
			final Class<?> type = field.getType();
			final boolean present = props.containsKey(name);
			String stringValue = present ? props.get(name) : option.defaultValue();

			// Special case for boolean : '-Osilent' is equivalent to '-Osilent=true'
			if (type.equals(Boolean.class) && present && stringValue == null) {
				stringValue = "true";
			}

			final Object value;
			if (stringValue.equals("null")) {
				value = null;
			} else {
				try {
					value = JakeUtilsString.parse(type, stringValue);
				} catch(final IllegalArgumentException e) {
					throw new JakeException("Option " + name + "=" + stringValue
							+ " can't be parsed to type " + type.getName() + " : " + e.getMessage());
				}
			}
			JakeUtilsReflect.setFieldValue(this, field, value);
		}
		this.freeOptions.clear();
		for (final Map.Entry<String, String> entry : props.entrySet()) {
			if (!names.contains(entry.getKey())) {
				this.freeOptions.put(entry.getKey(), entry.getValue());
			}
		}
	}


	private static Map<String, String> augmentWithSystemProps(Map<String,String> map) {
		final Map<String, String> result = new HashMap<String, String>(map);
		for (final Object name : System.getProperties().keySet()) {
			final String value = System.getProperty(name.toString());
			if (name.toString().startsWith("jake.")) {
				result.put(name.toString().substring(5), value);
			}
		}
		return result;

	}

	public static boolean isVerbose() {
		return INSTANCE.verbose;
	}

	public static boolean isSlent() {
		return INSTANCE. silent;
	}

	/**
	 * Returns a multi-line text standing the current option values.
	 */
	public List<String> toStrings() {
		final List<String> list = new LinkedList<String>();
		final List<Field> fields = JakeUtilsReflect.getAllDeclaredField(this.getClass(), JakeOption.class);
		StringBuilder builder = new StringBuilder("Standard options : ");
		for (final Field field : fields) {
			final String name = field.getName();
			final String value = JakeUtilsString.toString(JakeUtilsReflect.getFieldValue(this, name));
			builder.append(name).append("=").append(value).append(", ");
		}
		builder.delete(builder.length()-2, builder.length()-1);
		list.add(builder.toString() );
		if (!freeOptions.isEmpty()) {
			builder = new StringBuilder("Free options : ");
			for (final Map.Entry<String, String> entry : freeOptions.entrySet()) {
				builder.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
			}
			builder.delete(builder.length()-2, builder.length()-1);
			list.add(builder.toString());
		}
		return list;
	}

	/**
	 * Returns a multi-line text standing for the descriptions of the available options.
	 */
	public List<String> help() {
		final List<String> result = new LinkedList<String>();
		final List<Field> fields = JakeUtilsReflect.getAllDeclaredField(this.getClass(), JakeOption.class);
		for (final Field field : fields) {
			final String name = field.getName();
			final JakeOption option = field.getAnnotation(JakeOption.class);
			final Class<?> type = field.getType();
			final String defaultValue = option.defaultValue();
			final String string = name +" (" + type.getSimpleName()+", default="+ defaultValue+") : ";
			result.add( string + option.value()[0] );
			if (option.value().length > 1) {
				final String margin = JakeUtilsString.repeat(" ", string.length());
				for (int i=1; i < option.value().length; i++) {
					result.add(margin + option.value()[i]);
				}
			}
			result.add("");
		}
		return result;
	}

}
