package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

final class FieldInjector {

    private static final String UNHANDLED_TYPE = "";

    static Set<String> inject(Object target, Map<String, String> props) {
        return inject(target, props, "");
    }

    private static Set<String> inject(Object target, Map<String, String> props, String fieldPrefix) {
        Set<String> usedProperties = new HashSet<>();
        for (final Field field : getOptionFields(target.getClass())) {
            Set<String> matchedKeys = inject(target, field, props, fieldPrefix);
            usedProperties.addAll(matchedKeys);
        }
        return usedProperties;
    }

    static void injectEnv(Object target) {
        for (final Field field : getOptionFields(target.getClass())) {
            final JkEnv env = field.getAnnotation(JkEnv.class);
            if (env != null) {
                final String stringValue = System.getenv(env.value());
                if (stringValue != null) {
                    final Class<?> type = field.getType();
                    Object value;
                    try {
                        value = parse(type, stringValue);
                    } catch (final IllegalArgumentException e) {
                        throw new JkException("Option " + env.value() + " has been set with improper value '"
                                + stringValue + "'");
                    }
                    JkUtilsReflect.setFieldValue(target, field, value);
                }
            }
        }
    }

    static List<Field> getOptionFields(Class<?> clazz) {
        return Arrays.asList(clazz.getFields()).stream().filter(field -> !Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toList());
    }

    private static Set<String> inject(Object target, Field field, Map<String, String> props, String prefix) {
        final String name = field.getName();
        final Class<?> type = field.getType();
        final boolean present = props.containsKey(name);
        if (present) {
            final String stringValue = props.get(name);
            Object value;
            try {
                value = parse(type, stringValue);
            } catch (final IllegalArgumentException e) {
                throw new JkException("Option " + name + " has been set with improper value '" + stringValue + "'");
            }
            if (value == UNHANDLED_TYPE) {
                throw new IllegalArgumentException("Class " + target.getClass().getName()
                        + ", field " + name + ", can't handle type " + type);
            }
            if (Modifier.isFinal(field.getModifiers())) {
                throw new JkException("Can not set value on final " + field.getDeclaringClass().getName()
                        + "#" + field.getName() + " field.");
            }
            JkUtilsReflect.setFieldValue(target, field, value);
            return Collections.singleton(prefix + name);
        } else if (hasKeyStartingWith(name + ".", props)) {
            String fieldPrefix = name + ".";
            Object value = JkUtilsReflect.getFieldValue(target, field);
            if (value == null) {
                value = JkUtilsReflect.newInstance(field.getType());
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new JkException("Can not set value on final " + field.getDeclaringClass().getName()
                            + "#" + field.getName() + " field.");
                }
                JkUtilsReflect.setFieldValue(target, field, value);
            }
            final Map<String, String> subProps = extractKeyStartingWith(fieldPrefix, props);
            return inject(value, subProps, prefix + fieldPrefix);
        } else {
            return Collections.emptySet();
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return true;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static Object parse(Class<?> type, String stringValue)
            throws IllegalArgumentException {
        if (stringValue == null) {
            return defaultValue(type);
        }
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

        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        if (type.isEnum()) {
            @SuppressWarnings("rawtypes")
            final Class enumType = type;
            return Enum.valueOf(enumType, stringValue);
        }
        if (type.equals(File.class)) {
            return new File(stringValue);
        }
        if (type.equals(Path.class)) {
            return Paths.get(stringValue);
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

}
