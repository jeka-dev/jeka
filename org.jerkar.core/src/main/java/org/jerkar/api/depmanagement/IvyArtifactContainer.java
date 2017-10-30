package org.jerkar.api.depmanagement;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;

/**
 * Created by angibaudj on 20-06-17.
 */
class IvyArtifactContainer {

    private final Map<JkVersionedModule, List<Path>> map = new HashMap<>();

    static IvyArtifactContainer of(ArtifactDownloadReport[] artifactDownloadReports) {
        IvyArtifactContainer result = new IvyArtifactContainer();
        for (ArtifactDownloadReport report : artifactDownloadReports) {
            result.put(report.getArtifact().getModuleRevisionId(), report.getLocalFile().toPath());
        }
        return result;
    }

    private void put(ModuleRevisionId moduleRevisionId, Path file) {
        JkVersionedModule versionedModule = IvyTranslations.toJkVersionedModule(moduleRevisionId);
        List<Path> files = map.computeIfAbsent(versionedModule, k -> new LinkedList<>());
        files.add(file);
    }

    List<Path> getArtifacts(JkVersionedModule versionedModule) {
        List<Path> result = map.get(versionedModule);
        if (result == null) {
            return new LinkedList<>();
        }
        return result;
    }

}
