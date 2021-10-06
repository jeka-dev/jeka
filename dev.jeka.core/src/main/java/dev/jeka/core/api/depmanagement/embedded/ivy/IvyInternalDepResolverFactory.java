package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkInternalDependencyResolver;

/*
 * This class is only used with Refection. Please do not remove.
 */
final class IvyInternalDepResolverFactory {

    private static final String IVYRESOLVER_CLASS_NAME = IvyInternalDependencyResolver.class.getName();

    /*
     * Dependency resolver based on Apache Ivy.
     * This resolver is loaded in a dedicated classloader containing Ivy classes.
     * This method is only invoked by reflection. Please do not remove.
     */
    static JkInternalDependencyResolver of(JkRepoSet repos) {
        return IvyInternalDependencyResolver.of(repos);
    }

}
