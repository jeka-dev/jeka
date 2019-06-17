package dev.jeka.core.api.java.embedded.classgraph;

import dev.jeka.core.api.java.JkInternalClasspathScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

class ClassGraphClasspathScanner implements JkInternalClasspathScanner {

    @Override
    public Set<Class<?>> loadClassesMatching(ClassLoader classLoader, String... globPatterns) {
        ClassGraph classGraph = new ClassGraph()
                .overrideClassLoaders(classLoader)
                .whitelistPaths(globPatterns);
        return new HashSet<Class<?>>(classGraph.scan().getAllClasses().loadClasses());
    }

}