package dev.jeka.core.api.java;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Map;

public class JkClassLoader {

    private static final int JAVA_SUFFIX_LENGTH = ".java".length();

    private final ClassLoader delegate;

    private JkClassLoader(ClassLoader delegate) {
        this.delegate = delegate;
    }

    public static JkClassLoader of(ClassLoader classLoader) {
        JkUtilsAssert.notNull(classLoader, "Wrapped classloader cannot be null.");
        return new JkClassLoader(classLoader);
    }

    /**
     * Returns a {@link JkClassLoader} wrapping the current thread context classloader.
     */
    public static JkClassLoader ofCurrent() {
        return of(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Returns a {@link JkClassLoader} wrapping the class loader having loaded
     * the specified class.
     */
    public static JkClassLoader ofLoaderOf(Class<?> clazz) {
        return of(clazz.getClassLoader() == null ? ClassLoader.getSystemClassLoader() : clazz.getClassLoader());
    }

    /**
     * Return the {@link URLClassLoader} wrapped by this object.
     */
    public ClassLoader get() {
        return delegate;
    }

    /**
     * Delegates the call to {@link ClassLoader#loadClass(String)} of this
     * wrapped <code>class loader</code>.<br/>
     * The specified class is supposed to be defined in this class loader,
     * otherwise an {@link IllegalArgumentException} is thrown.
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> load(String className) {
        try {
            return (Class<T>) delegate.loadClass(className);
        } catch (final ClassNotFoundException | NoClassDefFoundError e) {
            throw new IllegalArgumentException("Fail at loading class " + className + " on " + this, e);
        }
    }

    /**
     * Returns if the specified class is defined in this <code>classloader</code>.
     */
    public boolean isDefined(String className) {
        try {
            delegate.loadClass(className);
            return true;
        } catch (final ClassNotFoundException e) { // NOSONAR
            return false;
        }
    }

    /**
     * Loads the class having the specified name or return <code>null</code> if
     * no such class is defined in this <code>classloader</code>.
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> loadIfExist(String className) {
        try {
            return (Class<T>) delegate.loadClass(className);
        } catch (final ClassNotFoundException e) { // NOSONAR
            return null;
        }
    }

    /**
     * Loads a class given its source relative path. For example
     * <code>loadGivenSourcePath("mypack1/subpack/MyClass.java")
     * will load the class <code>mypack1.subpack.MyClass</code>.
     */
    public <T> Class<T> loadGivenClassSourcePath(String classSourcePath) {
        final String className = classSourcePath.replace('/', '.').replace('\\', '.')
                .substring(0, classSourcePath.length() - JAVA_SUFFIX_LENGTH);
        return load(className);
    }

    /**
     * Returns <code>true</code> if this classloader is descendant or same as the specified classloader.
     */
    public boolean isDescendantOf(ClassLoader classLoader) {
        if (this.delegate.getParent() == null) {
            return false;
        }
        if (this.delegate.equals(classLoader)) {
            return true;
        }
        return JkClassLoader.of(delegate.getParent()).isDescendantOf(classLoader);
    }

    /**
     * Invokes a static method on the specified class using the provided
     * arguments. <br/>
     * If the argument classes are the same on the current class loader and this
     * one then arguments are passed as is, otherwise arguments are serialized
     * in the current class loader and deserialized in this class loader in
     * order to be compliant with it. <br/>
     * The current thread context class loader is switched to this for the
     * method execution. <br/>
     * It is then turned back to the former one when the execution is done.
     */
    @SuppressWarnings("unchecked")
    public <T> T invokeStaticMethod(boolean serializeResult, String className, String methodName,
                                    Object... args) {
        final Object[] effectiveArgs = crossClassloaderArgs(args);
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(delegate);
        try {
            initLogInClassloader();
            final Class<?> clazz = this.load(className);
            final Object returned = JkUtilsReflect.invokeStaticMethod(clazz, methodName,
                    effectiveArgs);
            final T result;
            if (serializeResult) {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
                result = (T) crossClassLoader(returned, currentClassLoader);
            } else {
                result = (T) returned;
            }
            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private Object[] crossClassloaderArgs(Object[] args) {
        if (args == null) {
            args = new Object[0];
        }
        final Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = crossClassLoader(args[i], delegate);
        }
        return result;
    }

    /**
     * Creates an instance of the class having the specified name in this class
     * loader.
     */
    @SuppressWarnings("unchecked")
    public <T> T instantiate(String className) {
        final Class<?> clazz = this.load(className);
        return (T) JkUtilsReflect.newInstance(clazz);
    }

    @Override
    public String toString() {
        if (delegate instanceof URLClassLoader) {
            return JkUrlClassLoader.of((URLClassLoader) delegate).toString();
        }
        else return delegate.toString();
    }

    private static Object crossClassLoader(Object object, ClassLoader to) {
        if (object == null) {
            return null;
        }
        if (JkClassLoader.of(to).isDescendantOf(object.getClass().getClassLoader())) {
            return object;
        }
        final Class<?> clazz = object.getClass();
        final String className;
        if (clazz.isArray()) {
            className = object.getClass().getComponentType().getName();
        } else {
            className = object.getClass().getName();
        }

        final JkClassLoader from = JkClassLoader.ofLoaderOf(object.getClass());
        final Class<?> toClass = JkClassLoader.of(to).load(className);
        final boolean container = Collection.class.isAssignableFrom(clazz)
                || Map.class.isAssignableFrom(clazz);
        if (from.delegate == null && !container) { // Class from JDK
            return object;
        }
        if (toClass.equals(object.getClass()) && !container) {
            return object;
        }

        return JkUtilsIO.cloneBySerialization(object, to);
    }

    private class CrossClassloaderInvokationHandler implements InvocationHandler {

        CrossClassloaderInvokationHandler(Object target, ClassLoader fromClassLoader) {
            this.targetObject = target;
            this.fromClassLoader = fromClassLoader;
        }

        private final Object targetObject;

        private final ClassLoader fromClassLoader;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final Method targetMethod = JkUtilsReflect.methodWithSameNameAndArgType(method,
                    targetObject.getClass());
            return invokeInstanceMethod(fromClassLoader, targetObject, targetMethod, args);
        }

    }

    /**
     * Creates an instance of the specified class in this classloader and
     * callable from the current thread classloader. Arguments ans result are
     * serialized (if needed) so we keep compatibility between classes.
     */
    @SuppressWarnings("unchecked")
    public <T> T createCrossClassloaderProxy(Class<T> interfaze, String className,
                                             String staticMethodFactory, Object... args) {
        final Object target = this.invokeStaticMethod(false, className, staticMethodFactory, args);
        ClassLoader from = Thread.currentThread().getContextClassLoader();
        return ((T) Proxy.newProxyInstance(from,
                new Class[]{interfaze}, new CrossClassloaderInvokationHandler(target, from)));
    }

    /**
     * Invoke instance method on specified object using this classloader as the curent context class loader.
     *
     * @param from If not <code>null</code> the result is transformed to be used in a specified classloader.
     *
     **/
    @SuppressWarnings("unchecked")
    public <T> T invokeInstanceMethod(ClassLoader from, Object object, Method method,
                                      Object... args) {
        final Object[] effectiveArgs = crossClassloaderArgs(args);
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(delegate);
        initLogInClassloader();
        try {
            final Object returned = JkUtilsReflect.invoke(object, method, effectiveArgs);
            final T result;
            if (from != null) {
                result = (T) crossClassLoader(returned, from);
            } else {
                result = (T) returned;
            }
            return result;
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    /**
     * Loads a class given its source relative path if exists. For example
     * <code>loadGivenSourcePath("mypack1/subpack/MyClass.java")
     * will load the class <code>mypack1.subpack.MyClass</code>. Returns
     * <code>null</code> if no such class exists.
     */
    public <T> Class<T> loadGivenClassSourcePathIfExist(String classSourcePath) {
        final String className = classSourcePath.replace('/', '.').replace('\\', '.')
                .substring(0, classSourcePath.length() - JAVA_SUFFIX_LENGTH);
        return loadIfExist(className);
    }

    private void initLogInClassloader() {
        JkLog.initializeInClassLoader(this.get());
    }


}
