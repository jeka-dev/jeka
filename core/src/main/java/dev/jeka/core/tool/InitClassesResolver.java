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

import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Find the classes to initialize, based on the initial ones + ones discovered with @Required annotation
 */
class InitClassesResolver {

    private final List<Class<? extends KBean>> classesToInitialize;

    private InitClassesResolver(List<Class<? extends KBean>> classesToInitialize) {
        this.classesToInitialize = classesToInitialize;
    }

    // Create an iniClassesResolver discovering
    static InitClassesResolver of(JkRunbase runbase, List<Class<? extends KBean>> initialClasses) {

        List<Class<? extends KBean>> result = new LinkedList<>(initialClasses);
        List<Class<? extends KBean>> workingList;
        boolean newFound = true;
        while (newFound) {
            workingList = new LinkedList<>(result);
            newFound = false;
            int index = 0;
            for (Class<? extends KBean> inspectKBeanClass : workingList) {
                List<Class<? extends KBean>> requiredKbeanClasses =
                        RequiringClass.of(inspectKBeanClass, runbase).getRequiredKbeanClasses();
                for (Class<? extends KBean> requiredKBeanClass : requiredKbeanClasses) {
                    if (!workingList.contains(requiredKBeanClass) && !result.contains(requiredKBeanClass)) {
                        result.add(index, requiredKBeanClass); // insert before
                        newFound = true;
                    }
                }
                index++;
            }
        }
        List<Class<? extends KBean>> sorted = sort(result, runbase);
        return new InitClassesResolver(sorted);
    }

    List<Class<? extends KBean>> getClassesToInitialize() {
        return Collections.unmodifiableList(classesToInitialize);
    }

    private static List<Class<? extends KBean>> sort(List<Class<? extends KBean>> kBeanClass, JkRunbase runbase) {
        List<RequiringClass> result = new LinkedList<>();
        for (Class<? extends KBean> kbenClass : kBeanClass) {
            RequiringClass requiringClass = RequiringClass.of(kbenClass, runbase);
            if (result.isEmpty()) {
                result.add(requiringClass);
            } else {
                int index = 0;
                for (RequiringClass presentClass : result) {
                    if (requiringClass.compareTo(presentClass) < 0) {
                        break;
                    }
                    index++;
                }
                result.add(index, requiringClass);
            }
        }
        return result.stream()
                .map(requiringClass -> requiringClass.kBeanClass)
                .collect(Collectors.toList());
    }


    /*
     * KBean class wrapper providing graph operation about required and post init classes
     */
    private static class RequiringClass implements Comparable<RequiringClass> {

        private static final Map<KBeanClassRunbase, RequiringClass> MAP = new HashMap<>();

        private final Class<? extends KBean> kBeanClass;

        private final JkRunbase runbase; // needed to resolve dynamic requires

        private List<Class<? extends KBean>> requiredKbeanClassesCache;

        private List<Class<? extends KBean>> postInitClassesCache;

        private RequiringClass(Class<? extends KBean> kBeanClass, JkRunbase runbase) {
            this.kBeanClass = kBeanClass;
            this.runbase = runbase;
        }

        static RequiringClass of(Class<? extends KBean> kBeanClass, JkRunbase runbase) {
            KBeanClassRunbase kBeanClassRunbase = new KBeanClassRunbase(kBeanClass, runbase);
            return MAP.computeIfAbsent(kBeanClassRunbase,
                    (key) -> new RequiringClass(kBeanClass, runbase));
        }

        @Override
        public int compareTo(RequiringClass other) {
            if (this.isDependingOn(other)) {
                return 1;
            }
            if (other.isDependingOn(this)) {
                return -1;
            }
            return 0;
        }

        List<Class<? extends KBean>> getRequiredKbeanClasses() {
            if (requiredKbeanClassesCache == null) {
                LinkedHashSet<Class<? extends KBean>> result =
                        new LinkedHashSet<>(getDirectRequiredKbeanClasses(runbase));
                LinkedHashSet<Class<? extends KBean>> workinList = new LinkedHashSet<>(result);
                for (Class<? extends KBean> requiredClass : result) {
                    workinList.addAll(RequiringClass.of(requiredClass, runbase).getRequiredKbeanClasses());
                }
                requiredKbeanClassesCache = new ArrayList<>(workinList);
            }
            return requiredKbeanClassesCache;
        }

        List<Class<? extends KBean>> getPostInitClasses() {
            if (postInitClassesCache == null) {
                LinkedHashSet<Class<? extends KBean>> result =
                        new LinkedHashSet<>(getDirectPostInitializedKbeanClasses());
                LinkedHashSet<Class<? extends KBean>> workinList = new LinkedHashSet<>(result);
                for (Class<? extends KBean> postInitClass : result) {
                    workinList.addAll(RequiringClass.of(postInitClass, runbase).getDirectPostInitializedKbeanClasses());
                }
                postInitClassesCache = new ArrayList<>(workinList);
            }
            return postInitClassesCache;
        }

