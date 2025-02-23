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

package dev.jeka.core.api.java.embedded.classgraph;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.KBean;
import io.github.classgraph.*;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ClassGraphClasspathScanner implements JkInternalClasspathScanner {

    private static final String[] REJECTED_PACKAGES = new String[] {
            "java",
            "javax",
            "com.sun",
            "org.apache",
            "org.bouncycastle",
            "nonapi.io.github.classgraph",
            "org.springframework",
            "org.commonmark",
            "io.github.classgraph",
            "org.antlr",
            "org.objectiveweb.asm",
            "org.aspectj",
            "com.google.api.client",
            "com.google.common",
            "org.h2",
            "org.hibernate",
            "jakarta.activation",
            "jakarta.persistence",
            "org.glassfish.jaxb",
            "org.junit.platform",
            "org.junit.jupiter",
            "org.apache.logging",
            "io.micrometer",
            "org.mockito",
    };

    static ClassGraphClasspathScanner of() {
        return new ClassGraphClasspathScanner();
    }

    @Override
    public Set<Class<?>> loadClassesHavingSimpleNameMatching(Predicate<String> predicate) {
        return loadClassesMatching(hasSimpleName(predicate), false);
    }

    @Override
    public <T> Class<T> loadFirstFoundClassHavingNameOrSimpleName(String name, Class<T> superClass) {
        for (Class<?> clazz : loadClassesMatching(classInfo -> classInfo.getName().equals(name), true)) {
            if (superClass.isAssignableFrom(clazz)) {
                return (Class<T>) clazz;
            }
        }
        for (Class<?> clazz : loadClassesMatching(classInfo -> classInfo.getSimpleName().equals(name), true)) {
            if (superClass.isAssignableFrom(clazz)) {
                return (Class<T>) clazz;
            }
        }
        return null;
    }

    private static Predicate<ClassInfo> hasSimpleName(Predicate<String> namePredicate) {
        return classInfo ->  namePredicate.test(classInfo.getSimpleName());

    }

    private Set<Class<?>> loadClassesMatching(Predicate<ClassInfo> predicate, boolean ignoreClassVisibility) {
        ClassGraph classGraph = new ClassGraph()
                .ignoreClassVisibility()
                .enableClassInfo()
                .enableRealtimeLogging()
                .rejectPackages(REJECTED_PACKAGES);
        if (ignoreClassVisibility) {
            classGraph = classGraph.ignoreClassVisibility();
        }
        final Set<Class<?>> result;
        try (ScanResult scanResult = classGraph.scan()) {
            result = new HashSet<>();
            for (final ClassInfo classInfo : scanResult.getAllClasses()) {
                if (predicate.test(classInfo)) {
                    result.add(classInfo.loadClass());
                }
            }
        }
        return result;
    }

    @Override
    public List<String> findClassesWithMainMethod(ClassLoader extraClassLoader) {
        List<String> result = findClassesWithMainMethod(extraClassLoader, true);
        if (!result.isEmpty()) {
            return result;
        }
        return findClassesWithMainMethod(extraClassLoader, false);
    }

    @Override
    public List<String> findClassesMatchingAnnotations(ClassLoader classloader,
                                                       Predicate<List<String>> annotationPredicate) {
        final ClassGraph classGraph = new ClassGraph()
                .enableClassInfo()
                .overrideClassLoaders(classloader)
                .enableAnnotationInfo()
                .ignoreParentClassLoaders();
        final List<String> result;
        try (ScanResult scanResult = classGraph.scan()) {
            result = new LinkedList<>();
            for (final ClassInfo classInfo : scanResult.getAllClasses()) {
                AnnotationInfoList annotationInfoList = classInfo.getAnnotationInfo();
                List<String> annotationNames = annotationInfoList.getNames();
                if (annotationPredicate.test(annotationNames)) {
                    result.add(classInfo.getName());
                }
            }
        }
        return result;
    }

    @Override
    public List<String> findClassesExtending(
             ClassLoader classLoader,
             Class<?> baseClass,
             boolean ignoreParentClassloaders,
             boolean scanJar,
             boolean scanFolder) {

        ClassGraph classGraph = new ClassGraph()
                .disableNestedJarScanning()
                .disableRuntimeInvisibleAnnotations()
                .disableNestedJarScanning()
                .disableModuleScanning()
                .enableClassInfo()

                .rejectPackages(REJECTED_PACKAGES)

                .overrideClassLoaders(classLoader)
                .ignoreClassVisibility();
        if (ignoreParentClassloaders) {
            classGraph.ignoreParentClassLoaders();
        }
        if (!scanJar) {
            classGraph.disableJarScanning();
        }
        if (!scanFolder) {
            classGraph.disableDirScanning();
        }

        try (ScanResult scanResult = classGraph.scan(4)) {  // using 4 instead default don't change perf
            return scanResult.getAllClasses().stream()
                    .filter(classInfo -> !classInfo.isAbstract())
                    .filter(classInfo -> classInfo.extendsSuperclass(KBean.class.getName()))
                    .map(ClassInfo::getName)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<String> findClassesExtending(ClassLoader classLoader, Class<?> baseClass, Path classDir) {
        String pathElementPah = classDir.toAbsolutePath().normalize().toString()
                .replace('\\', '/'); // failed on Windows if no relacement
        ClassGraph classGraph = new ClassGraph()
                .disableNestedJarScanning()
                .disableRuntimeInvisibleAnnotations()
                .disableNestedJarScanning()
                .disableModuleScanning()
                .disableJarScanning()
                .enableClassInfo()

                //.verbose()

                .filterClasspathElements(path -> path.equals(pathElementPah))
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader)
                .ignoreClassVisibility();

        try (ScanResult scanResult = classGraph.scan()) {  // using 4 instead default don't change perf
            return scanResult.getAllClasses().stream()
                    .filter(classInfo -> !classInfo.isAbstract())
                    .filter(classInfo -> classInfo.extendsSuperclass(KBean.class.getName()))
                    .map(ClassInfo::getName)
                    .collect(Collectors.toList());
        }
    }

    public List<String> findClassesInheritingOrAnnotatesWith(ClassLoader classLoader,
                                                            Class<?> baseClass,
                                                            Predicate<String> scanElementFilter,
                                                            Predicate<Path> returnElementFilter,
                                                            boolean ignoreVisibility,
                                                            boolean ignoreParentClassLoaders,
                                                            Class<?> ... annotations) {
        ClassGraph classGraph = new ClassGraph()
                .enableClassInfo()
                .rejectPackages(REJECTED_PACKAGES)
                .disableNestedJarScanning()
                .disableModuleScanning()
                .filterClasspathElements(scanElementFilter::test);
        if (annotations.length > 0) {
            classGraph.enableAnnotationInfo();
        }
        if (ignoreParentClassLoaders) {
            classGraph
                .ignoreParentClassLoaders()
                .overrideClassLoaders(classLoader);
        }
        if (ignoreVisibility) {
            classGraph = classGraph.ignoreClassVisibility();
        }
        try (ScanResult scanResult = classGraph.scan()) {
            return scanResult.getAllClasses().stream()
                    .filter(classInfo -> !classInfo.isAbstract())
                    .filter(classInfo -> inheritOf(classInfo, baseClass.getName())
                            || hasAnnotation(classInfo, annotations))
                    .filter(classInfo -> returnElementFilter.test(classInfo.getClasspathElementFile().toPath()))
                    .map(ClassInfo::getName)
                    .collect(Collectors.toList());
        }
    }

    public JkPathSequence getClasspath(ClassLoader classLoader) {
        List<File> files;
        try (ScanResult scanResult = new ClassGraph().scan()) {
            files = scanResult.getClasspathFiles();
        }
        return JkPathSequence.of(JkUtilsPath.toPaths(files));
    }

    private static List<String> findClassesWithMainMethod(ClassLoader extraClassLoader, boolean onlyPublicClasses) {
        ClassGraph classGraph = new ClassGraph()
                .rejectPackages(REJECTED_PACKAGES)
                .enableClassInfo()
                .enableMethodInfo()
                .disableJarScanning()  // only scan non-jar entries
                .overrideClassLoaders(extraClassLoader)
                .ignoreParentClassLoaders();
        if (!onlyPublicClasses) {
            classGraph = classGraph.ignoreClassVisibility();
        }
        final List<String> result;
        try (ScanResult scanResult = classGraph.scan()) {
            result = new LinkedList<>();
            for (final ClassInfo classInfo : scanResult.getAllClasses()) {
                MethodInfoList methodInfoList = classInfo.getMethodInfo("main");
                for (MethodInfo methodInfo : methodInfoList) {
                    if (methodInfo.isPublic() && methodInfo.isStatic() && methodInfo.getParameterInfo().length == 1) {
                        MethodParameterInfo methodParameterInfo = methodInfo.getParameterInfo()[0];
                        if (methodParameterInfo.getTypeDescriptor() instanceof ArrayTypeSignature) {
                            ArrayTypeSignature arrayTypeSignature = (ArrayTypeSignature) methodParameterInfo.getTypeDescriptor();
                            if ("java.lang.String[]".equals(arrayTypeSignature.toString())) {
                                result.add(classInfo.getName());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static boolean inheritOf(ClassInfo classInfo, String parentClassName) {
        return classInfo.getSuperclasses().stream()
                .anyMatch(parentClassInfo -> parentClassInfo.getName().equals(parentClassName));
    }

    public static boolean hasAnnotation(ClassInfo classInfo, Class<?>[] annotations) {
        for (Class<?> annotation  : annotations) {
            if (classInfo.hasAnnotation(annotation.getName())) {
                return true;
            }
        }
        return false;
    }

}
