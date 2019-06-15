package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkInternalDepResolver;
import dev.jeka.core.api.java.JkClassLoader;

class IvyInternalDepResolverFactory {

    private static final String IVYRESOLVER_CLASS_NAME = IvyInternalDepResolver.class.getName();

    /**
     * Dependency resolver based on Apache Ivy.
     * This resolver is loaded in a dedicated classloader containing Ivy classes.
     */
    static JkInternalDepResolver of(JkRepoSet repos) {
        if (JkClassLoader.ofCurrent().isDefined(IvyClassloader.IVY_CLASS_NAME)) {
            return IvyInternalDepResolver.of(repos);
        }
        return IvyClassloader.CLASSLOADER.createCrossClassloaderProxy(
                JkInternalDepResolver.class, IVYRESOLVER_CLASS_NAME, "of", repos);
    }

}
