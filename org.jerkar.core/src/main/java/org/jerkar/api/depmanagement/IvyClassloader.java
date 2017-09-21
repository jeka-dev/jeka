package org.jerkar.api.depmanagement;

import org.jerkar.api.java.JkClassLoader;

final class IvyClassloader {

    static final JkClassLoader CLASSLOADER = classloader();

    private IvyClassloader() {
        // no instance
    }

    private static JkClassLoader classloader() {
        if (JkClassLoader.current().isDefined("org.apache.ivy.Ivy")) {
            return JkClassLoader.current();
        }
        return JkClassLoader.current().sibling(IvyClassloader.class.getResource("ivy-2.4.0.jar"));
    }

}
