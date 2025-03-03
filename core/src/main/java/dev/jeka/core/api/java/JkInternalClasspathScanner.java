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

package dev.jeka.core.api.java;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Not part of public api
 */
public interface JkInternalClasspathScanner {

    static JkInternalClasspathScanner of() {
        return Cache.get(JkProperties.ofStandardProperties());
    }

    List<String> findClassesWithMainMethod(ClassLoader extraClassLoader);

    List<String> findClassesMatchingAnnotations(
            ClassLoader classloader,
            Predicate<List<String>> annotationPredicate);

    List<String> findClassesExtending(
            ClassLoader classLoader,
            Class<?> baseClass,
            boolean ignoreParentClassloaders,
            boolean scanJars,
            boolean scanFolder);

    List<String> findClassesExtending(
            ClassLoader classLoader,
            Class<?> baseClass,
            Path classDir);

    List<String> findClassesInheritingOrAnnotatesWith(
            ClassLoader classLoader,
            Class<?> baseClass,
            Predicate<String> scanElementFilter,
            Predicate<Path> returnElementFilter,
            boolean ignoreVisibility,
            boolean ignoreParentClassloaders,
            Class<?> ... annotations);

    Set<Class<?>> loadClassesHavingSimpleNameMatching(Predicate<String> predicate);

    <T> Class<T> loadFirstFoundClassHavingNameOrSimpleName(String name, Class<T> superClass);

    default Set<Class<?>> loadClassesHavingSimpleName(String simpleName) {
        return loadClassesHavingSimpleNameMatching( name -> name.equals(simpleName));
    }

    JkPathSequence getClasspath(ClassLoader classLoader);

    class Cache {

        private static JkInternalClasspathScanner CACHED_INSTANCE;

        private static JkInternalClasspathScanner get(JkProperties properties) {
            if (CACHED_INSTANCE != null) {
                return CACHED_INSTANCE;
            }
            String IMPL_CLASS = "dev.jeka.core.api.java.embedded.classgraph.ClassGraphClasspathScanner";

            // Another version of classGraph may be present on the classpath
            // Some libraries as org.webjars:webjars-locator-core use it.
            // For this library version we need to create a dedicated classloader
            // with child-first strategy.
            JkCoordinateFileProxy classgraphJar = JkCoordinateFileProxy.ofStandardRepos(properties,
                    "io.github.classgraph:classgraph:4.8.162");
            ClassLoader parentClassloader = JkInternalClasspathScanner.class.getClassLoader();
            JkInternalChildFirstClassLoader childFirstClassLoader = JkInternalChildFirstClassLoader.of(classgraphJar.get(),
                    parentClassloader);
            Class clazz = JkClassLoader.of(childFirstClassLoader).load(IMPL_CLASS);
            CACHED_INSTANCE = JkUtilsReflect.invokeStaticMethod(clazz, "of");

            return CACHED_INSTANCE;
        }

    }

}
