package org.jerkar.tool;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;

final class OptionInjector {

    private static final String UNHANDLED_TYPE = "";

    public static void inject(Object target, Map<String, String> props) {
        final List<Field> fields = involvedFields(target, props);
        // final List<Field> fields = optionField(target.getClass());
        for (final Field field : fields) {
            inject(target, field, props);
        }
    }

    private static List<Field> involvedFields(Object inspected, Map<String, String> props) {
        final List<Field> fields = JkUtilsReflect.getAllDeclaredField(inspected.getClass(), true);
        final Set<String> candidateFields = candidateFields(props);
        final List<Field> result = new LinkedList<>();
        for (final Field field : fields) {
            if (candidateFields.contains(field.getName()) && injectable(field)) {
                result.add(field);
            }
        }
        return result;
    }

    private static boolean injectable(Field field) {
        if (Modifier.isPrivate(field.getModifiers()) && field.getAnnotation(JkDoc.class) == null) {
            return false;
        }
        return !Modifier.isStatic(field.getModifiers());
    }

    @SuppressWarnings("unchecked")
    private static void inject(Object target, Field field, Map<String, String> props) {
        final String name = field.getName();
        final Class<?> type = field.getType();
        final boolean present = props.containsKey(name);
        if (present) {
            final String stringValue = props.get(name);
            Object value;
            if (stringValue == null) {
                value = defaultValue(type);
            } else {
                value = parse((Class<Object>) type, stringValue);
            }
            if (value == UNHANDLED_TYPE) {
                throw new IllegalArgumentException("Class " + target.getClass().getName()
                        + ", field " + name + ", can't handle type " + type);
            }
            JkUtilsReflect.setFieldValue(target, field, value);
        } else if (hasKeyStartingWith(name + ".", props)) {
            Object value = JkUtilsReflect.getFieldValue(target, field);
            if (value == null) {
                value = JkUtilsReflect.newInstance(field.getType());
                JkUtilsReflect.setFieldValue(target, field, value);
            }
            final Map<String, String> subProps = extractKeyStartingWith(name + ".", props);
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
    private static Object parse(Class<Object> type, String stringValue)
            throws IllegalArgumentException {
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
            final Class enumType = type;
            return Enum.valueOf(enumType, stringValue);
        }
        return UNHANDLED_TYPE;
    }

    private static boolean hasKeyStartingWith(String prefix, Map<String, String> values) {
        for (final String string : values.keySet()) {
            if (string.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> extractKeyStartingWith(String prefix,
            Map<String, String> values) {
        final Map<String, String> result = new HashMap<>();
        for (final String string : values.keySet()) {
            if (string.startsWith(prefix)) {
                result.put(string.substring(prefix.length()), values.get(string));
            }
        }
        return result;
    }

    private static Set<String> candidateFields(Map<String, String> props) {
        final Set<String> result = new HashSet<>();
        for (final String key : props.keySet()) {
            if (key.contains(".")) {
                result.add(JkUtilsString.substringBeforeFirst(key, "."));
            } else {
                result.add(key);
            }
        }
        return result;
    }

    // ----------------------------------
    // Methods to inspect field values
    // ----------------------------------

    public static Map<String, String> injectedFields(Object inspected) {
        return injectedFields("", inspected);
    }

    private static Map<String, String> injectedFields(String context, Object inspected) {
        final List<Field> fields = JkUtilsReflect.getAllDeclaredField(inspected.getClass(), true);
        fields.removeIf(field -> !injectable(field));
        final Map<String, String> result = new TreeMap<>();
        for (final Field field : fields) {
            final Object value = JkUtilsReflect.getFieldValue(inspected, field);
            final String stringValue = stringValue(value);

            // Composite value
            if (stringValue == UNHANDLED_TYPE) {
                final String subContext = context + field.getName() + ".";
                result.putAll(injectedFields(subContext, value));
            } else {
                result.put(context + field.getName(), stringValue);
            }
        }
        return result;
    }

    private static String stringValue(Object value) throws IllegalArgumentException {
        if (value == null) {
            return "null";
        }
        final Class<?> type = value.getClass();
        if (type.equals(String.class)) {
            return (String) value;
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return Boolean.toString((Boolean) value);
        }
        try {
            if (type.equals(Integer.class) || type.equals(int.class)) {
                return Integer.toString((Integer) value);
            }
            if (type.equals(Long.class) || type.equals(long.class)) {
                return Long.toString((Long) value);
            }
            if (type.equals(Short.class) || type.equals(short.class)) {
                return Short.toString((Short) value);
            }
            if (type.equals(Byte.class) || type.equals(byte.class)) {
                return Byte.toString((Byte) value);
            }
            if (type.equals(Double.class) || type.equals(double.class)) {
                return Double.toString((Double) value);
            }
            if (type.equals(Float.class) || type.equals(float.class)) {
                return Float.toString((Float) value);
            }
            if (type.equals(File.class)) {
                return ((File) value).getPath();
            }
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        if (type.isEnum()) {
            return value.toString();
        }
        return UNHANDLED_TYPE;
    }

}
