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

package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Container for artifact files to publish.
 */
public final class JkArtifactPublisher {

    private final Map<JkArtifactId, Consumer<Path>> consumers = new LinkedHashMap<>();

    /**
     * Artifact locator to localize artifacts
     */
    public final JkArtifactLocator artifactLocator;

    private JkArtifactPublisher(JkArtifactLocator artifactLocator) {
        this.artifactLocator = artifactLocator;
    }

    public static JkArtifactPublisher of(JkArtifactLocator artifactLocator) {
        return new JkArtifactPublisher(artifactLocator);
    }

    /**
     * Returns all the artifact ids likely to be produced by this artifact producer. By default, it returns
     * a single element list containing the main artifact file id
     */
    public List<JkArtifactId> getArtifactIds() {
        return new LinkedList<>(consumers.keySet());
    }

    /**
     * Returns a string representation the artifacts that will be published.
     */
    public String info() {
        StringBuilder sb = new StringBuilder();
        getArtifactIds().forEach(artifactId -> {
            boolean producerProvided = consumers.get(artifactId) != null;
            String producerHint = producerProvided ? " (Producer provided)" : "";
            sb.append(artifactLocator.getArtifactPath(artifactId) +  producerHint + "\n");
        });
        return sb.toString();
    }

    JkArtifactPublisher putArtifact(JkArtifactId artifactId, Consumer<Path> artifactFileMaker) {
        consumers.put(artifactId, artifactFileMaker);
        return this;
    }

    JkArtifactPublisher removeArtifact(JkArtifactId artifactId) {
        consumers.remove(artifactId);
        return this;
    }

    void makeMissingArtifacts() {
        for (final JkArtifactId artifactId : getArtifactIds()) {
            Path path = artifactLocator.getArtifactPath(artifactId);
            if (Files.exists(path)) {
                continue;
            }
            Consumer<Path> consumer = consumers.get(artifactId);
            if (consumer == null) {
                throw new IllegalArgumentException("No artifact file and artifact maker found for " + artifactId + ". " +
                        "Artifact maker are defined for : " + consumers.entrySet());
            }
            JkLog.startTask("generate-artifact (missing %s)", path.getFileName());
            consumer.accept(path);
            JkLog.endTask();
        }
    }

    @Override
    public String toString() {
        return "ArtifactPublisher{" +
                "artifactLocator:" + artifactLocator +
                '}';
    }
}