        boolean isDependingOn(RequiringClass other) {
            return this.getRequiredKbeanClasses().contains(other.kBeanClass) ||
                    this.getPostInitClasses().contains(other.kBeanClass);
        }

        private List<Class<? extends KBean>> getDirectRequiredKbeanClasses(JkRunbase runbase) {
            // Add classes from fields annotated with @JkInject
            List<Class<? extends KBean>> result = new LinkedList<>(Injects.getLocalInjectedClasses(kBeanClass));

            // Add Classes from postInit methods annotated with @JkPostInit(require=true)
            for (Method method : kBeanClass.getDeclaredMethods()) {
                JkPostInit postInit = method.getAnnotation(JkPostInit.class);
                if (postInit != null) {
                    if (postInit.required()) {
                        PostInitializer.assertPostInitMethodValid(method);
                        Class<? extends KBean> postInitializedType = (Class<? extends KBean>) method.getParameterTypes()[0];
                        if (!result.contains(postInitializedType)) {
                            result.add(postInitializedType);
                        }
                    }
                }
            }

            // Add classes returned by methods annotated with @JkRequire
            for (Method method : kBeanClass.getDeclaredMethods()) {
                JkRequire require = method.getAnnotation(JkRequire.class);

                // check static
                if (require != null) {
                    if (!Modifier.isStatic(method.getModifiers())) {
                        String msg = String.format("%s is not static. Method annotated with @%s should be static." +
                                        " Please fix this in the code.",
                                method,
                                JkRequire.class.getSimpleName());
                        throw new JkException(msg);
                    }

                    // check param
                    Class[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length != 1 || !paramTypes[0].equals(JkRunbase.class)) {
                        String msg = String.format("Method %s annotated with @%s should accept a unique parameter of type %s." +
                                        "%nPlease fix in the code.",
                                method, JkRequire.class.getSimpleName(),
                                JkRunbase.class.getSimpleName());
                        throw new JkException(msg);
                    }

                    // check return type
                    Class<?> returnType = method.getReturnType();
                    if (!Class.class.equals(returnType)) {
                        String msg = String.format("Method annotated with @%s should should return an object of type " +
                                        "`Class<? extends KBean>: was %s." +
                                        "%nPlease fix in the code: %s.",
                                JkRequire.class.getSimpleName(), returnType, method);
                        throw new JkException(msg);
                    }

                    Class invokeResult = JkUtilsReflect.invoke(null, method, runbase);
                    if (invokeResult != null) {
                        if (!KBean.class.isAssignableFrom(invokeResult)) {
                            String msg = String.format("Method %s annotated with @%s should should return an object of type `Class<? extends KBean>." +
                                            "%nPlease fix in the code.",
                                    method, JkRequire.class.getSimpleName());
                            throw new JkException(msg);
                        }
                    }
                    if (!result.contains(invokeResult)) {
                        result.add(invokeResult);
                    }
                }
            }
            return result;
        }

        private List<Class<? extends KBean>> getDirectPostInitializedKbeanClasses() {
            // Add classes from fields annotated with @JkInject
            List<Class<? extends KBean>> result = new LinkedList<>();

            // Add Classes from postInit methods annotated with @JkPostInit(require=true)
            for (Method method : kBeanClass.getDeclaredMethods()) {
                JkPostInit postInit = method.getAnnotation(JkPostInit.class);
                if (postInit != null) {
                    PostInitializer.assertPostInitMethodValid(method);
                    Class<? extends KBean> postInitializedType = (Class<? extends KBean>) method.getParameterTypes()[0];
                    if (!result.contains(postInitializedType)) {
                        result.add(postInitializedType);
                    }
                } else {
                    JkPreInit preInit = method.getAnnotation(JkPreInit.class);
                    if (preInit != null) {
                        PreInitializer.assertMethodDeclarationValid(method);
                        Class<? extends KBean> preInitializedType = (Class<? extends KBean>) method.getParameterTypes()[0];
                        if (!result.contains(preInitializedType)) {
                            result.add(preInitializedType);
                        }
                    }
                }
            }
            return result;
        }
    }

    private static class KBeanClassRunbase {

        final Class<? extends KBean> kBeanClass;

        final Path runbasePath;

        KBeanClassRunbase(Class<? extends KBean> kBeanClass, JkRunbase runbase) {
            this.kBeanClass = kBeanClass;
            this.runbasePath = runbase.getBaseDir();
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof KBeanClassRunbase)) return false;

            KBeanClassRunbase that = (KBeanClassRunbase) o;
            return kBeanClass.equals(that.kBeanClass) && runbasePath.equals(that.runbasePath);
        }

        @Override
        public int hashCode() {
            int result = kBeanClass.hashCode();
            result = 31 * result + runbasePath.hashCode();
            return result;
        }
    }

}
