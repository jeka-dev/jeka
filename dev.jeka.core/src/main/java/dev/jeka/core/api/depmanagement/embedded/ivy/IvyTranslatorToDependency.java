package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.publication.JkIvyConfigurationMapping;
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
import java.util.List;
import java.util.stream.Collectors;

class IvyTranslatorToDependency {

    static List<DefaultDependencyDescriptor> toDependencyDescriptors(JkQualifiedDependencies dependencies) {
        return dependencies.getQualifiedDependencies().stream()
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
        DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(moduleRevisionId, false, changing);
        String masterConfs = configurationMapping.getLeftAsIvYExpression();
        moduleDependency.getExclusioins().forEach(exclusion ->
                result.addExcludeRule(masterConfs, toExcludeRule(exclusion)));
        String dependencyConfs = dependencyConfs(configurationMapping, moduleDependency.getTransitivity());
        result.addDependencyConfiguration(masterConfs, dependencyConfs);
        return result;
    }

    private static String dependencyConfs(JkIvyConfigurationMapping confMapping, JkTransitivity transitivity) {
        if (!confMapping.getRight().isEmpty()) {
            return confMapping.getRightAsIvYExpression();
        }
        if (transitivity == null) {
            return "*";
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


}
