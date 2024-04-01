/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
