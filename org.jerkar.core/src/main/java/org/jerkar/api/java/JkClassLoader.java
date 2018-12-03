package org.jerkar.api.java;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsReflect;

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
        return new JkClassLoader(classLoader);
    }

    public static JkClassLoader ofCurrent() {
        return of(JkClassLoader.class.getClassLoader());
    }

    /**
     * Returns a {@link JkClassLoader} wrapping the class loader having loaded
     * the specified class.
     */
    public static JkClassLoader ofLoaderOf(Class<?> clazz) {
        return new JkClassLoader(clazz.getClassLoader());
    }

    /**
     * Return the {@link URLClassLoader} wrapped by this object.
     */
    public ClassLoader getClassloader() {
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
        if (args == null) {
            args = new Object[0];
        }
        final Class<?> clazz = this.load(className);
        final Object[] effectiveArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            effectiveArgs[i] = traverseClassLoader(args[i], this);
        }
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(delegate);
        initLogInClassloader();
        try {
            final Object returned = JkUtilsReflect.invokeStaticMethod(clazz, methodName,
                    effectiveArgs);
            final T result;
            if (serializeResult) {
                result = (T) traverseClassLoader(returned, JkClassLoader.ofCurrent());
            } else {
                result = (T) returned;
            }
            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
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

    private static Object traverseClassLoader(Object object, JkClassLoader to) {
        if (object == null) {
            return null;
        }
        final Class<?> clazz = object.getClass();
        final String className;
        if (clazz.isArray()) {
            className = object.getClass().getComponentType().getName();
        } else {
            className = object.getClass().getName();
        }

        final JkClassLoader from = JkClassLoader.ofLoaderOf(object.getClass());
        final Class<?> toClass = to.load(className);
        final boolean container = Collection.class.isAssignableFrom(clazz)
                || Map.class.isAssignableFrom(clazz);
        if (from.delegate == null && !container) { // Class from JDK
            return object;
        }
        if (toClass.equals(object.getClass()) && !container) {
            return object;
        }

        return JkUtilsIO.cloneBySerialization(object, to.getClassloader());
    }

    private class TransClassloaderInvokationHandler implements InvocationHandler {

        TransClassloaderInvokationHandler(Object target) {
            super();
            this.target = target;
        }

        private final Object target;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final Method targetMethod = JkUtilsReflect.methodWithSameNameAndArgType(method,
                    target.getClass());
            return invokeInstanceMethod(true, target, targetMethod, args);

        }

    }

    /**
     * Creates an instance from the specified class in this classloader and
     * callable from the current class loader. Arguments ans result are
     * serialized (if needed) so we keep compatibility between classes.
     */
    @SuppressWarnings("unchecked")
    public <T> T createTransClassloaderProxy(Class<T> interfaze, String className,
                                             String staticMethodFactory, Object... args) {
        final Object target = this.invokeStaticMethod(false, className, staticMethodFactory, args);
        return ((T) Proxy.newProxyInstance(JkClassLoader.ofCurrent().delegate,
                new Class[]{interfaze}, new TransClassloaderInvokationHandler(target)));
    }

    /**
     * Invokes an instance method on the specified object using the specified
     * arguments. <br/>
     * If the argument classes are the same on the current class loader and this
     * one then arguments are passed as is, otherwise arguments are serialized
     * in the current class loader and deserialized andAccept this class loader of
     * order to be compliant with it. <br/>
     * The current thread context class loader is switched to this for the
     * method execution. <br/>
     * It is then turned back to the former one when the execution is done.
     */
    @SuppressWarnings("unchecked")
    public <T> T invokeInstanceMethod(boolean serializeResult, Object object, Method method,
                                      Object... args) {
        if (args == null) {
            args = new Object[0];
        }
        final Object[] effectiveArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            effectiveArgs[i] = traverseClassLoader(args[i], this);
        }
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(delegate);
        initLogInClassloader();
        try {

            final Object returned = JkUtilsReflect.invoke(object, method, effectiveArgs);
            final T result;
            if (serializeResult) {
                result = (T) traverseClassLoader(returned, JkClassLoader.ofCurrent());
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
        JkLog.initializeInClassLoader(this.getClassloader());
    }


}
