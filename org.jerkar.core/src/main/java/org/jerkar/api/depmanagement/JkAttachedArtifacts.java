package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Describe the mapping between {@link JkModuleId} and their extra attached artifacts. JkExtraPacking artifacts are
 * attached to a module through a scope. This mean that for getting attached artifact you can request a dependency
 * with a scope mapping having a right side matching to the desired files.
 */
public final class JkAttachedArtifacts implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<JkModuleId, Map<JkScope, Set<JkModuleDepFile>>> map = new HashMap<JkModuleId, Map<JkScope, Set<JkModuleDepFile>>>();

    /**
     * Construct an empty {@link JkAttachedArtifacts}
     */
    public JkAttachedArtifacts() {
        super();
    }

    /**
     * Returns the extra artifacts of the specified module attached with the specified scope.
     */
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

    /**
     * Add a attached artifact to this {@link JkAttachedArtifacts}
     */
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