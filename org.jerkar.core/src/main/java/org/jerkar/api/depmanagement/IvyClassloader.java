package org.jerkar.api.depmanagement;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkUrlClassLoader;

final class IvyClassloader {

    private static final String IVY_JAR_NAME = "ivy-2.5.0-rc1.jar";

    static final JkClassLoader CLASSLOADER = classloader();

    private IvyClassloader() {
        // no instance
    }

    private static JkClassLoader classloader() {
        if (JkClassLoader.ofCurrent().isDefined("org.apache.ivy.Ivy")) {
            return JkClassLoader.ofCurrent();
        }
        return JkUrlClassLoader.ofCurrent().getSibling(IvyClassloader.class.getResource(IVY_JAR_NAME)).toJkClassLoader();
    }

}
