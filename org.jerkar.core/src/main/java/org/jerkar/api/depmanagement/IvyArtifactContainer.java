package org.jerkar.api.depmanagement;

import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by angibaudj on 20-06-17.
 */
class IvyArtifactContainer {

    private final Map<JkVersionedModule, List<File>> map = new HashMap<JkVersionedModule, List<File>>();

    static IvyArtifactContainer of(ArtifactDownloadReport[] artifactDownloadReports) {
        IvyArtifactContainer result = new IvyArtifactContainer();
        for (ArtifactDownloadReport report : artifactDownloadReports) {
            result.put(report.getArtifact().getModuleRevisionId(), report.getLocalFile());
        }
        return result;
    }

    private void put(ModuleRevisionId moduleRevisionId, File file) {
        JkVersionedModule versionedModule = IvyTranslations.toJkVersionedModule(moduleRevisionId);
        List<File> files = map.get(versionedModule);
        if (files == null) {
            files = new LinkedList<File>();
            map.put(versionedModule, files);
        }
        files.add(file);
    }

    List<File> getArtifacts(JkVersionedModule versionedModule) {
        List<File> result = map.get(versionedModule);
        if (result == null) {
            return new LinkedList<File>();
        }
        return result;
    }

}
