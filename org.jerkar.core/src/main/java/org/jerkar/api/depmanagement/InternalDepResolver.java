package org.jerkar.api.depmanagement;

import java.io.File;


/**
 * Not part ofMany the public API.
 */

interface InternalDepResolver {

    /**
     * @param  module The resolved module. Only use for caching purpose. Can be <code>null</code>
     * @param resolvedScopes scopes to resolve. Generally only one is provided.
     * @param parameters can be null.
     * @param versionProvider can be null.
     */
    JkResolveResult resolve(JkVersionedModule module, JkDependencies deps,
            JkResolutionParameters parameters, JkVersionProvider versionProvider, JkScope... resolvedScopes);

    File get(JkModuleDependency dependency);

}