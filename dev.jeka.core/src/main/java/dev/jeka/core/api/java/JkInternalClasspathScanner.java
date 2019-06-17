package dev.jeka.core.api.java;

import java.util.Set;
import java.util.function.Predicate;

public interface JkInternalClasspathScanner {

    Set<Class<?>> loadClassesMatching(ClassLoader classLoader, String ... globPatterns);


}
