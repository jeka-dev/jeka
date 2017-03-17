package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.*;

/**
 * Created by djeang on 17-03-17.
 */
public class JkArtifactsWithClassifier implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<JkModuleId, Map<String, Set<JkModuleDepFile>>> map = new HashMap<JkModuleId, Map<String, Set<JkModuleDepFile>>>();

    /**
     * Construct an empty {@link JkAttachedArtifacts}
     */
    public JkArtifactsWithClassifier() {
        super();
    }

    /**
     * Returns the extra artifacts of the specified module attached with the specified scope.
     */
    public Set<JkModuleDepFile> getArtifacts(JkModuleId moduleId, String classifier) {
        final Map<String, Set<JkModuleDepFile>> subMap = map.get(moduleId);
        if (subMap == null) {
            return Collections.emptySet();
        }
        final Set<JkModuleDepFile> artifacts = subMap.get(classifier);
        if (artifacts == null) {
            return Collections.emptySet();
        }
        return artifacts;

    }

    /**
     * Add a attached artifact to this {@link JkAttachedArtifacts}
     */
    public void add(String classifier, JkModuleDepFile artifact) {
        Map<String, Set<JkModuleDepFile>> subMap = map.get(artifact.versionedModule().moduleId());
        if (subMap == null) {
            subMap = new HashMap<String, Set<JkModuleDepFile>>();
            map.put(artifact.versionedModule().moduleId(), subMap);
        }
        Set<JkModuleDepFile> subArtifacts = subMap.get(classifier);
        if (subArtifacts == null) {
            subArtifacts = new HashSet<JkModuleDepFile>();
            subMap.put(classifier, subArtifacts);
        }
        subArtifacts.add(artifact);
    }

    @Override
    public String toString() {
        return this.map.toString();
    }


}
