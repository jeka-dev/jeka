/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for dealing with generic Object class instances.
 *
 * @author Jerome Angiabud
 */
public final class JkUtilsObject {

    /**
     * Returns the objects of the specified array that is not <code>null</code>.
     * Throw an {@link IllegalArgumentException} if all array elements are
     * <code>null</code> or the specified array is empty.
     */
    @SuppressWarnings("unchecked")
    public static <T> T firstNonNull(T... items) {
        for (final T item : items) {
            if (item != null) {
                return item;
            }
        }
        throw new IllegalArgumentException("Both objects can't be null.");
    }

    /**
     * Returns <code>true</true> if both object are <code>null</code> or the two
     * objects are equals. Returns <code>false</code> otherwise.
     */
    public static boolean equals(Object object1, Object object2) {
        if (object1 == null) {
            return object2 == null;
        }
        return object1.equals(object2);
    }

    /**
     * Returns the hash code of the specified object or 0 if it's
     * <code>null</code>.
     */
    public static int hashCode(Object object) {
        if (object == null) {
            return 0;
        }
        return object.hashCode();
    }

    /**
     * Null safe for {@link Object#toString()}. If the specified object is
     * <code>null</code> than this method returns "null".
     */
    public static String toString(Object object) {
        if (object == null) {
            return "null";
        }
        return object.toString();
    }

    public static <T extends Enum<T>> T valueOfEnum(Class<T> enumType, String name) {
        try {
            return Enum.valueOf(enumType, name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /**
     * Copies non null public instance fields for specified overrider instance into the specified source one.
     * The copy is recursive for fields which have themselves public instance fields.
     */
    public static <T> void copyNonNullPublicFieldsInto(T source, T overrider) {
        List<Field> fields = getInstancePublicField(source.getClass());
        for (Field field : fields) {
            Object overriderValue = JkUtilsReflect.getFieldValue(overrider, field);
            if (overriderValue != null) {
                Object sourceFieldValue = JkUtilsReflect.getFieldValue(source, field);
                if (sourceFieldValue == null) {
                    JkUtilsReflect.setFieldValue(source, field, overriderValue);
                    continue;
                }
                if (sourceFieldValue == null) {
                    continue;
                }
                // both field values are not null
                boolean isTerminal = getInstancePublicField(sourceFieldValue.getClass()).isEmpty();
                if (isTerminal) {
                    JkUtilsReflect.setFieldValue(source, field, overriderValue);
                } else {
                    copyNonNullPublicFieldsInto(sourceFieldValue, overriderValue);
                }
            }
        }
    }

    private static List<Field> getInstancePublicField(Class clazz) {
        return Arrays.stream(clazz.getFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toList());
    }

}
