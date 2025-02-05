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

    private final List<KBean> preInitializedKbeans = new LinkedList<>();

    static PreInitializer of(List<Class<? extends KBean>> kbeanClasses) {
        Map<Class<?>, JkConsumers<? extends KBean>> map = new LinkedHashMap<>();
        for (Class<?> kbeanClass : kbeanClasses) {
            map.putAll(findMethods(kbeanClass));
        }
        return new PreInitializer(map);
    }

    // TODO remove
    JkConsumers get(Class<? extends KBean> kbeanClass) {
        JkConsumers<? extends KBean> result = map.getOrDefault(kbeanClass, JkConsumers.of());
        JkLog.debug("Pre-initialization of %s found %s.", kbeanClass.getName(), result);
        return result;
    }

    void accept(KBean kbean) {
        JkConsumers<KBean> consumers = (JkConsumers<KBean>) map.getOrDefault(kbean.getClass(), JkConsumers.of());
        JkLog.debug("Pre-initialization of %s found %s.", kbean.getClass().getName(), consumers);
        if (!consumers.isEmpty()) {
            preInitializedKbeans.add(kbean);
        }
        consumers.accept(kbean);
    }

    List<KBean> getPreInitializedKbeans() {
        return Collections.unmodifiableList(preInitializedKbeans);
    }

    List<String> getInitializerNamesFor(Class<? extends KBean> kbeanClass) {
        return map.getOrDefault(kbeanClass, JkConsumers.of()).getConsumerNames();
    }

    static void assertMethodDeclarationValid(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            String msg = String.format("@JkPreInit method '%s' should be static. " +
                    "- **Please declare this method as static to resolve the issue**\n", method);
            throw new IllegalStateException(msg);
        }
        Class[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            String msg = String.format("@JkPreInit method '%s' must have exactly one parameter, which should be a subclass of KBean. " +
                    "- **Please update the method declaration to fix this issue**\n", method);
            throw new IllegalStateException(msg);
        }
    }

    private static Map<Class<?>, JkConsumers<? extends KBean>> findMethods(Class<?> kbeanClass) {
        JkLog.debug("Finding Pre-initialisation methods in class %s ", kbeanClass.getName());
        List<Method> methods = JkUtilsReflect.getDeclaredMethodsWithAnnotation(kbeanClass, JkPreInit.class);
        Map<Class<?>, JkConsumers<? extends KBean>> result = new HashMap<>();
        for (Method method : methods) {
            assertMethodDeclarationValid(method);
            Class[] paramTypes = method.getParameterTypes();
            Class paramType = paramTypes[0];

            Consumer kbeanConsumer = kbean -> {
                method.setAccessible(true);
                JkUtilsReflect.invoke(null, method, kbean);
            };
            result.putIfAbsent(paramType, JkConsumers.of());
            JkConsumers<?> consumers = result.get(paramType);
            JkLog.debug("Adding pre-initialization method %s for KBean %s ", method, paramType.getName());
            consumers.append(methodName(method), kbeanConsumer);
        }
        return result;
    }

    private static String methodName(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

}
