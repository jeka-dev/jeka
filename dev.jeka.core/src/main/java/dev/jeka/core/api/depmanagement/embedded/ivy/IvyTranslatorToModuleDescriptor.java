package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.OverrideDependencyDescriptorMediator;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;

import java.util.List;
import java.util.Map;

import static dev.jeka.core.api.depmanagement.embedded.ivy.IvyTranslatorToConfiguration.toMasterConfigurations;
import static dev.jeka.core.api.depmanagement.embedded.ivy.IvyTranslatorToDependency.*;

class IvyTranslatorToModuleDescriptor {

    static DefaultModuleDescriptor toResolveModuleDescriptor(JkVersionedModule module,
                                                             JkQualifiedDependencies dependencies) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module
                .getModuleId().getGroup(), module.getModuleId().getName(), module.getVersion().getValue());
        final DefaultModuleDescriptor result = new DefaultModuleDescriptor(
                thisModuleRevisionId, "integration", null);

        // Add configurations
        toMasterConfigurations(dependencies).forEach(conf -> result.addConfiguration(conf));

        // Add dependencies
        toDependencyDescriptors(dependencies).forEach(dep -> IvyTranslatorToDependency.bind(result, dep));

        // Add global exclusions
        dependencies.getGlobalExclusions().stream().map(exclusion -> toExcludeRule(exclusion))
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

    static DefaultModuleDescriptor toPublishModuleDescriptor(JkVersionedModule module,
                                                                JkQualifiedDependencies dependencies,
                                                                JkMavenPublication mavenPublication) {
        DefaultModuleDescriptor result = toResolveModuleDescriptor(module, dependencies);
        Map<String, Artifact> artifactMap = IvyTranslatorToArtifact.toMavenArtifacts(module,
                mavenPublication.getArtifactLocator());
        IvyTranslatorToArtifact.bind(result, artifactMap);
        return result;
    }

    static DefaultModuleDescriptor toPublishModuleDescriptor(JkVersionedModule module,
                                                                 JkQualifiedDependencies dependencies,
                                                                 JkIvyPublication ivyPublication) {
        DefaultModuleDescriptor result = toResolveModuleDescriptor(module, dependencies);
        List<IvyTranslatorToArtifact.ArtifactAndConfigurations> artifactAndConfigurationsList =
            IvyTranslatorToArtifact.toIvyArtifacts(module, ivyPublication.getAllArtifacts());
        IvyTranslatorToArtifact.bind(result, artifactAndConfigurationsList);
        return result;
    }


}
