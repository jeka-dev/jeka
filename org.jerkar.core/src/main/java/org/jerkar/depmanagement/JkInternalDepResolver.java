package org.jerkar.depmanagement;

/**
 * Not part of the public API.
 */
import java.util.Set;

import org.jerkar.internal.ivy.IvyResolver.JkAttachedArtifacts;

public interface JkInternalDepResolver {

	boolean hasMavenPublishRepo();

	boolean hasIvyPublishRepo();

	Set<JkArtifact> resolveAnonymous(JkDependencies deps,
			JkScope resolvedScope, JkResolutionParameters parameters);

	Set<JkArtifact> resolve(JkVersionedModule module, JkDependencies deps,
			JkScope resolvedScope, JkResolutionParameters parameters);

	/**
	 * Get artifacts of the given modules published for the specified scopes (no
	 * transitive resolution).
	 */
	JkAttachedArtifacts getArtifacts(Iterable<JkVersionedModule> modules,
			JkScope... scopes);

}