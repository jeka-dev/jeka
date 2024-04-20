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

package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/*
 * This describes the methods and fields exposed by a KBean,
 * which are obtained through reflection
 *
 * @author Jerome Angibaud
 */
final class KBeanDescription {

    private static final Map<Class<? extends KBean>, KBeanDescription> CACHE = new HashMap<>();

    final Class<? extends KBean> kbeanClass;

    final String synopsisHeader;

    final String synopsisDetail;

    final List<BeanMethod> beanMethods;

    final List<BeanField> beanFields;

    final boolean includeDefaultValues;

    private KBeanDescription(
            Class<? extends KBean> kbeanClass,
            String synopsisHeader,
            String synopsisDetail,
            List<BeanMethod> beanMethods,
            List<BeanField> beanFields,
            boolean includeDefaultValues) {

        super();
        this.kbeanClass = kbeanClass;
        this.synopsisHeader = synopsisHeader;
        this.synopsisDetail = synopsisDetail;
        this.beanMethods = Collections.unmodifiableList(beanMethods);
        this.beanFields = Collections.unmodifiableList(beanFields);
        this.includeDefaultValues = includeDefaultValues;
    }

    static KBeanDescription of(Class<? extends KBean> kbeanClass, boolean computeDefaultValue) {
        if (CACHE.containsKey(kbeanClass)) {
            return CACHE.get(kbeanClass);
        }

        final List<BeanMethod> methods = new LinkedList<>();
        for (final Method method : executableMethods(kbeanClass)) {
            methods.add(BeanMethod.of(method));
        }
        Collections.sort(methods);
        final List<BeanField> beanFields = new LinkedList<>();
        List<NameAndField> nameAndFields =  fields(kbeanClass, "", true, null);
        for (final NameAndField nameAndField : nameAndFields) {
            beanFields.add(BeanField.of(kbeanClass, nameAndField.field,
                    nameAndField.name, nameAndField.rootClass, computeDefaultValue));
        }
        Collections.sort(beanFields);

        // Grab header + description from content of @JkDoc
        final JkDoc jkDoc = kbeanClass.getAnnotation(JkDoc.class);
        final String header;
        final String detail;
        final String fullDesc = jkDoc == null ? "" : jkDoc.value();
        String[] lines = fullDesc.split("\n");
        if (lines.length == 0) {
            header = "";
            detail = "";
        } else {
            header = lines[0];
            if (lines.length > 1) {
                detail = JkUtilsString.substringAfterFirst(fullDesc, "\n");
            } else {
                detail = "";
            }
        }
        KBeanDescription result = new KBeanDescription(kbeanClass, header, detail, methods, beanFields,
                computeDefaultValue);
        if (computeDefaultValue) {
            CACHE.put(kbeanClass, result);
        }
        return result;
    }

     boolean isContainingField(String fieldName) {
        return this.beanFields.stream().anyMatch(beanField -> fieldName.equals(beanField.name));
    }

    private static List<Method> executableMethods(Class<?> clazz) {
        final List<Method> result = new LinkedList<>();
        for (final Method method : clazz.getMethods()) {
            final JkDoc jkDoc = method.getAnnotation(JkDoc.class);
            if (jkDoc != null && jkDoc.hide()) {
                continue;
            }
            final int modifier = method.getModifiers();
            if (method.getReturnType().equals(void.class) && method.getParameterTypes().length == 0
                    && !JkUtilsReflect.isMethodPublicIn(Object.class, method.getName())
                    && !Modifier.isAbstract(modifier) && !Modifier.isStatic(modifier)) {
                result.add(method);
            }

        }
        return result;
    }

    private static List<NameAndField> fields(Class<?> clazz, String prefix, boolean root, Class<?> rClass) {
        final List<NameAndField> result = new LinkedList<>();
        for (final Field field : getPropertyFields(clazz)) {
            final JkDoc jkDoc = field.getAnnotation(JkDoc.class);
            if (jkDoc != null && jkDoc.hide()) {
                continue;
            }
            final Class<?> rootClass = root ? field.getDeclaringClass() : rClass;

            if (isTerminal(field.getType())) {  // optimization to avoid costly discoveries
                result.add(new NameAndField(prefix + field.getName(), field, rootClass));
            } else {
                final List<NameAndField> subOpts = fields(field.getType(), prefix + field.getName() + ".", false,
                        rootClass);
                result.addAll(subOpts);
            }
        }
        return result.stream()
                .filter(nameAndField -> !Modifier.isFinal(nameAndField.field.getModifiers()))
                .collect(Collectors.toList());
    }

