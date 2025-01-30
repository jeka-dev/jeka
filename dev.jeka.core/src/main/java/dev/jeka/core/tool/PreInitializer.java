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

import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

class PreInitializer {

    private final Map<Class<?>, JkConsumers<? extends KBean>> map;

    private PreInitializer(Map<Class<?>, JkConsumers<? extends KBean>> map) {
        this.map = map;
    }

    static PreInitializer of(List<Class<? extends KBean>> kbeanClasses) {
        Map<Class<?>, JkConsumers<? extends KBean>> map = new HashMap<>();
        List<Class<? extends KBean>> allPreInitializerClasses = findPreInitializerClasses(kbeanClasses);
        for (Class<?> kbeanClass : allPreInitializerClasses) {
            map.putAll(findMethods(kbeanClass));
        }
        return new PreInitializer(map);
    }

    JkConsumers get(Class<? extends KBean> kbeanClass) {
        JkConsumers<? extends KBean> result = map.getOrDefault(kbeanClass, JkConsumers.of());
        JkLog.debug("Pre-initialization of %s found %s.", kbeanClass.getName(), result);
        return result;
    }

    private static Map<Class<?>, JkConsumers<? extends KBean>> findMethods(Class<?> kbeanClass) {
        JkLog.debug("Finding Pre-initialisation methods in class %s ", kbeanClass.getName());
        List<Method> methods = JkUtilsReflect.getDeclaredMethodsWithAnnotation(kbeanClass, JkPreInit.class);
        Map<Class<?>, JkConsumers<? extends KBean>> result = new HashMap<>();
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) {
                String msg = String.format("@JkDefaultProvider method '%s' should be static. " +
                        "- **Please declare this method as static to resolve the issue**\n", method);
                throw new IllegalStateException(msg);
            }
            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                String msg = String.format("@JkDefaultProvider method '%s' must have exactly one parameter, which should be a subclass of KBean. " +
                        "- **Please update the method declaration to fix this issue**\n", method);
                throw new IllegalStateException(msg);
            }
            Class paramType = paramTypes[0];

            Consumer kbeanConsumer = kbean -> {
                method.setAccessible(true);
                JkUtilsReflect.invoke(null, method, kbean);
            };
            result.putIfAbsent(paramType, JkConsumers.of());
            JkConsumers<?> consumers = result.get(paramType);
            JkLog.debug("Adding Pre-initialization method %s for KBean %s ", method, paramType.getName());
            consumers.append(method.toString(), kbeanConsumer);
        }
        return result;
    }

    private static List<Class<? extends KBean>> findPreInitializerClasses(
            List<Class<? extends KBean>> initializerClasses) {

        List<Class<? extends KBean>> result = new LinkedList<>(initializerClasses);
        for (Class<? extends KBean> preInitializerClass : initializerClasses) {
            JkPreInitKBeans preInitKBeans = preInitializerClass.getAnnotation(JkPreInitKBeans.class);
            if (preInitKBeans != null) {
                Arrays.stream(preInitKBeans.value()).forEach(extraInitializerClass -> {
                    if (!result.contains(extraInitializerClass)) {
                        JkLog.debug("Add pre-initializer class %s ", extraInitializerClass.getName());
                        result.add(extraInitializerClass);
                    }
                });
            }

        }
        if (result.size() != initializerClasses.size()) {
            return findPreInitializerClasses(result);  // find the classes recursively
        }
        return result;
    }

}
