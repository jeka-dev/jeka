/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.java;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.function.Predicate;

/**
 * Wrapper class around {@link ClassLoader} providing convenient methods.
 */
public class JkClassLoader {

    private final ClassLoader delegate;

    private JkClassLoader(ClassLoader delegate) {
        this.delegate = delegate;
    }

    public static JkClassLoader of(ClassLoader classLoader) {
        JkUtilsAssert.argument(classLoader != null, "Wrapped classloader cannot be null.");
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
            String detail = JkLog.isVerbose() ? " on " + this : "";
            throw new IllegalArgumentException("Fail at loading class " + className + detail, e);
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
        return toString(delegate, Integer.MAX_VALUE);
    }

    public String toString(int maxEntryCount) {
        return toString(delegate, maxEntryCount);
    }

    /**
     * Returns all class names having a <code>main</code> method.
     */
    public List<String> findClassesHavingMainMethod() {
        return JkInternalClasspathScanner.of().findClassesWithMainMethod(this.delegate);
    }

    /**
     * Returns all classes matching the specified annotation predicate.
     */
    public List<String> findClassesMatchingAnnotations(Predicate<List<String>> annotationNamePredicate) {
        return JkInternalClasspathScanner.of().findClassesMatchingAnnotations(this.delegate, annotationNamePredicate);
    }

    /**
     * Returns the classpath for this classloader excluding elements on the platform/system classloader.
     */
    public JkPathSequence getClasspath() {
        return JkInternalClasspathScanner.of().getClasspath(this.delegate);
    }

    private static String ucltoString(URLClassLoader urlClassLoader, int maxEntryCount) {
        final StringBuilder builder = new StringBuilder();
        builder.append(urlClassLoader);
        int i = 0;
        for (final URL url : urlClassLoader.getURLs()) {
            if (i >= maxEntryCount) {
                builder.append("\n  ...");
                break;
            }
            builder.append("\n  ").append(url);
            i++;
        }
        return builder.toString();
    }

    private static String toString(ClassLoader classLoader, int maxEntryCount) {
        String result;
        if (classLoader instanceof URLClassLoader) {
            result = ucltoString((URLClassLoader) classLoader, maxEntryCount);
        } else {
            result = classLoader.toString();
        }
        if (classLoader.getParent() != null) {
            result = result + "\n" + toString(classLoader.getParent(), maxEntryCount);
        }
        return result;
    }

}
