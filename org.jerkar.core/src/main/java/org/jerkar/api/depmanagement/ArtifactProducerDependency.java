package org.jerkar.api.depmanagement;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.utils.JkUtilsIterable;


/*
 * A dependency on a given artifact file id ofMany a given {@link JkArtifactProducer}
 */
class ArtifactProducerDependency extends JkComputedDependency  {

    private static final long serialVersionUID = 1L;

    private final JkArtifactProducer artifactProducer;

    /*
     * Constructs a {@link ArtifactProducerDependency} from an artifact producer and the artifact file id
     * one is interested on.
     */
    ArtifactProducerDependency(JkArtifactProducer producer,
            Iterable<JkArtifactFileId> fileIds) {
        super(() -> producer.makeArtifactFilesIfNecessary(artifacts(producer, fileIds)),
                baseDir(producer),
                jars(producer, artifacts(producer, fileIds)), () -> runtimeDeps(producer, artifacts(producer, fileIds)));
        this.artifactProducer = producer;
    }

    /*
     * Constructs a {@link ArtifactProducerDependency} from an artifact producer and the artifact file id
     * one is interested on.
     */
    ArtifactProducerDependency(JkArtifactProducer artifactProducer,
            JkArtifactFileId ... artifactFileIds) {
        this(artifactProducer, Arrays.asList(artifactFileIds));
    }

    private static Iterable<JkArtifactFileId> artifacts(JkArtifactProducer artifactProducer, Iterable<JkArtifactFileId> artifactFileIds) {
        if (!artifactFileIds.iterator().hasNext()) {
            return JkUtilsIterable.listOf(artifactProducer.mainArtifactFileId());
        }
        return artifactFileIds;
    }

    private static List<Path> jars(JkArtifactProducer producer, Iterable<JkArtifactFileId> artifactIds) {
        JkPathSequence result = JkPathSequence.of();
        for (final JkArtifactFileId artifactFileId : artifactIds) {
            result = result.and( producer.artifactPath(artifactFileId));
        }
        return result.withoutDuplicates().entries();
    }

    private static List<Path> runtimeDeps(JkArtifactProducer producer, Iterable<JkArtifactFileId> artifactIds) {
        JkPathSequence result = JkPathSequence.of();
        for (final JkArtifactFileId artifactFileId : artifactIds) {
            result = result.andMany( producer.runtimeDependencies(artifactFileId));
        }
        return result.withoutDuplicates().entries();
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
