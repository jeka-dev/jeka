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

class PostInitializer {

    //Key stands for the KBean class to postInit
    private final Map<Class<?>, JkConsumers> map = new LinkedHashMap<>();

    private final  Set<Class<?>> registeredInitializers = new HashSet<>();

    private final  Set<KBean> postInitializedKBeans = new HashSet<>();

    private PostInitializer() {
    }

    static PostInitializer of() {
        return new PostInitializer();
    }

    void addPostInitializerCandidate(KBean initilizerKBean) {
        if (registeredInitializers.contains(initilizerKBean.getClass())) {
            JkLog.info("Post-initializer %s is already registered.", initilizerKBean.getClass().getName());
            return;
        }
        registeredInitializers.add(initilizerKBean.getClass());
        Map<Class<?>, JkConsumers<? extends KBean>> initializers = findMethods(initilizerKBean);
        for (Map.Entry<Class<?>, JkConsumers<? extends KBean>> entry : initializers.entrySet()) {
            Class<?> beanToInitializeClass = entry.getKey();
            map.putIfAbsent(beanToInitializeClass, JkConsumers.of());
            JkConsumers consumers = map.get(beanToInitializeClass);
            consumers.append(entry.getValue());
        }
    }

    List<String> apply(KBean kbeanToPostInitialize) {
        if (postInitializedKBeans.contains(kbeanToPostInitialize)) {
            JkLog.info("Kbean %s has already been post-initialized.", kbeanToPostInitialize);
            return Collections.emptyList();
        }
        postInitializedKBeans.add(kbeanToPostInitialize);
        Class<? extends KBean> kbeanClass = kbeanToPostInitialize.getClass();
        if (map.containsKey(kbeanClass)) {
            JkConsumers consumers = map.get(kbeanClass);
            consumers.accept(kbeanToPostInitialize);
            return consumers.getConsumerNames();
        }
        return Collections.emptyList();
    }

    private static Map<Class<?>, JkConsumers<? extends KBean>> findMethods(KBean initializerKBean) {
        Class<?> kbeanClass = initializerKBean.getClass();
        JkLog.debug("Finding Post-initialisation methods in class %s.", kbeanClass.getName());
        List<Method> methods = JkUtilsReflect.getDeclaredMethodsWithAnnotation(kbeanClass, JkPostInit.class);
        Map<Class<?>, JkConsumers<? extends KBean>> result = new HashMap<>();
        for (Method method : methods) {
            assertPostInitMethodValid(method);
            Class[] paramTypes = method.getParameterTypes();
            Class paramType = paramTypes[0];
            Consumer kbeanConsumer = kbean -> {
                method.setAccessible(true);
                JkUtilsReflect.invoke(initializerKBean, method, kbean);
            };
            result.putIfAbsent(paramType, JkConsumers.of());
            JkConsumers<?> consumers = result.get(paramType);
            JkLog.debug("Adding post-initialization method %s for KBean %s ", method, paramType.getName());
            consumers.append(methodName(method), kbeanConsumer);
        }
        return result;
    }

    static void assertPostInitMethodValid(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            String msg = String.format("@JkPostInit method '%s' should not be static. " +
                    "- **Please declare this method as non-static to resolve the issue**\n", method);
            throw new IllegalStateException(msg);
        }
        Class[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            String msg = String.format("@JkPostInit method '%s' must have exactly one parameter, which should be a subclass of KBean. " +
                    "- **Please update the method declaration to fix this issue**\n", method);
            throw new IllegalStateException(msg);
        }
    }

    private static String methodName(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

}
