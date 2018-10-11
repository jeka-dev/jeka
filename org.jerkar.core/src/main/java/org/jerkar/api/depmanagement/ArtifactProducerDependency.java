package org.jerkar.api.depmanagement;

import java.nio.file.Path;
import java.util.Arrays;

import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.utils.JkUtilsIterable;


/*
 * A dependency on a given artifact file id of a given {@link JkArtifactProducer}
 */
class ArtifactProducerDependency extends JkComputedDependency  {

    private static final long serialVersionUID = 1L;

    private final JkArtifactProducer artifactProducer;

    /*
     * Constructs a {@link ArtifactProducerDependency} from an artifact producer and the artifact file id
     * one is interested on.
     */
    ArtifactProducerDependency(JkArtifactProducer producer, Iterable<JkArtifactId> fileIds) {
        super(() -> producer.makeArtifactsIfAbsent(artifacts(producer, fileIds)),
                baseDir(producer),
                jars(producer, artifacts(producer, fileIds)), () -> runtimeDeps(producer, artifacts(producer, fileIds)));
        this.artifactProducer = producer;
    }

    /*
     * Constructs a {@link ArtifactProducerDependency} from an artifact producer and the artifact file id
     * one is interested on.
     */
    ArtifactProducerDependency(JkArtifactProducer artifactProducer,
            JkArtifactId... artifactFileIds) {
        this(artifactProducer, Arrays.asList(artifactFileIds));
    }

    private static Iterable<JkArtifactId> artifacts(JkArtifactProducer artifactProducer, Iterable<JkArtifactId> artifactFileIds) {
        if (!artifactFileIds.iterator().hasNext()) {
            return JkUtilsIterable.listOf(artifactProducer.mainArtifactId());
        }
        return artifactFileIds;
    }

    private static Iterable<Path> jars(JkArtifactProducer producer, Iterable<JkArtifactId> artifactIds) {
        JkPathSequence result = JkPathSequence.of();
        for (final JkArtifactId artifactFileId : artifactIds) {
            result = result.withAppending( producer.artifactPath(artifactFileId));
        }
        return result.withoutDuplicates();
    }

    private static Iterable<Path> runtimeDeps(JkArtifactProducer producer, Iterable<JkArtifactId> artifactIds) {
        JkPathSequence result = JkPathSequence.of();
        for (final JkArtifactId artifactFileId : artifactIds) {
            result = result.withAppendingMany( producer.runtimeDependencies(artifactFileId));
        }
        return result.withoutDuplicates();
    }

    private static Path baseDir(JkArtifactProducer artifactProducer) {
        if (artifactProducer instanceof JkFileSystemLocalizable) {
            return ((JkFileSystemLocalizable) artifactProducer).baseDir();
        }
        return null;
    }

    @Override
    public String toString() {
        return this.artifactProducer.toString();
    }
}
