package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.publication.JkIvyConfigurationMapping;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import org.apache.ivy.core.module.descriptor.*;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class IvyTranslatorToDependency {

    static List<DefaultDependencyDescriptor> toDependencyDescriptors(JkQualifiedDependencies dependencies) {
        return dependencies.replaceUnspecifiedVersionsWithProvider().getQualifiedDependencies().stream()
                .map(qDep -> toDependencyDescriptor(qDep.getQualifier(), (JkModuleDependency) qDep.getDependency()))
                .collect(Collectors.toList());
    }

    static void bind(DefaultModuleDescriptor moduleDescriptor, DependencyDescriptor dependencyDescriptor) {

        // If we don't set parent, force version on resolution won't work
        final Field field = JkUtilsReflect.getField(DefaultDependencyDescriptor.class, "parentId");
        JkUtilsReflect.setFieldValue(dependencyDescriptor, field, moduleDescriptor.getModuleRevisionId());
        moduleDescriptor.addDependency(dependencyDescriptor);
    }

    private static DefaultDependencyDescriptor toDependencyDescriptor(String qualifier, JkModuleDependency moduleDependency) {
        JkIvyConfigurationMapping configurationMapping = JkIvyConfigurationMapping.of(qualifier);
        JkVersion version = moduleDependency.getVersion();
        ModuleRevisionId moduleRevisionId = ModuleRevisionId.newInstance(
                moduleDependency.getModuleId().getGroup(), moduleDependency.getModuleId().getName(),
                version.getValue());
        boolean changing = version.isDynamic() || version.isSnapshot();
        boolean isTransitive = moduleDependency.getTransitivity() != JkTransitivity.NONE;
        final boolean force = !version.isDynamic();
        DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(null, moduleRevisionId, force, changing,
                isTransitive);
        final Set<String> masterConfs =  configurationMapping.getLeft().isEmpty() ?
                Collections.singleton(IvyTranslatorToConfiguration.DEFAULT) : configurationMapping.getLeft();
        String classifier = moduleDependency.getClassifier();
        for (String masterConf : masterConfs) {
            moduleDependency.getExclusions().forEach(exclusion ->
                    result.addExcludeRule(masterConf, toExcludeRule(exclusion)));
            final Set<String> dependencyConfs =  configurationMapping.getRight().isEmpty() ?
                    Collections.singleton(null) : configurationMapping.getRight();
            for (String dependencyConf : dependencyConfs) {
                String effectiveConf = dependencyConfs(dependencyConf, moduleDependency.getTransitivity());
                result.addDependencyConfiguration(masterConf, effectiveConf);
            }
            if (!JkUtilsString.isBlank(classifier)) {
                result.addDependencyArtifact(masterConf, toDependencyArtifact(result, classifier,
                        moduleDependency.getType()));
            }
        }
        return result;
    }

    private static String dependencyConfs(String dependencyConf, JkTransitivity transitivity) {
        if (dependencyConf != null) {
            return dependencyConf;
        }
        if (transitivity == null) {
            JkQualifiedDependencies.getIvyTargetConfigurations(JkTransitivity.RUNTIME);
        }
        return JkQualifiedDependencies.getIvyTargetConfigurations(transitivity);
    }

    static DefaultExcludeRule toExcludeRule(JkDependencyExclusion depExclude) {
        String type = depExclude.getClassifier() == null ? PatternMatcher.ANY_EXPRESSION : depExclude.getClassifier();
        String ext = depExclude.getExtension() == null ? PatternMatcher.ANY_EXPRESSION : depExclude.getExtension();
        ModuleId moduleId = toModuleId(depExclude.getModuleId());
        ArtifactId artifactId = new ArtifactId(moduleId, "*", type, ext);
        return new DefaultExcludeRule(artifactId, ExactPatternMatcher.INSTANCE, null);
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

    private static DependencyArtifactDescriptor toDependencyArtifact(DependencyDescriptor dependencyDescriptor,
                                                                     String classifier, String type) {
        String name = classifier;
        String artifactType = JkUtilsObject.firstNonNull(type, "jar");
        return new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, artifactType, artifactType,
                null, null);
    }



}
