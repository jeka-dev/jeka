package dev.jeka.core.api.java.embedded.classgraph;

import dev.jeka.core.api.java.JkInternalClasspathScanner;
import io.github.classgraph.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

class ClassGraphClasspathScanner implements JkInternalClasspathScanner {

    static ClassGraphClasspathScanner of() {
        return new ClassGraphClasspathScanner();
    }

    @Override
    public Set<Class<?>> loadClassesHavingSimpleNameMatching(Predicate<String> predicate) {
        return loadClassesMatching(hasSimpleName(predicate), false);
    }

    @Override
    public <T> Class<T> loadClassesHavingNameOrSimpleName(String name, Class<T> superClass) {
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
                .blacklistPackages("java", "org.apache.ivy", "org.bouncycastle", "nonapi.io.github.classgraph",
                        "org.commonmark", "io.github.classgraph");
        if (ignoreClassVisibility) {
            classGraph = classGraph.ignoreClassVisibility();
        }
        final ScanResult scanResult = classGraph.scan();
        final Set<Class<?>> result = new HashSet<>();
        for (final ClassInfo classInfo : scanResult.getAllClasses()) {
            if (predicate.test(classInfo)) {
                result.add(classInfo.loadClass());
            }
        }
        return result;
    }

    @Override
    public List<String> findClassesHavingMainMethod(ClassLoader classloader) {
        final ClassGraph classGraph = new ClassGraph()
                .enableClassInfo()
                .enableMethodInfo()
                .overrideClassLoaders(classloader)
                .ignoreParentClassLoaders();
        final ScanResult scanResult = classGraph.scan();
        final List<String> result = new LinkedList<>();
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
        return result;
    }

}
