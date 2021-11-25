package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.tool.JkBean;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Not part of public api
 */
public interface JkInternalClasspathScanner {

    JkInternalClasspathScanner INSTANCE = of();

    static JkInternalClasspathScanner of() {
        String IMPL_CLASS = "dev.jeka.core.api.java.embedded.classgraph.ClassGraphClasspathScanner";
        Class<JkInternalClasspathScanner> clazz = JkClassLoader.ofCurrent().loadIfExist(IMPL_CLASS);
        if (clazz != null) {
            return JkUtilsReflect.invokeStaticMethod(clazz, "of");
        }
        return JkInternalClassloader.ofMainEmbeddedLibs().createCrossClassloaderProxy(JkInternalClasspathScanner.class, IMPL_CLASS, "of");
    }

    List<String> findClassesHavingMainMethod(ClassLoader extraClassLoader);

    List<String> findClassesMatchingAnnotations(ClassLoader classloader, Predicate<List<String>> annotationPredicate);

    List<String> findClassedExtending(ClassLoader classLoader, Class<?> baseClass,
                                      Predicate<String> classpathElementFilter, boolean ignoreVisibility);

    Set<Class<?>> loadClassesHavingSimpleNameMatching(Predicate<String> predicate);

    <T> Class<T> loadFirstFoundClassHavingNameOrSimpleName(String name, Class<T> superClass);

    default Set<Class<?>> loadClassesHavingSimpleName(String simpleName) {
        return loadClassesHavingSimpleNameMatching( name -> name.equals(simpleName));
    }

    JkPathSequence getClasspath(ClassLoader classLoader);

}
