package dev.jeka.core.api.java;

import dev.jeka.core.api.utils.JkUtilsReflect;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Not part of public api
 */
public interface JkInternalClasspathScanner {

    JkInternalClasspathScanner INSTANCE = of();

    Set<Class<?>> loadClassesHavingSimpleNameMatching(Predicate<String> predicate);

    List<String> findClassesHavingMainMethod(ClassLoader extraCclassLoader);

    <T> Class<T> loadClassesHavingNameOrSimpleName(String name, Class<T> superClass);

    static JkInternalClasspathScanner of() {
        String IMPL_CLASS = "dev.jeka.core.api.java.embedded.classgraph.ClassGraphClasspathScanner";
        Class<JkInternalClasspathScanner> clazz = JkClassLoader.ofCurrent().loadIfExist(IMPL_CLASS);
        if (clazz != null) {
            return JkUtilsReflect.invokeStaticMethod(clazz, "of");
        }
        return JkInternalClassloader.ofEmbeddedLibs().createCrossClassloaderProxy(JkInternalClasspathScanner.class, IMPL_CLASS, "of");
    }

    default Set<Class<?>> loadClassesHavingSimpleName(String simpleName) {
        return loadClassesHavingSimpleNameMatching( name -> name.equals(simpleName));
    }


    List<String> findClassesMatchingAnnotations(ClassLoader classloader,
                                                Predicate<List<String>> annotationPredicate);
}
