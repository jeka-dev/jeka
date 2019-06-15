package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.File;


/**
 * Not part of the public API.
 */

public interface JkInternalDepResolver {

    static final String FACTORY_CLASS_NAME = "dev.jeka.core.api.depmanagement.embedded.ivy.IvyInternalDepResolverFactory";

    /**
     * @param  module The resolved module. Only use for caching purpose. Can be <code>null</code>
     * @param resolvedScopes scopes to resolve. Generally only one is provided.
     * @param parameters can be null.
     */
    JkResolveResult resolve(JkVersionedModule module, JkDependencySet deps,
            JkResolutionParameters parameters, JkScope... resolvedScopes);

    File get(JkModuleDependency dependency);

    static JkInternalDepResolver of(JkRepoSet repos) {
        final JkInternalDepResolver ivyPublisher;
        Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(FACTORY_CLASS_NAME);
        if (factoryClass != null) {
            ivyPublisher = JkUtilsReflect.invokeStaticMethod(factoryClass, "of", repos);
        } else {
            throw new IllegalStateException("Use embedded class loader");
        }
        return ivyPublisher;
    }

}