package dev.jeka.core.api.depmanagement;

import java.io.File;


/**
 * Not part of the public API.
 */

interface ModuleDepResolver {

    /**
     * @param  module The resolved module. Only use for caching purpose. Can be <code>null</code>
     * @param resolvedScopes scopes to resolve. Generally only one is provided.
     * @param parameters can be null.
     */
    JkResolveResult resolve(JkVersionedModule module, JkDependencySet deps,
            JkResolutionParameters parameters, JkScope... resolvedScopes);

    File get(JkModuleDependency dependency);

}