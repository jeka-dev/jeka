package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.publication.JkArtifactPublisher;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsString;
import org.apache.ivy.core.module.descriptor.*;
import org.apache.ivy.core.module.id.ModuleRevisionId;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

class IvyTranslatorToArtifact {

    private static final String EXTRA_NAMESPACE = "http://ant.apache.org/ivy/extra";

    private static final String EXTRA_PREFIX = "e";

    static Map<String, Artifact> toMavenArtifacts(JkCoordinate coordinate, JkArtifactPublisher artifactPublisher) {
        Map<String, Artifact> result = new HashMap<>();
        Instant now = Instant.now();
        for (JkArtifactId artifactId : artifactPublisher.getArtifactIds()) {
            Path file = artifactPublisher.artifactLocator.getArtifactPath(artifactId);
            ModuleRevisionId moduleRevisionId = IvyTranslatorToDependency.toModuleRevisionId(coordinate);
            String classifier = artifactId.getClassifier();
            final Artifact artifact = toMavenArtifact(file, classifier, moduleRevisionId, now);
            result.put(classifier, artifact);
        }
        return result;
    }

    static List<ArtifactAndConfigurations> toIvyArtifacts(JkCoordinate coordinate,
                                                          List<JkIvyPublication.JkIvyPublishedArtifact> jkArtifacts) {
        List<ArtifactAndConfigurations> result = new LinkedList<>();
        Instant now = Instant.now();
        for (JkIvyPublication.JkIvyPublishedArtifact jkArtifact : jkArtifacts) {
            ModuleRevisionId moduleRevisionId = IvyTranslatorToDependency.toModuleRevisionId(coordinate);
            final Artifact artifact = toIvyArtifact(jkArtifact, moduleRevisionId, now);
            result.add(new ArtifactAndConfigurations(artifact, jkArtifact.configurationNames));
        }
        return result;
    }

    static void bind(DefaultModuleDescriptor descriptor, Map<String, Artifact> artifactMap) {
        artifactMap.forEach((classifier, artifact) -> {
            String conf = JkArtifactId.MAIN_ARTIFACT_CLASSIFIER.equals(classifier) ? "default" : classifier;
            if (descriptor.getConfiguration(conf) == null) {
                descriptor.addConfiguration(new Configuration(conf));
            }
            descriptor.addArtifact(conf, artifact);
            descriptor.addExtraAttributeNamespace(EXTRA_PREFIX, EXTRA_NAMESPACE);
        });
    }

    static void bind(DefaultModuleDescriptor descriptor, List<ArtifactAndConfigurations> artifactAndConfigurations) {
        artifactAndConfigurations.forEach(artifactAndConfs -> {
            Set<String> actualConfs = artifactAndConfs.configurations.isEmpty() ? Collections.singleton("default") :
                    artifactAndConfs.configurations;
            for (String actualConf : actualConfs) {
                if (descriptor.getConfiguration(actualConf) == null) {
                    descriptor.addConfiguration(new Configuration(actualConf));
                }
                descriptor.addArtifact(actualConf, artifactAndConfs.artifact);
            }
        });
    }

    static DependencyArtifactDescriptor toArtifactDependencyDescriptor(DependencyDescriptor dependencyDescriptor,
                                                                       String classifier, String type) {
        String name = dependencyDescriptor.getDependencyId().getName();
        String effectiveType = JkUtilsObject.firstNonNull(type, "jar");
        Map<String, String> extraAttributes = JkUtilsString.isBlank(classifier) ? Collections.emptyMap() :
            JkUtilsIterable.mapOf(EXTRA_PREFIX + ":classifier", classifier);
        return new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, effectiveType, effectiveType,
                null, extraAttributes);

    }

    private static Artifact toMavenArtifact(Path artifactFile, String classifier, ModuleRevisionId moduleId, Instant date) {
        final String extension = JkUtilsString.substringAfterLast(artifactFile.getFileName().toString(), ".");
        final Map<String, String> extraAttribute;
        if (JkArtifactId.MAIN_ARTIFACT_CLASSIFIER.equals(classifier)) {
            extraAttribute = new HashMap<>();
        } else {
            extraAttribute = JkUtilsIterable.mapOf(EXTRA_PREFIX + ":classifier", classifier);
        }
        return new DefaultArtifact(moduleId, new Date(date.toEpochMilli()), moduleId.getName(), extension, extension,
                extraAttribute);
    }

    static Artifact toIvyArtifact(JkIvyPublication.JkIvyPublishedArtifact artifact,
                                  ModuleRevisionId moduleId, Instant date) {
        final String name = JkUtilsString.isBlank(artifact.name) ? moduleId.getOrganisation() + "."
                + moduleId.getName() : artifact.name;
        final String extension = JkUtilsObject.firstNonNull(artifact.extension,
                JkPathFile.of(artifact.file).getExtension(), "");
        final String type = JkUtilsObject.firstNonNull(artifact.type, extension);
        return new DefaultArtifact(moduleId, new Date(date.toEpochMilli()), name, type, extension);
    }


    static class ArtifactAndConfigurations {

        final Set<String> configurations;

        final Artifact artifact;

        public ArtifactAndConfigurations(Artifact artifact, Set<String> configurations) {
            this.configurations = configurations;
            this.artifact = artifact;
        }
    }


}
