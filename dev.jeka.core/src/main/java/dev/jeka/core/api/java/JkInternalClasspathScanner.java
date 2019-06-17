package dev.jeka.core.api.java;

import dev.jeka.core.api.utils.JkUtilsReflect;

import java.util.Set;

/**
 * Not part of public api
 */
public interface JkInternalClasspathScanner {

    static final JkInternalClasspathScanner INSTANCE = of();

    Set<Class<?>> loadClassesMatching(ClassLoader classLoader, String ... globPatterns);

    static JkInternalClasspathScanner of() {
        String IMPL_CLASS = "dev.jeka.core.api.java.embedded.classgraph.ClassGraphClasspathScanner";
        Class<JkInternalClasspathScanner> clazz = JkClassLoader.ofCurrent().loadIfExist(IMPL_CLASS);
        if (clazz != null) {
            return JkUtilsReflect.invokeStaticMethod(clazz, "of");
        }
        return JkInternalEmbeddedClassloader.createCrossClassloaderProxy(JkInternalClasspathScanner.class, IMPL_CLASS, "of");
    }


}
