package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsString;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class IvyTranslatorToArtifact {

    private static final String EXTRA_NAMESPACE = "http://ant.apache.org/ivy/extra";

    private static final String EXTRA_PREFIX = "e";

    static Map<String, Artifact> toMavenArtifacts(JkVersionedModule versionedModule, JkArtifactLocator artifactLocator) {
        Map<String, Artifact> result = new HashMap<>();
        Instant now = Instant.now();
        for (JkArtifactId artifactId : artifactLocator.getArtifactIds()) {
            Path file = artifactLocator.getArtifactPath(artifactId);
            ModuleRevisionId moduleRevisionId = IvyTranslatorToDependency.toModuleRevisionId(versionedModule);
            String classifier = artifactId.getName();
            final Artifact artifact = toMavenArtifact(file, classifier, moduleRevisionId, now);
            result.put(classifier, artifact);
        }
        return result;
    }

    static Map<String, Artifact> toIvyArtifacts(JkVersionedModule versionedModule,
                                                List<JkIvyPublication.JkPublicationArtifact> jkArtifacts) {
        Map<String, Artifact> result = new HashMap<>();
        Instant now = Instant.now();
        for (JkIvyPublication.JkPublicationArtifact jkArtifact : jkArtifacts) {
            ModuleRevisionId moduleRevisionId = IvyTranslatorToDependency.toModuleRevisionId(versionedModule);
            final Artifact artifact = toIvyArtifact(jkArtifact, moduleRevisionId, now);
            result.put(jkArtifact.configuration, artifact);
        }
        return result;
    }

    static void bind(DefaultModuleDescriptor descriptor, Map<String, Artifact> artifactMap) {
        artifactMap.forEach((classifier, artifact) -> {
            String conf = JkArtifactId.MAIN_ARTIFACT_NAME.equals(classifier) ? "default" : classifier;
            if (descriptor.getConfiguration(conf) == null) {
                descriptor.addConfiguration(new Configuration(conf));
            }
            descriptor.addArtifact(conf, artifact);
            descriptor.addExtraAttributeNamespace(EXTRA_PREFIX, EXTRA_NAMESPACE);
        });
    }

    private static Artifact toMavenArtifact(Path artifactFile, String classifier, ModuleRevisionId moduleId, Instant date) {
        final String extension = JkUtilsString.substringAfterLast(artifactFile.getFileName().toString(), ".");
        final Map<String, String> extraMap;
        if (JkArtifactId.MAIN_ARTIFACT_NAME.equals(classifier)) {
            extraMap = new HashMap<>();
        } else {
            extraMap = JkUtilsIterable.mapOf(EXTRA_PREFIX + ":classifier", classifier);
        }
        return new DefaultArtifact(moduleId, new Date(date.toEpochMilli()), moduleId.getName(), extension, extension,
                extraMap);
    }

    private static Artifact toIvyArtifact(JkIvyPublication.JkPublicationArtifact artifact,
                                  ModuleRevisionId moduleId, Instant date) {
        final String name = JkUtilsString.isBlank(artifact.name) ? moduleId.getName() : artifact.name;
        final String extension = JkUtilsObject.firstNonNull(artifact.extension, "");
        final String type = JkUtilsObject.firstNonNull(artifact.type, extension);
        return new DefaultArtifact(moduleId, new Date(date.toEpochMilli()), name, type, extension);
    }


}
