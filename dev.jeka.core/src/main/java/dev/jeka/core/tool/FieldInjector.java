package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        for (final Field field : getPropertyFields(target.getClass())) {
            Set<String> matchedKeys = inject(target, field, props, fieldPrefix);
            usedProperties.addAll(matchedKeys);
        }
        return usedProperties;
    }

    static void injectAnnotatedProperties(Object target, JkProperties properties) {
        for (final Field field : getPropertyFields(target.getClass())) {
            final JkInjectProperty injectProperty = field.getAnnotation(JkInjectProperty.class);
            if (injectProperty != null) {
                String propertyName = injectProperty.value();
                if (properties.get(propertyName) == null) {
                    JkLog.info("No property '%s' defined for injecting in field %s", propertyName, field);
                }
                String propertyValue = properties.get(propertyName);
                final Class<?> type = field.getType();
                Object value;
                try {
                    value = parse(type, propertyValue);
                } catch (final IllegalArgumentException e) {
                    throw new JkException("Property " + injectProperty.value() + " has been set with improper value '"
                            + propertyValue + "' : " + e.getMessage());
                }
                JkUtilsReflect.setFieldValue(target, field, value);
            } else {
                Object fieldValue = JkUtilsReflect.getFieldValue(target, field);
                if (fieldValue != null) {
                    injectAnnotatedProperties(fieldValue, properties);
                }
            }
        }
    }

    static List<Field> getPropertyFields(Class<?> clazz) {
        return JkUtilsReflect.getAllDeclaredFields(clazz,true).stream()
                .filter(FieldInjector::isPropertyField)
                .collect(Collectors.toList());
    }

    private static boolean isPropertyField(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        if (Modifier.isPublic(field.getModifiers())) {
            return true;
        }
        String setterName = "set" + JkUtilsString.capitalize(field.getName());
        return Arrays.asList(field.getDeclaringClass().getMethods()).stream()
                .map(Method::getName)
                .anyMatch(name -> name.equals(setterName));
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
                throw new JkException("Option " + name + " has been set with improper value '" + stringValue + "' : "
                        + e.getMessage());
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
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new JkException("Can not set value on final " + field.getDeclaringClass().getName()
                            + "#" + field.getName() + " field. \n"
                            + "Field is : " + field + "\n"
                            + "Field Value is : " + value +  "\n"
                            + "Props is : " + props + "\n"
                            + "Prefix is : " + prefix + "\n"
                    );
                }
                value = JkUtilsReflect.newInstance(field.getType());
                JkUtilsReflect.setFieldValue(target, field, value);
            }
            final Map<String, String> subProps = extractKeyStartingWith(fieldPrefix, props);
            return inject(value, subProps, prefix + fieldPrefix);
        } else {
            return Collections.emptySet();
        }
    }

    @SuppressWarnings("unchecked")
    static Object parse(Class<?> type, String stringValue)
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
