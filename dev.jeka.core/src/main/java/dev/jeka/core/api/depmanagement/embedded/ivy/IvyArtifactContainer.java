package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.system.JkLog;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Jerome Angibaud on 20-06-17.
 */
final class IvyArtifactContainer {

    private final Map<JkCoordinate, List<Path>> map = new HashMap<>();

    static IvyArtifactContainer of(ArtifactDownloadReport[] artifactDownloadReports) {
        IvyArtifactContainer result = new IvyArtifactContainer();
        for (ArtifactDownloadReport report : artifactDownloadReports) {
            if (report.getLocalFile() == null) {
                JkLog.warn("File for " + report.getArtifact() + " hasn't been downloaded.");
                continue;
                //throw new IllegalStateException("File for " + report.getArtifact() + " hasn't been downloaded.");
            }
            result.put(report.getArtifact().getModuleRevisionId(), report.getLocalFile().toPath());
        }
        return result;
    }

    private void put(ModuleRevisionId moduleRevisionId, Path file) {
        JkCoordinate coordinate = IvyTranslatorToDependency.toJkCoordinate(moduleRevisionId);
        List<Path> files = map.computeIfAbsent(coordinate, k -> new LinkedList<>());
        files.add(file);
    }

    List<Path> getArtifacts(JkCoordinate coordinate) {
        List<Path> result = map.get(coordinate);
        if (result == null) {
            return new LinkedList<>();
        }
        return result;
    }

}
