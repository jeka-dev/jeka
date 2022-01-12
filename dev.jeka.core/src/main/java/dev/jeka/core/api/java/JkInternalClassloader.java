package dev.jeka.core.api.java;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Not part of the public API
 */
public class JkInternalClassloader {

    private final ClassLoader classLoader;

    private JkInternalClassloader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static final Path URL_CACHE_DIR = JkLocator.getCacheDir().resolve("url-content");

    static {
        JkUtilsPath.createDirectories(URL_CACHE_DIR);
    }

    public static JkInternalClassloader of(ClassLoader classLoader) {
        return new JkInternalClassloader(classLoader);
    }

    public static JkInternalClassloader ofMainEmbeddedLibs() {
        return ofMainEmbeddedLibs(Collections.emptyList());
    }

    public static JkInternalClassloader ofMainEmbeddedLibs(List<Path> extraEntries) {
        JkUtilsSystem.disableUnsafeWarning();  // Avoiding unsafe warning due to Ivy.
        List<Path> pathList = new LinkedList<>();
        URL embeddedNameUrl = JkClassLoader.ofCurrent().get().getResource("META-INF/jeka-embedded-name");
        if (embeddedNameUrl != null) {
            String jarName = JkUtilsIO.read(embeddedNameUrl);
            Path file = getEmbeddedLibAsPath("META-INF/" + jarName);
            pathList.add(file);
        }
        pathList.addAll(extraEntries);
        List<URL> urlList = pathList.stream()
                .map(JkUtilsPath::toUrl)
                .collect(Collectors.toList());
        URL[] urls = urlList.toArray(new URL[0]);
        ClassLoader classLoader = new URLClassLoader(urls, JkClassLoader.ofCurrent().get());
        return of(classLoader);
    }

    public static Path getEmbeddedLibAsPath(String resourcePath) {
        URL url = JkClassLoader.ofCurrent().get().getResource(resourcePath);
        final String name = resourcePath.contains("/") ? JkUtilsString.substringBeforeLast(resourcePath, "/")
                : resourcePath;
        Path result = URL_CACHE_DIR.resolve(name);
        if (Files.exists(result)) {
            return result;
        }
        return JkUtilsIO.copyUrlContentToCacheFile(url, null, URL_CACHE_DIR);
    }

    public JkClassLoader get() {
        return JkClassLoader.of(classLoader);
    }

    /**
     * Creates an instance of the specified class in this classloader and
     * callable from the current thread classloader.
     */
    @SuppressWarnings("unchecked")
    public <T> T createCrossClassloaderProxy(Class<T> interfaze, String className,
                                             String staticMethodFactory, Object... args) {
        final Object target = invokeStaticMethod(className, staticMethodFactory, args);
        ClassLoader from = Thread.currentThread().getContextClassLoader();
        return ((T) Proxy.newProxyInstance(from,
                new Class[]{interfaze}, new CrossClassloaderInvocationHandler(target, from)));
    }

    private class CrossClassloaderInvocationHandler implements InvocationHandler {

        CrossClassloaderInvocationHandler(Object target, ClassLoader fromClassLoader) {
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
