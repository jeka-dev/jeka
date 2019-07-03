package dev.jeka.core.api.java.embedded.classgraph;

import dev.jeka.core.api.java.JkInternalClasspathScanner;
import dev.jeka.core.api.system.JkLog;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

class ClassGraphClasspathScanner implements JkInternalClasspathScanner {

    static ClassGraphClasspathScanner of() {
        return new ClassGraphClasspathScanner();
    }

    @Override
    public Set<Class<?>> loadClassesHavingSimpleNameMatching(Predicate<String> predicate) {
        ClassGraph classGraph = new ClassGraph()
                .enableClassInfo()
                .blacklistPackages("java", "org.apache.ivy", "org.bouncycastle", "nonapi.io.github.classgraph",
                        "org.commonmark", "io.github.classgraph");
        ScanResult scanResult = classGraph.scan();
        Set<Class<?>> result = new HashSet<>();
        for (ClassInfo classInfo : scanResult.getAllClasses()) {
            if (predicate.test(classInfo.getSimpleName())) {
                result.add(classInfo.loadClass());
            }
        }
        return result;
    }

}