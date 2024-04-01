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
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link URLClassLoader} offering convenient methods and fluent
 * interface to deal with <code>URLClassLoader</code>.
 *
 * @author Jerome Angibaud
 */
public final class JkUrlClassLoader {

    private final URLClassLoader delegate;

    private JkUrlClassLoader(URLClassLoader delegate) {
        this.delegate = delegate;
    }

    public static JkUrlClassLoader of(URLClassLoader urlClassLoader) {
        return new JkUrlClassLoader(urlClassLoader);
    }

    public static JkUrlClassLoader of(Iterable<Path> paths) {
        return of(new URLClassLoader(toUrl(paths)));
    }

    public static JkUrlClassLoader of(Iterable<Path> paths, ClassLoader parent) {
        final List<Path> cleanedPath = JkUtilsPath.disambiguate(paths);
        return of(new URLClassLoader(toUrl(cleanedPath), parent));
    }

    /**
     * Returns a {@link JkUrlClassLoader} wrapping the current thread context classloader.
     *
     * @see Class#getClassLoader()
     */
    public static JkUrlClassLoader ofCurrent() {
        return wrapUrlClassLoader(Thread.currentThread().getContextClassLoader());
    }

    private static JkUrlClassLoader wrapUrlClassLoader(ClassLoader classLoader) {
        if (! (classLoader instanceof URLClassLoader)) {
            throw new IllegalStateException("The current or system classloader is not instance of URLClassLoader but "
                    + classLoader.getClass() + ". It is probably due that you are currently running on JDK9.");
        }
        return of((URLClassLoader) classLoader);
    }

    /**
     * Return the {@link URLClassLoader} wrapped by this object.
     */
    public URLClassLoader get() {
        return delegate;
    }

    /**
     * Returns the class loader parent of this one.
     */
    public JkClassLoader getParent() {
        return JkClassLoader.of(this.delegate.getParent());
    }

    /**
     * Returns the classpath of this classloader without mentioning classpath of
     * the parent classloaders.
     */
    public JkPathSequence getDirectClasspath() {
        return JkPathSequence.of(JkUtilsSystem.classloaderEntries(this.delegate));
    }

    public JkClassLoader toJkClassLoader() {
        return JkClassLoader.of(delegate);
    }

    @Override
    public String toString() {
        return toJkClassLoader().toString();
    }

    private static URL[] toUrl(Iterable<Path> paths) {
        List<Path> pathList = JkUtilsPath.disambiguate(paths);
        final List<URL> urls = new ArrayList<>();
        for (final Path file : pathList) {
            try {
                urls.add(file.toUri().toURL());
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException(file + " is not convertible to URL");
            }
        }
        return urls.toArray(new URL[0]);
    }

}
