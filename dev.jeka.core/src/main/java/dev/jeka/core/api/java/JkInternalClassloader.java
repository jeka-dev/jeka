package dev.jeka.core.api.java;

import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Not part of the public API
 */
public class JkInternalClassloader {

    private final ClassLoader classLoader;

    private JkInternalClassloader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static JkInternalClassloader of(ClassLoader classLoader) {
        return new JkInternalClassloader(classLoader);
    }

    public static JkInternalClassloader ofEmbeddedLibs() {
        JkUtilsSystem.disableUnsafeWarning();  // Avoiding unsafe warning due to Ivy.
        URL embeddedNameUrl = JkClassLoader.ofCurrent().get().getResource("META-INF/jeka-embedded-name");
        String jarName = JkUtilsIO.read(embeddedNameUrl);
        URL url = JkClassLoader.ofCurrent().get().getResource("META-INF/" + jarName);
        Path file = JkUtilsIO.copyUrlContentToCacheFile(url, null, JkUrlClassLoader.URL_CACHE_DIR);
        ClassLoader classLoader = new URLClassLoader(new URL[] {JkUtilsPath.toUrl(file)}, JkClassLoader.ofCurrent().get());
        return of(classLoader);
    }

    public JkClassLoader get() {
        return JkClassLoader.of(classLoader);
    }

    /**
     * Creates an instance of the specified class in this classloader and
     * callable from the current thread classloader. Arguments ans result are
     * serialized (if needed) so we keep compatibility between classes.
     */
    @SuppressWarnings("unchecked")
    public <T> T createCrossClassloaderProxy(Class<T> interfaze, String className,
                                             String staticMethodFactory, Object... args) {
        final Object target = invokeStaticMethod(className, staticMethodFactory, args);
        ClassLoader from = Thread.currentThread().getContextClassLoader();
        return ((T) Proxy.newProxyInstance(from,
                new Class[]{interfaze}, new CrossClassloaderInvokationHandler(target, from)));
    }

    private class CrossClassloaderInvokationHandler implements InvocationHandler {

        CrossClassloaderInvokationHandler(Object target, ClassLoader fromClassLoader) {
            this.targetObject = target;
            this.fromClassLoader = fromClassLoader;
        }

        private final Object targetObject;

        private final ClassLoader fromClassLoader;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            final Method targetMethod = JkUtilsReflect.methodWithSameNameAndArgType(method,
                    targetObject.getClass());
            return invokeInstanceMethod(fromClassLoader, targetObject, targetMethod, args);
        }

    }

    @SuppressWarnings("unchecked")
    private <T> T invokeInstanceMethod(ClassLoader from, Object object, Method method,
                                      Object... args) {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            final Object returned = JkUtilsReflect.invoke(object, method, args);
            return (T) returned;
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeStaticMethod(String className, String methodName,
                                    Object... args) {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            final Class<?> clazz = get().load(className);
            return (T) JkUtilsReflect.invokeStaticMethod(clazz, methodName, args);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

}