    // For nested props, JkDoc must be present on the class or one of its fields.
    private static boolean isTerminal(Class<?> fieldType) {
        return !fieldType.isAnnotationPresent(JkDoc.class) &&
                JkUtilsReflect.getDeclaredFieldsWithAnnotation(fieldType, JkDoc.class).isEmpty();
    }

    private static List<Field> getPropertyFields(Class<?> clazz) {
        return JkUtilsReflect.getDeclaredFieldsWithAnnotation(clazz,true).stream()
                .filter(KBean::isPropertyField)
                .collect(Collectors.toList());
    }


    /**
     * Definition of method in a given class that can be called by Jeka.
     *
     * @author Jerome Angibaud
     */
    static final class BeanMethod implements Comparable<BeanMethod> {

        final String name;

        final String description;

        private final Class<?> declaringClass;

        private BeanMethod(String name, String description, Class<?> declaringClass) {
            super();
            this.name = name;
            this.description = description;
            this.declaringClass = declaringClass;
        }

        static BeanMethod of(Method method) {
            final JkDoc jkDoc = JkUtilsReflect.getInheritedAnnotation(method, JkDoc.class);
            final String descr;
            if (jkDoc != null) {
                descr = String.join("\n", jkDoc.value());
            } else {
                descr = null;
            }
            return new BeanMethod(method.getName(), descr, method.getDeclaringClass());
        }

        @Override
        public int compareTo(BeanMethod other) {
            if (this.declaringClass.equals(other.declaringClass)) {
                return this.name.compareTo(other.name);
            }
            if (this.declaringClass.isAssignableFrom(other.declaringClass)) {
                return -1;
            }
            return 1;
        }

    }

    /**
     * Definition for Jeka class option. Jeka class options are fields belonging to a
     * Jeka class.
     *
     * @author Jerome Angibaud
     */
    static final class BeanField implements Comparable<BeanField> {

        final Field field;

        final String name;

        final String description;

        private final Object bean;

        final Object defaultValue;

        final Class<?> type;

        final String injectedPropertyName;

        private BeanField(
                Field field,
                String name,
                String description,
                Object bean,
                Object defaultValue,
                Class<?> type,
                String injectedPropertyName) {

            super();
            this.field = field;
            this.name = name;
            this.description = description;
            this.bean = bean;
            this.defaultValue = defaultValue;
            this.type = type;
            this.injectedPropertyName = injectedPropertyName;
        }

        static BeanField of(
                Class<? extends KBean> beanClass,
                Field field,
                String name,
                Class<?> rootDeclaringClass,
                boolean computeDefaultValue) {

            final JkDoc jkDoc = field.getAnnotation(JkDoc.class);
            final String descr;
            if (jkDoc != null) {
                descr = String.join("\n", jkDoc.value());
            } else {
                descr = null;
            }
            final Class<?> type = field.getType();
            Object instance = computeDefaultValue ? JkUtilsReflect.newInstance(beanClass) : null;
            Object defaultValue = computeDefaultValue ? value(instance, name) : null;
            final JkInjectProperty injectProperty = field.getAnnotation(JkInjectProperty.class);
            final String propertyName = injectProperty != null ? injectProperty.value() : null;
            return new BeanField(
                    field,
                    name,
                    descr,
                    instance,
                    defaultValue,
                    type,
                    propertyName);
        }

        private static Object value(Object runInstance, String optName) {
            if (!optName.contains(".")) {
                return JkUtilsReflect.getFieldValue(runInstance, optName);
            }
            final String first = JkUtilsString.substringBeforeFirst(optName, ".");
            Object firstObject = JkUtilsReflect.getFieldValue(runInstance, first);
            if (firstObject == null) {
                final Class<?> firstClass = JkUtilsReflect.getField(runInstance.getClass(), first).getType();
                firstObject = JkUtilsReflect.newInstance(firstClass);
            }
            final String last = JkUtilsString.substringAfterFirst(optName, ".");
            return value(firstObject, last);
        }

        @Override
        public int compareTo(BeanField other) {
            if (this.bean == null || other.bean == null) { // maybe null if we don't compute default values
                return 0;
            }
            if (this.bean.getClass().equals(other.bean.getClass())) {
                return this.name.compareTo(other.name);
            }
            if (this.bean.getClass().isAssignableFrom(other.bean.getClass())) {
                return -1;
            }
            return 1;
        }

    }

    private static class NameAndField {
        final String name;
        final Field field;
        final Class<?> rootClass; // for nested fields, we need the class declaring

        // the asScopedDependency object

        NameAndField(String name, Field field, Class<?> rootClass) {
            super();
            this.name = name;
            this.field = field;
            this.rootClass = rootClass;
        }

        @Override
        public String toString() {
            return name + ", to " + rootClass.getName();
        }

    }

}
