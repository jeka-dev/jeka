package org.jerkar.api.depmanagement;

import java.io.File;


/**
 * Not part of the public API.
 */

interface InternalDepResolver {

    JkResolveResult resolveAnonymous(JkDependencies deps, JkScope resolvedScope,
            JkResolutionParameters parameters);

    /**
     * @param resolvedScope
     *            can be null.
     */
    JkResolveResult resolve(JkVersionedModule module, JkDependencies deps, JkScope resolvedScope,
            JkResolutionParameters parameters);

    /**
     * Get artifacts of the given modules published for the specified scopes (no
     * transitive resolution).
     */
    JkAttachedArtifacts getArtifacts(Iterable<JkVersionedModule> modules, JkScope... scopes);

    File get(JkModuleDependency dependency);

}