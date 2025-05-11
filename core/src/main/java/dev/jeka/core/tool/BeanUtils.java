/*
 * Copyright 2014-2025  the original author or authors.
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

import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;


class BeanUtils {

    static void setValue(Object bean, String propName, Object value) {

        if (propName.contains(".")) {
            String first = JkUtilsString.substringBeforeFirst(propName, ".");
            String remaining = JkUtilsString.substringAfterFirst(propName, ".");
            Object child = JkUtilsReflect.getFieldValue(bean, first);
            if (child == null) {
                String msg = java.lang.String.format("Compound property '%s' on class '%s' should not value 'null' right after " +
                        "been instantiate.%n. Please instantiate this property in %s constructor",
                        first, bean.getClass().getName(), bean.getClass().getSimpleName());
                throw new JkException(msg);
            } else {
                if (child instanceof JkMultiValue) {
                    JkMultiValue<?> multiValue = (JkMultiValue<?>) child;
                    String key = remaining;
                    if (key.contains(".")) {
                        key = JkUtilsString.substringBeforeFirst(remaining, ".");
                    }
                    String keyRemaining = JkUtilsString.substringAfterFirst(propName,
                            first + "." + key + ".");
                    if (keyRemaining.isEmpty()) {
                        multiValue.putAny(key, value);
                    } else {
                        Object mValue = multiValue.get(key);
                        if (mValue == null) {
                            mValue = JkUtilsReflect.newInstance(multiValue.getType());
                            multiValue.putAny(key, mValue);
                        }

                        setValue(mValue, keyRemaining, value);
                    }
                } else {
                    setValue(child, remaining, value);
                }

            }
        } else {
            Field field = JkUtilsReflect.getField(bean.getClass(), propName);
            JkUtilsAssert.state(field != null, "Null field found for class %s and field %s",
                    new Object[]{bean.getClass().getName(), propName});
            JkUtilsReflect.setFieldValue(bean, field, value);
        }
    }

    /**
     * Removes the generic multtValue BeanField as addresses.[key].street, with the ones
     * passed as arguments.
     * @param cmdArgs contains fields passed as cmdline args as addresses.1.street=toto.
     */
    static List<JkBeanDescription.BeanField> enhanceWithMultiValues(List<JkBeanDescription.BeanField> beanFields,
                                                                    List<String> cmdArgs) {
        List<String> propNames = cmdArgs.stream().map(BeanUtils::argToPropName).collect(Collectors.toList());
        return beanFields.stream()
                .flatMap(beanField -> convert(beanField, propNames).stream())
                .collect(Collectors.toList());

    }

    static List<String> extractKBeanPropertyNamesFromProps(Class<? extends KBean> kbeanClass, JkProperties props) {
        Map<String, String> allProps = props.getAllStartingWith("@", false);
        List<String> beanProps = new ArrayList<>();
        allProps.forEach((key, value) -> {
            String beanName = JkUtilsString.substringBeforeFirst(key, ".");
            if (KBean.nameMatches(kbeanClass.getName(), beanName)) {
                String propName = JkUtilsString.substringAfterFirst(key, ".");
                beanProps.add(propName);
            }
        });
        return beanProps;
    }

    private static String argToPropName(String cmdArg) {
        String propName = cmdArg;
        if (cmdArg.contains("=")) {
            propName = JkUtilsString.substringBeforeFirst(cmdArg, "=");
        }
        return propName;
    }

    private static List<JkBeanDescription.BeanField> convert(JkBeanDescription.BeanField beanField, List<String> propNames) {
        if (!beanField.containsMultiValue()) {
            return Collections.singletonList(beanField);
        }
        List<JkBeanDescription.BeanField> result = new LinkedList<>();
        for (String propName : propNames) {
            if (beanField.matchName(propName)) {
                result.add(beanField.withName(propName));
            }
        }
        return result;
    }

    static Class<?> getValueType(Field multiValueField) {
        Type genericType = multiValueField.getGenericType();

        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] typeArguments = pt.getActualTypeArguments();
            if (typeArguments.length == 0) {
                throw new IllegalStateException("JkMultiValue field " + multiValueField + " must declare a generic type");
            }
            Type type = typeArguments[0];
            if (type instanceof Class<?>) {
                return (Class<?>) type;
            }
            if (type instanceof ParameterizedType) {
                Type rawType = ((ParameterizedType) type).getRawType();
                if (rawType instanceof Class<?>) {
                    return (Class<?>) rawType;
                }
            }
            throw new IllegalStateException("Cannot find class of type " + type);
        } else {
            throw new IllegalStateException("No parametrized type found in " + multiValueField);
        }
    }
}
