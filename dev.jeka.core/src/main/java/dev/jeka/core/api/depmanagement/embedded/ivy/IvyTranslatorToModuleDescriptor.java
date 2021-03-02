package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
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

    static DefaultModuleDescriptor toResolveModuleDescriptor(JkVersionedModule module,
                                                             JkQualifiedDependencies dependencies) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module
                .getModuleId().getGroup(), module.getModuleId().getName(), module.getVersion().getValue());
        final DefaultModuleDescriptor result = newDefaultModuleDescriptor(thisModuleRevisionId);

        // Add configurations
        toMasterConfigurations(dependencies).forEach(conf -> result.addConfiguration(conf));

        // Add dependencies
        toDependencyDescriptors(dependencies).forEach(dep -> IvyTranslatorToDependency.bind(result, dep));

        // Add global exclusions
        dependencies.getGlobalExclusions().stream().map(exclusion -> toExcludeRule(exclusion,
                result.getConfigurationsNames()))
                .forEach(excludeRule -> result.addExcludeRule(excludeRule));

        // Add version overwrites for transitive dependencies
        JkVersionProvider versionProvider = dependencies.getVersionProvider();
        for (final JkModuleId moduleId : versionProvider.getModuleIds()) {
            final JkVersion version = versionProvider.getVersionOf(moduleId);
            result.addDependencyDescriptorMediator(toModuleId(moduleId),
                    ExactOrRegexpPatternMatcher.INSTANCE,
                    new OverrideDependencyDescriptorMediator(null, version.getValue()));
        }
        return result;
    }

    static DefaultModuleDescriptor toMavenPublishModuleDescriptor(JkVersionedModule module,
                                                                  JkDependencySet dependencies,
                                                                  JkMavenPublication mavenPublication) {
        List<JkQualifiedDependency> qualifiedDependencies = dependencies.getEntries().stream()
                .filter(JkModuleDependency.class::isInstance)
                .map(JkModuleDependency.class::cast)
                .map(dep -> {
                    JkTransitivity transitivity = dep.getTransitivity();
                    String qualifier = JkTransitivity.COMPILE.equals(transitivity) ? "compile" : "runtime";
                    return JkQualifiedDependency.of(qualifier, dep);
                })
                .collect(Collectors.toList());
        DefaultModuleDescriptor result = toResolveModuleDescriptor(module,
                JkQualifiedDependencies.of(qualifiedDependencies));
        Map<String, Artifact> artifactMap = IvyTranslatorToArtifact.toMavenArtifacts(module,
                mavenPublication.getArtifactLocator());
        IvyTranslatorToArtifact.bind(result, artifactMap);
        return result;
    }

    static DefaultModuleDescriptor toIvyPublishModuleDescriptor(JkVersionedModule module,
                                                                JkQualifiedDependencies dependencies,
                                                                JkIvyPublication ivyPublication) {
        DefaultModuleDescriptor result = toResolveModuleDescriptor(module, dependencies);
        List<IvyTranslatorToArtifact.ArtifactAndConfigurations> artifactAndConfigurationsList =
            IvyTranslatorToArtifact.toIvyArtifacts(module, ivyPublication.getAllArtifacts());
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
