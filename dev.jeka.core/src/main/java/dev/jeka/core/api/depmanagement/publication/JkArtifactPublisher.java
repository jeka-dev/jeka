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
            boolean producerProvided = consumers.containsKey(artifactId);
            String producerHint = producerProvided ? " : Producer provided" : "";
            sb.append(artifactId + producerHint + "\n");
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
            JkLog.startTask("Artifact file missing %s : generating artifact...", path.getFileName());
            consumer.accept(path);
            JkLog.endTask();
        }
    }
}
