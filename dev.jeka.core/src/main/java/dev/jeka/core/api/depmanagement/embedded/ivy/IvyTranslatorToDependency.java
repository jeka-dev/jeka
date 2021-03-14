package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsReflect;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

class IvyTranslatorToDependency {

    static List<DefaultDependencyDescriptor> toDependencyDescriptors(JkQualifiedDependencies dependencies) {
        return dependencies.replaceUnspecifiedVersionsWithProvider().getEntries().stream()
                .map(qDep -> toDependencyDescriptor(qDep.getQualifier(), (JkModuleDependency) qDep.getDependency()))
                .collect(Collectors.toList());
    }

    static void bind(DefaultModuleDescriptor moduleDescriptor, DependencyDescriptor dependencyDescriptor) {

        // If we don't set parent, force version on resolution won't work
        final Field field = JkUtilsReflect.getField(DefaultDependencyDescriptor.class, "parentId");
        JkUtilsReflect.setFieldValue(dependencyDescriptor, field, moduleDescriptor.getModuleRevisionId());
        moduleDescriptor.addDependency(dependencyDescriptor);
    }

    private static DefaultDependencyDescriptor toDependencyDescriptor(String qualifier,
                                                                      JkModuleDependency moduleDependency) {

        JkVersion version = moduleDependency.getVersion();
        ModuleRevisionId moduleRevisionId = ModuleRevisionId.newInstance(
                moduleDependency.getModuleId().getGroup(), moduleDependency.getModuleId().getName(),
                version.getValue());
        boolean changing = version.isDynamic() || version.isSnapshot();
        boolean isTransitive = moduleDependency.getTransitivity() != JkTransitivity.NONE;
        final boolean force = !version.isDynamic();
        DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(null, moduleRevisionId, force, changing,
                isTransitive);
        List<IvyConfigurationMapping> configurationMappings = IvyConfigurationMapping.ofMultiple(qualifier);
       for (IvyConfigurationMapping configurationMapping : configurationMappings) {
        //IvyConfigurationMapping configurationMapping = IvyConfigurationMapping.of(qualifier);
           final Set<String> masterConfigurations = configurationMapping.getLeft().isEmpty() ?
                   Collections.singleton(IvyTranslatorToConfiguration.DEFAULT) : configurationMapping.getLeft();
           for (String masterConfiguration : masterConfigurations) {
               moduleDependency.getExclusions().forEach(exclusion ->
                       result.addExcludeRule(masterConfiguration, toExcludeRule(exclusion)));
               final Set<String> dependencyConfigurations = configurationMapping.getRight().isEmpty() ?
                       Collections.singleton(null) : configurationMapping.getRight();
               for (String dependencyConfiguration : dependencyConfigurations) {
                   Set<String> effectiveDepConfs = dependencyConfs(dependencyConfiguration, moduleDependency.getTransitivity());
                   effectiveDepConfs.forEach(depConf -> result.addDependencyConfiguration(masterConfiguration, depConf));
               }
               for (JkModuleDependency.JkArtifactSpecification artifactSpecification : moduleDependency.getArtifactSpecifications()) {
                   result.addDependencyArtifact(masterConfiguration, IvyTranslatorToArtifact.toArtifactDependencyDescriptor(
                           result, artifactSpecification.getClassifier(), artifactSpecification.getType()));
               }
           }
       }
        return result;
    }

    private static Set<String> dependencyConfs(String dependencyConf, JkTransitivity transitivity) {
        if (dependencyConf != null) {
            return Collections.singleton(dependencyConf);
        }
        JkTransitivity effectiveTransitivity = JkUtilsObject.firstNonNull(transitivity, JkTransitivity.RUNTIME);
        String ivyExpression = JkQualifiedDependencies.getIvyTargetConfigurations(effectiveTransitivity);
        return IvyConfigurationMapping.of(ivyExpression).getLeft();
    }

    static DefaultExcludeRule toExcludeRule(JkDependencyExclusion depExclude, String... configurationNames) {
        String type = depExclude.getClassifier() == null ? PatternMatcher.ANY_EXPRESSION : depExclude.getClassifier();
        String ext = depExclude.getType() == null ? PatternMatcher.ANY_EXPRESSION : depExclude.getType();
        ModuleId moduleId = toModuleId(depExclude.getModuleId());
        ArtifactId artifactId = new ArtifactId(moduleId, "*", type, ext);
        DefaultExcludeRule result = new DefaultExcludeRule(artifactId, ExactPatternMatcher.INSTANCE, null);
        Arrays.stream(configurationNames).forEach(name -> result.addConfiguration(name));
        return result;
    }

    static ModuleId toModuleId(JkModuleId moduleId) {
        return new ModuleId(moduleId.getGroup(), moduleId.getName());
    }

    static ModuleRevisionId toModuleRevisionId(JkVersionedModule jkVersionedModule) {
        return new ModuleRevisionId(toModuleId(jkVersionedModule.getModuleId()), jkVersionedModule
                .getVersion().getValue());
    }

    static JkVersionedModule toJkVersionedModule(ModuleRevisionId moduleRevisionId) {
        return JkVersionedModule.of(
                JkModuleId.of(moduleRevisionId.getOrganisation(), moduleRevisionId.getName()),
                JkVersion.of(moduleRevisionId.getRevision()));
    }

}
