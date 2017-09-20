package org.jerkar.api.depmanagement;

import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.utils.JkUtilsIterable;

import java.io.File;
import java.util.Arrays;
import java.util.List;


/*
 * A dependency on a given artifact file id of a given {@link JkArtifactProducer}
 */
class ArtifactProducerDependency extends JkComputedDependency  {

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

    private static List<File> jars(JkArtifactProducer producer, Iterable<JkArtifactFileId> artifactIds) {
        JkPath result = JkPath.of();
        for (JkArtifactFileId artifactFileId : artifactIds) {
            result = result.and( producer.artifactFile(artifactFileId));
        }
        return result.withoutDuplicates().entries();
    }

    private static List<File> runtimeDeps(JkArtifactProducer producer, Iterable<JkArtifactFileId> artifactIds) {
        JkPath result = JkPath.of();
        for (JkArtifactFileId artifactFileId : artifactIds) {
            result = result.and( producer.runtimeDependencies(artifactFileId));
        }
        return result.withoutDuplicates().entries();
    }

    private static File baseDir(JkArtifactProducer artifactProducer) {
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
