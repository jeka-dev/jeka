package org.jerkar;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsString;

class OptionInjector {

	private static final Object UNHANDLED_TYPE = new Object();

	public static Map<String, String> injectedFields(Object inspected, Map<String, String> props) {
		final List<Field> fields = involvedFields(inspected, props);


	}

	public static void inject(Object target, Map<String, String> props) {
		final List<Field> fields = involvedFields(target, props);
		//final List<Field> fields = optionField(target.getClass());
		for (final Field field : fields) {
			inject(target, field, props);
		}
	}

	private static List<Field> involvedFields(Object inspected, Map<String, String> props) {
		final List<Field> fields = JkUtilsReflect.getAllDeclaredField(inspected.getClass(), true);
		final Set<String> candidateFields = candidateFields(props);
		for (final Iterator<Field> it = fields.iterator(); it.hasNext();) {
			final Field field = it.next();
			if (!candidateFields.contains(field.getName())) {
				it.remove();
			}
			if (Modifier.isPrivate(field.getModifiers()) && field.getAnnotation(JkDoc.class) == null) {
				it.remove();
			}
			if (Modifier.isStatic(field.getModifiers())) {
				it.remove();
			}
		}
		return fields;
	}

	@SuppressWarnings("unchecked")
	private static void inject(Object target, Field field, Map<String, String> props) {
		final String name = field.getName();
		final Class<?> type = field.getType();
		final boolean present = props.containsKey(name);
		if (present) {
			final String stringValue = props.get(name);
			Object value;
			if (stringValue == null ) {
				value = defaultValue(type);
			} else {
				value = parse((Class<Object>) type, stringValue);
			}
			if (value == UNHANDLED_TYPE) {
				throw new IllegalArgumentException("Class " + target.getClass().getName() +", field "
						+ name +", can't handle type " + type);
			}
			JkUtilsReflect.setFieldValue(target, field, value);
		} else if (hasKeyStartingWith(name + ".", props)) {
			Object value = JkUtilsReflect.getFieldValue(target, field);
			if (value == null) {
				value = JkUtilsReflect.newInstance(field.getType());
				JkUtilsReflect.setFieldValue(target, field, value);
			}
			final Map<String, String> subProps = extractKeyStartingWith(name+".", props);
			inject(value, subProps);
		}


	}

	private static void markInjected(Map<String, String> context, String prefix, Object target, Field field, Map<String, String> props) {
		final String name = field.getName();
		final Class<?> type = field.getType();
		final boolean present = props.containsKey(name);
		if (present) {
			final String stringValue = props.get(name);
			Object value;
			if (stringValue == null ) {
				value = defaultValue(type);
			} else {
				value = parse((Class<Object>) type, stringValue);
			}
			if (value == UNHANDLED_TYPE) {
				throw new IllegalArgumentException("Class " + target.getClass().getName() +", field "
						+ name +", can't handle type " + type);
			}
			context.put(prefix + name, props.get(prefix + name));
		} else if (hasKeyStartingWith(name + ".", props)) {
			Object value = JkUtilsReflect.getFieldValue(target, field);
			if (value == null) {
				value = JkUtilsReflect.newInstance(field.getType());
				JkUtilsReflect.setFieldValue(target, field, value);
			}
			final Map<String, String> subProps = extractKeyStartingWith(name+".", props);
			inject(value, subProps);
		}

	}

	private static Object defaultValue(Class<?> type) {
		if (type.equals(Boolean.class) || type.equals(boolean.class)) {
			return true;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Object parse(Class<Object> type, String stringValue) throws IllegalArgumentException {
		if (type.equals(String.class)) {
			return stringValue;
		}

		if (type.equals(Boolean.class) || type.equals(boolean.class)) {
			return Boolean.valueOf(stringValue);
		}
		try {
			if (type.equals(Integer.class) || type.equals(int.class)) {
				return Integer.valueOf(stringValue);
			}
			if (type.equals(Long.class) || type.equals(long.class)) {
				return Long.valueOf(stringValue);
			}
			if (type.equals(Short.class) || type.equals(short.class)) {
				return Short.valueOf(stringValue);
			}
			if (type.equals(Byte.class) || type.equals(byte.class)) {
				return Byte.valueOf(stringValue);
			}
			if (type.equals(Double.class) || type.equals(double.class)) {
				return Double.valueOf(stringValue);
			}
			if (type.equals(Float.class) || type.equals(float.class)) {
				return Float.valueOf(stringValue);
			}
			if (type.equals(File.class)) {
				return new File(stringValue);
			}
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		if (type.isEnum()) {
			@SuppressWarnings("rawtypes")
			final
			Class enumType = type;
			return Enum.valueOf(enumType, stringValue);
		}
		return UNHANDLED_TYPE;

	}

	private static boolean hasKeyStartingWith(String prefix, Map<String, String> values)  {
		for (final String string : values.keySet()) {
			if (string.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private static Map<String, String> extractKeyStartingWith(String prefix, Map<String, String> values)  {
		final Map<String, String> result = new HashMap<String, String>();
		for (final String string : values.keySet()) {
			if (string.startsWith(prefix)) {
				result.put(string.substring(prefix.length()), values.get(string));
			}
		}
		return result;
	}

	private static List<Field> optionField(Class<?> clazz) {
		return JkUtilsReflect.getAllDeclaredField(clazz, JkOption.class);
	}

	private static Set<String> candidateFields(Map<String, String> props) {
		final Set<String> result = new HashSet<String>();
		for (final String key : props.keySet()) {
			if (key.contains(".")) {
				result.add(JkUtilsString.substringBeforeFirst(key, "."));
			} else {
				result.add(key);
			}
		}
		return result;
	}

}
