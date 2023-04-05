package dev.jeka.core.api.java;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Not part of public api
 */
public interface JkInternalClasspathScanner {

    static JkInternalClasspathScanner of() {
        return Cache.get(JkProperties.ofSysPropsThenEnv());
    }

    List<String> findClassesHavingMainMethod(ClassLoader extraClassLoader);

    List<String> findClassesMatchingAnnotations(ClassLoader classloader, Predicate<List<String>> annotationPredicate);

    List<String> findClassedExtending(ClassLoader classLoader, Class<?> baseClass,
                                      Predicate<String> classpathElementFilter, boolean ignoreVisibility,
                                      boolean ignoreParentClassloaders);

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
            Class<JkInternalClasspathScanner> clazz = JkClassLoader.ofCurrent().loadIfExist(IMPL_CLASS);
            if (clazz != null) {
                return JkUtilsReflect.invokeStaticMethod(clazz, "of");
            }
            JkCoordinateFileProxy classgraphJar = JkCoordinateFileProxy.ofStandardRepos(properties, "io.github.classgraph:classgraph:4.8.41");
            JkInternalEmbeddedClassloader internalClassloader = JkInternalEmbeddedClassloader.ofMainEmbeddedLibs(classgraphJar.get());
            CACHED_INSTANCE = internalClassloader
                    .createCrossClassloaderProxy(JkInternalClasspathScanner.class, IMPL_CLASS, "of");
            JkUtilsAssert.argument(internalClassloader.get().isDefined(IMPL_CLASS), "Class %s not found in %s",
                IMPL_CLASS,  "embedded lib");
            return CACHED_INSTANCE;
        }

    }

}
