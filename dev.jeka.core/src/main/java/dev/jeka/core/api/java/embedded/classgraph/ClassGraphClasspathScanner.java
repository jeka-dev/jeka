package dev.jeka.core.api.java.embedded.classgraph;

import dev.jeka.core.api.java.JkInternalClasspathScanner;
import io.github.classgraph.ClassGraph;

import java.util.HashSet;
import java.util.Set;

class ClassGraphClasspathScanner implements JkInternalClasspathScanner {

    static ClassGraphClasspathScanner of() {
        return new ClassGraphClasspathScanner();
    }

    @Override
    public Set<Class<?>> loadClassesMatching(ClassLoader classLoader, String... globPatterns) {
        ClassGraph classGraph = new ClassGraph()
                .overrideClassLoaders(classLoader)
                .whitelistPaths(globPatterns);
        return new HashSet<>(classGraph.scan().getAllClasses().loadClasses());
    }

}