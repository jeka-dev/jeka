package org.jerkar.api.depmanagement;

import java.io.File;


/**
 * Not part of the public API.
 */

interface InternalDepResolver {

    /**
     * @param  module The resolved module. Only use for caching purpose. Can be <code>null</code>
     * @param resolvedScope if <code>null</code> it will resolve all scope
     * @param parameters can be null.
     * @param versionProvider can be null.
     */
    JkResolveResult resolve(JkVersionedModule module, JkDependencies deps, JkScope resolvedScope,
            JkResolutionParameters parameters, JkVersionProvider versionProvider);

    File get(JkModuleDependency dependency);

}