package dev.jeka.core.api.java.embedded.classgraph;

import dev.jeka.core.api.java.JkInternalClasspathScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.util.HashSet;
import java.util.Set;

class ClassGraphClasspathScanner implements JkInternalClasspathScanner {

    static ClassGraphClasspathScanner of() {
        return new ClassGraphClasspathScanner();
    }

    @Override
    public Set<Class<?>> loadClassesMatching(ClassLoader classLoader, String... globPatterns) {
        ClassGraph classGraph = new ClassGraph()
                .enableClassInfo()
                .overrideClassLoaders(classLoader);
        ScanResult scanResult = classGraph.scan();
        for (ClassInfo classInfo : scanResult.getAllClasses()) {
            System.out.println(classGraph);
        }
        return new HashSet<>(scanResult.getAllClasses().loadClasses());
    }

}