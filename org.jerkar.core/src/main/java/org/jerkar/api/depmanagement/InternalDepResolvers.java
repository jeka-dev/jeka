package org.jerkar.api.depmanagement;

import dev.jeka.core.api.java.JkClassLoader;

class InternalDepResolvers {

    private static final String IVYRESOLVER_CLASS_NAME = IvyResolver.class.getName();

    /**
     * Dependency resolver based on Apache Ivy.
     * This resolver is loaded in a dedicated classloader containing Ivy classes.
     */
    static ModuleDepResolver ivy(JkRepoSet repos) {
        if (JkClassLoader.ofCurrent().isDefined(IvyClassloader.IVY_CLASS_NAME)) {
            return IvyResolver.of(repos);
        }
        return IvyClassloader.CLASSLOADER.createCrossClassloaderProxy(
                ModuleDepResolver.class, IVYRESOLVER_CLASS_NAME, "of", repos);
    }

}
