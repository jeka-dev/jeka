package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.OverrideDependencyDescriptorMediator;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.conflict.AbstractConflictManager;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;

import static dev.jeka.core.api.depmanagement.embedded.ivy.IvyTranslatorToConfiguration.toConfigurationToDeclare;
import static dev.jeka.core.api.depmanagement.embedded.ivy.IvyTranslatorToConflictManager.bind;
import static dev.jeka.core.api.depmanagement.embedded.ivy.IvyTranslatorToConflictManager.toConflictManager;
import static dev.jeka.core.api.depmanagement.embedded.ivy.IvyTranslatorToDependency.*;

class IvyTranslatorToModuleDescriptor {

    static DefaultModuleDescriptor toResolutionModuleDescriptor(JkVersionedModule module,
                                                                JkQualifiedDependencies dependencies,
                                                                JkResolutionParameters resolutionParameters,
                                                                IvySettings ivySettings) {
        final ModuleRevisionId thisModuleRevisionId = ModuleRevisionId.newInstance(module
                .getModuleId().getGroup(), module.getModuleId().getName(), module.getVersion().getValue());
        final DefaultModuleDescriptor result = new DefaultModuleDescriptor(
                thisModuleRevisionId, "integration", null);

        // Add conflict manager if needed
        AbstractConflictManager conflictManager = toConflictManager(resolutionParameters.getConflictResolver());
        if (conflictManager != null) {
            bind(result, conflictManager, ivySettings);
        }

        // Add configurations
        toConfigurationToDeclare(dependencies).stream()
                .map(IvyTranslatorToConfiguration::toSimpleConfiguration)
                .forEach(conf -> result.addConfiguration(conf));

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


}
