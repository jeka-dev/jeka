package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class JkAttachedArtifacts implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<JkModuleId, Map<JkScope, Set<JkModuleDepFile>>> map= new HashMap<JkModuleId, Map<JkScope,Set<JkModuleDepFile>>>();

	public JkAttachedArtifacts() {
		super();
	}

	public Set<JkModuleDepFile> getArtifacts(JkModuleId moduleId, JkScope jkScope) {
		final Map<JkScope, Set<JkModuleDepFile>> subMap = map.get(moduleId);
		if (subMap == null) {
			return Collections.emptySet();
		}
		final Set<JkModuleDepFile> artifacts = subMap.get(jkScope);
		if (artifacts == null) {
			return Collections.emptySet();
		}
		return artifacts;

	}

	public void add(JkScope scope, JkModuleDepFile artifact) {
		Map<JkScope, Set<JkModuleDepFile>> subMap = map.get(artifact.versionedModule().moduleId());
		if (subMap == null) {
			subMap = new HashMap<JkScope, Set<JkModuleDepFile>>();
			map.put(artifact.versionedModule().moduleId(), subMap);
		}
		Set<JkModuleDepFile> subArtifacts = subMap.get(scope);
		if (subArtifacts == null) {
			subArtifacts = new HashSet<JkModuleDepFile>();
			subMap.put(scope, subArtifacts);
		}
		subArtifacts.add(artifact);
	}

	@Override
	public String toString() {
		return this.map.toString();
	}

}