package dev.jeka.core.api.java.embedded.classgraph;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import dev.jeka.core.api.java.JkInternalClasspathScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

class ClassGraphClasspathScanner implements JkInternalClasspathScanner {

    static ClassGraphClasspathScanner of() {
        return new ClassGraphClasspathScanner();
    }

    @Override
    public Set<Class<?>> loadClassesHavingSimpleNameMatching(Predicate<String> predicate) {
        final ClassGraph classGraph = new ClassGraph()
                .enableClassInfo()
                .blacklistPackages("java", "org.apache.ivy", "org.bouncycastle", "nonapi.io.github.classgraph",
                        "org.commonmark", "io.github.classgraph");
        final ScanResult scanResult = classGraph.scan();
        final Set<Class<?>> result = new HashSet<>();
        for (final ClassInfo classInfo : scanResult.getAllClasses()) {
            if (predicate.test(classInfo.getSimpleName())) {
                result.add(classInfo.loadClass());
            }
        }
        return result;
    }

}