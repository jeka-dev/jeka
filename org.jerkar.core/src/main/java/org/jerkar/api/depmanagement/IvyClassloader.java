package org.jerkar.api.depmanagement;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;

final class IvyClassloader {

    private static final String IVY_JAR_NAME = "ivy-2.4.0.jar";

    static final String IVY_CLASS_NAME = "org.apache.ivy.Ivy";

    static final JkClassLoader CLASSLOADER = classloader();

    private IvyClassloader() {
        // no instance
    }

    private static JkClassLoader classloader() {
        if (JkClassLoader.ofCurrent().isDefined(IVY_CLASS_NAME)) {
            return JkClassLoader.ofCurrent();
        }
        // We can not just create a new url classloader on top of the current one cause Jerkar classes expect to
        // load Ivy classes.
        return JkUrlClassLoader.ofCurrent().getSibling(IvyClassloader.class.getResource(IVY_JAR_NAME)).toJkClassLoader();
    }

}
