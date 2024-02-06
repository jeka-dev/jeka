package dev.jeka.core.api.java;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Not part of public api
 */
public interface JkInternalClasspathScanner {

    static JkInternalClasspathScanner of() {
        return Cache.get(JkProperties.ofSysPropsThenEnvThenGlobalProperties());
    }

    List<String> findClassesWithMainMethod(ClassLoader extraClassLoader);

    List<String> findClassesMatchingAnnotations(ClassLoader classloader,
                                                Predicate<List<String>> annotationPredicate);

    List<String> findClassesExtending(ClassLoader classLoader, Class<?> baseClass, boolean ignoreVisibility);

    List<String> findClassesInheritingOrAnnotatesWith(ClassLoader classLoader,
                                                      Class<?> baseClass,
                                                      Predicate<String> scanElementFilter,
                                                      Predicate<Path> returnElementFilter,
                                                      boolean ignoreVisibility,
                                                      boolean ignoreParentClassloaders,
                                                      Class<?> ... annotations);

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

            // Another version of classgraph may be present on the classpath
            // Some libraries as org.webjars:webjars-locator-core use it.
            // To for this library version mw need to create a dedicated classloader
            // with child first strategy.

            JkCoordinateFileProxy classgraphJar = JkCoordinateFileProxy.ofStandardRepos(properties,
                    "io.github.classgraph:classgraph:4.8.162");
            URL[] urls = JkPathSequence.of(classgraphJar.get())
                    .and(JkInternalEmbeddedClassloader.embeddedLibs())
                    .toUrls();
            ClassLoader parentClassloader = JkInternalClasspathScanner.class.getClassLoader();
            ChildFirstClassLoader childFirstClassLoader = new ChildFirstClassLoader(urls, parentClassloader);
            Class clazz = JkClassLoader.of(childFirstClassLoader).load(IMPL_CLASS);
            CACHED_INSTANCE = JkUtilsReflect.invokeStaticMethod(clazz, "of");

            /*
            Class<JkInternalClasspathScanner> clazz = JkClassLoader.ofCurrent().loadIfExist(IMPL_CLASS);
            if (clazz != null) {
                return JkUtilsReflect.invokeStaticMethod(clazz, "of");
            }

            JkInternalEmbeddedClassloader internalClassloader = JkInternalEmbeddedClassloader
                    .ofMainEmbeddedLibs(classgraphJar.get());


            CACHED_INSTANCE = internalClassloader
                    .createCrossClassloaderProxy(JkInternalClasspathScanner.class, IMPL_CLASS, "of");



            JkUtilsAssert.argument(internalClassloader.get().isDefined(IMPL_CLASS), "Class %s not found in %s",
                IMPL_CLASS,  "embedded lib");
                 */


            return CACHED_INSTANCE;
        }

    }

}
