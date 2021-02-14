package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
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

    static List<ArtifactAndConfigurations> toIvyArtifacts(JkVersionedModule versionedModule,
                                                List<JkIvyPublication.JkPublicationArtifact> jkArtifacts) {
        List<ArtifactAndConfigurations> result = new LinkedList<>();
        Instant now = Instant.now();
        for (JkIvyPublication.JkPublicationArtifact jkArtifact : jkArtifacts) {
            ModuleRevisionId moduleRevisionId = IvyTranslatorToDependency.toModuleRevisionId(versionedModule);
            final Artifact artifact = toIvyArtifact(jkArtifact, moduleRevisionId, now);
            result.add(new ArtifactAndConfigurations(artifact, jkArtifact.configurationNames));
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
        String artifactType = JkUtilsObject.firstNonNull(type, "jar");
        Map<String, String> extraAttribute = JkUtilsIterable.mapOf(EXTRA_PREFIX + ":classifier", classifier);
        return new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, artifactType, artifactType,
                null, extraAttribute);

    }

    private static Artifact toMavenArtifact(Path artifactFile, String classifier, ModuleRevisionId moduleId, Instant date) {
        final String extension = JkUtilsString.substringAfterLast(artifactFile.getFileName().toString(), ".");
        final Map<String, String> extraAttribute;
        if (JkArtifactId.MAIN_ARTIFACT_NAME.equals(classifier)) {
            extraAttribute = new HashMap<>();
        } else {
            extraAttribute = JkUtilsIterable.mapOf(EXTRA_PREFIX + ":classifier", classifier);
        }
        return new DefaultArtifact(moduleId, new Date(date.toEpochMilli()), moduleId.getName(), extension, extension,
                extraAttribute);
    }

    static Artifact toIvyArtifact(JkIvyPublication.JkPublicationArtifact artifact,
                                  ModuleRevisionId moduleId, Instant date) {
        final String name = JkUtilsString.isBlank(artifact.name) ? moduleId.getName() : artifact.name;
        final String extension = JkUtilsObject.firstNonNull(artifact.extension, "");
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
