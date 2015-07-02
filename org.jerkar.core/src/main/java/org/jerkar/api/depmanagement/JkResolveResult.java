package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Result of a module dependency resolution for a given scope.<br/>
 * When resolving a module dependencies for a given scope, we expect to get <ul>
 * <li>The list of local file constituting the concrete dependencies (the jar files for instances)</li>
 * <li>The list of versioned module id constituting the concrete dependencies</li>
 * <li>the {@link JkVersionProvider} that specify which static version has been taken in account when the module declares dynamic versions (as 1.0.+)</li>
 * </ul>
 */
public final class JkResolveResult implements Serializable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public static JkResolveResult empty() {
		return of(Collections.EMPTY_LIST);
	}

	public static JkResolveResult of(List<JkArtifact> artifacts,
			JkVersionProvider jkVersionProvider) {
		return new JkResolveResult(artifacts, jkVersionProvider);
	}

	public static JkResolveResult of(List<JkArtifact> artifacts) {
		return new JkResolveResult(artifacts, JkVersionProvider.empty());
	}

	private final List<JkArtifact> jkArtifacts;

	private final JkVersionProvider jkVersionProvider;

	private JkResolveResult(List<JkArtifact> artifacts, JkVersionProvider jkVersionProvider) {
		super();
		this.jkArtifacts = artifacts;
		this.jkVersionProvider = jkVersionProvider;
	}

	public List<File> localFiles() {
		final List<File> result = new LinkedList<File>();
		for (final JkArtifact artifact : this.jkArtifacts) {
			result.add(artifact.localFile());
		}
		return result;
	}

	public Set<JkVersionedModule> involvedModules() {
		final Set<JkVersionedModule> result = new HashSet<JkVersionedModule>();
		for (final JkArtifact artifact : this.jkArtifacts) {
			result.add(artifact.versionedModule());
		}
		return result;
	}

	public JkVersionProvider resolvedVersionProvider() {
		return jkVersionProvider;
	}

	public List<File> filesOf(JkModuleId jkModuleId) {
		final List<File> result = new LinkedList<File>();
		for (final JkArtifact artifact : this.jkArtifacts) {
			if (jkModuleId.equals(artifact.versionedModule().moduleId())) {
				result.add(artifact.localFile());
			}
		}
		return result;
	}


	public JkResolveResult and(JkResolveResult other) {
		final List<JkArtifact> artifacts = new LinkedList<JkArtifact>(this.jkArtifacts);
		artifacts.addAll(other.jkArtifacts);
		final JkVersionProvider jkVersionProvider = this.jkVersionProvider.and(other.jkVersionProvider);
		return new JkResolveResult(artifacts, jkVersionProvider);
	}



}
