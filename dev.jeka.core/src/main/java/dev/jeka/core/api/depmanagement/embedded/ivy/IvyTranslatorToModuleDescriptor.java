package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.publication.JkArtifactPublisher;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.OverrideDependencyDescriptorMediator;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.jeka.core.api.depmanagement.embedded.ivy.IvyTranslatorToConfiguration.toMasterConfigurations;
import static dev.jeka.core.api.depmanagement.embedded.ivy.IvyTranslatorToDependency.*;

class IvyTranslatorToModuleDescriptor {

    static DefaultModuleDescriptor toResolveModuleDescriptor(JkCoordinate coordinate,
                                                             JkQualifiedDependencySet dependencies) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(
                coordinate.getModuleId().getGroup(),
                coordinate.getModuleId().getName(),
                coordinate.getVersion().getValue());
        final DefaultModuleDescriptor result = newDefaultModuleDescriptor(thisModuleRevisionId);

        // Add configurations
        toMasterConfigurations(dependencies).forEach(configuration -> result.addConfiguration(configuration));

        // Add dependencies
        toDependencyDescriptors(dependencies).forEach(dep -> IvyTranslatorToDependency.bind(result, dep));

        // Add global exclusions
        dependencies.getGlobalExclusions().stream().map(exclusion -> toExcludeRule(exclusion,
                result.getConfigurationsNames()))
                .forEach(excludeRule -> result.addExcludeRule(excludeRule));

        // Add version overwrites for transitive dependencies
        JkVersionProvider versionProvider = dependencies.getVersionProvider();
        for (final JkModuleId jkModuleId : versionProvider.getModuleIds()) {
            final JkVersion version = versionProvider.getVersionOf(jkModuleId);
            result.addDependencyDescriptorMediator(toModuleId(jkModuleId),
                    ExactOrRegexpPatternMatcher.INSTANCE,
                    new OverrideDependencyDescriptorMediator(null, version.getValue()));
        }
        return result;
    }

    static DefaultModuleDescriptor toMavenPublishModuleDescriptor(JkCoordinate coordinate,
                                                                  JkDependencySet dependencies,
                                                                  JkArtifactPublisher artifactProducer) {
        List<JkQualifiedDependency> qualifiedDependencies = dependencies.getEntries().stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .map(JkCoordinateDependency.class::cast)
                .map(dep -> {
                    JkTransitivity transitivity = dep.getTransitivity();
                    String qualifier = JkTransitivity.COMPILE.equals(transitivity) ? "compile" : "runtime";
                    return JkQualifiedDependency.of(qualifier, dep);
                })
                .collect(Collectors.toList());
        DefaultModuleDescriptor result = toResolveModuleDescriptor(coordinate,
                JkQualifiedDependencySet.of(qualifiedDependencies));
        Map<String, Artifact> artifactMap = IvyTranslatorToArtifact.toMavenArtifacts(coordinate, artifactProducer);
        IvyTranslatorToArtifact.bind(result, artifactMap);
        return result;
    }

    static DefaultModuleDescriptor toIvyPublishModuleDescriptor(JkCoordinate coordinate,
                                                                JkQualifiedDependencySet dependencies,
                                                                List<JkIvyPublication.JkIvyPublishedArtifact> publishedArtifacts) {
        DefaultModuleDescriptor result = toResolveModuleDescriptor(coordinate, dependencies);
        List<IvyTranslatorToArtifact.ArtifactAndConfigurations> artifactAndConfigurationsList =
            IvyTranslatorToArtifact.toIvyArtifacts(coordinate, publishedArtifacts);
        IvyTranslatorToArtifact.bind(result, artifactAndConfigurationsList);
        return result;
    }

    private static DefaultModuleDescriptor newDefaultModuleDescriptor(ModuleRevisionId moduleRevisionId) {
        JkLog.Verbosity verbosity = JkLog.verbosity();
        if (!JkLog.isVerbose()) {  // Avoid internal ivy system.output emitted on DefaultModuleDescriptor constructor
            PrintStream previous = System.out;
            System.setOut(JkUtilsIO.nopPrintStream());
            try {
                return new DefaultModuleDescriptor(moduleRevisionId, "integration", null);
            } finally {
                System.setOut(previous);
                JkLog.setVerbosity(verbosity);
            }
        } else {
            return new DefaultModuleDescriptor(moduleRevisionId, "integration", null);
        }
    }


}
