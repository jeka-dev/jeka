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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Not part of public API
 */
public class JkInternalChildFirstClassLoader extends URLClassLoader {

    private final ClassLoader sysClzLoader;

    private JkInternalChildFirstClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        sysClzLoader = getSystemClassLoader();
    }

    public static JkInternalChildFirstClassLoader of(URL[] urls, ClassLoader parent) {
        return new JkInternalChildFirstClassLoader(urls, parent);
    }

    public static JkInternalChildFirstClassLoader of(Iterable<Path> paths, ClassLoader parent) {
        URL[] urls = JkPathSequence.of(paths)
                .and(JkInternalEmbeddedClassloader.embeddedLibs())
                .toUrls();
        return new JkInternalChildFirstClassLoader(urls, parent);
    }

    public static JkInternalChildFirstClassLoader of(Iterable<Path> paths) {
        URL[] urls = JkPathSequence.of(paths)
                .and(JkInternalEmbeddedClassloader.embeddedLibs())
                .toUrls();
        return new JkInternalChildFirstClassLoader(urls, Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // has the class loaded already?
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            try {
                if (sysClzLoader != null) {
                    loadedClass = sysClzLoader.loadClass(name);
                }
            } catch (ClassNotFoundException ex) {
                // class not found in system class loader... silently skipping
            }

            try {
                // find the class from given jar urls as in first constructor parameter.
                if (loadedClass == null) {
                    loadedClass = findClass(name);
                }
            } catch (ClassNotFoundException e) {
                // class is not found in the given urls.
                // Let's try it in parent classloader.
                // If class is still not found, then this method will throw class not found ex.
                loadedClass = super.loadClass(name, resolve);
            }
        }

        if (resolve) {      // marked to resolve
            resolveClass(loadedClass);
        }
        return loadedClass;
    }



    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> allRes = new LinkedList<>();

        // load resources from sys class loader
        Enumeration<URL> sysResources = sysClzLoader.getResources(name);
        if (sysResources != null) {
            while (sysResources.hasMoreElements()) {
                allRes.add(sysResources.nextElement());
            }
        }

        // load resource from this classloader
        Enumeration<URL> thisRes = findResources(name);
        if (thisRes != null) {
            while (thisRes.hasMoreElements()) {
                allRes.add(thisRes.nextElement());
            }
        }

        // then try finding resources from parent classloaders
        Enumeration<URL> parentRes = super.findResources(name);
        if (parentRes != null) {
            while (parentRes.hasMoreElements()) {
                allRes.add(parentRes.nextElement());
            }
        }

        return new Enumeration<URL>() {
            Iterator<URL> it = allRes.iterator();

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public URL nextElement() {
                return it.next();
            }
        };
    }

    @Override
    public URL getResource(String name) {
        URL res = null;
        if (sysClzLoader != null) {
            res = sysClzLoader.getResource(name);
        }
        if (res == null) {
            res = findResource(name);
        }
        if (res == null) {
            res = super.getResource(name);
        }
        return res;
    }
}