package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.publication.JkIvyConfigurationMapping;
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

class IvyTranslatorToDependency {

    static List<DefaultDependencyDescriptor> toDependencyDescriptors(JkQualifiedDependencies dependencies) {
        List<JkQualifiedDependency> qualifiedDependencies = dependencies.replaceUnspecifiedVersionsWithProvider()
                .getQualifiedDependencies();
        Map<JkVersionedModule, Set<ClassifierAndType>> moduleClassifierTypesMap =
                mapModuleToClassifierType(qualifiedDependencies);
        List<DefaultDependencyDescriptor> result = new LinkedList<>();
        for (JkQualifiedDependency qualifiedDependency : qualifiedDependencies) {
            JkModuleDependency moduleDependency = (JkModuleDependency) qualifiedDependency.getDependency();
            Set<ClassifierAndType> classifierAndTypes = moduleClassifierTypesMap.get(
                    moduleDependency.toVersionedModule());
            if (classifierAndTypes == null) {
                continue;
            }
            classifierAndTypes.remove(moduleDependency.toVersionedModule());
            result.add(toDependencyDescriptor(qualifiedDependency.getQualifier(), moduleDependency, classifierAndTypes));
        }
        return result;
    }

    static void bind(DefaultModuleDescriptor moduleDescriptor, DependencyDescriptor dependencyDescriptor) {

        // If we don't set parent, force version on resolution won't work
        final Field field = JkUtilsReflect.getField(DefaultDependencyDescriptor.class, "parentId");
        JkUtilsReflect.setFieldValue(dependencyDescriptor, field, moduleDescriptor.getModuleRevisionId());
        moduleDescriptor.addDependency(dependencyDescriptor);
    }

    private static DefaultDependencyDescriptor toDependencyDescriptor(String qualifier,
                                                                      JkModuleDependency moduleDependency,
                                                                      Set<ClassifierAndType> classifierAndTypes) {
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
        for (String masterConf : masterConfs) {
            moduleDependency.getExclusions().forEach(exclusion ->
                    result.addExcludeRule(masterConf, toExcludeRule(exclusion)));
            final Set<String> dependencyConfs =  configurationMapping.getRight().isEmpty() ?
                    Collections.singleton(null) : configurationMapping.getRight();
            for (String dependencyConf : dependencyConfs) {
                Set<String> effectiveDepConfs = dependencyConfs(dependencyConf, moduleDependency.getTransitivity());
                effectiveDepConfs.forEach(depConf -> result.addDependencyConfiguration(masterConf, depConf));
            }
            for (ClassifierAndType classifierAndType : classifierAndTypes) {
                if (classifierAndType.classifier == null && classifierAndType.classifier == null
                        && classifierAndTypes.size() <= 1) {
                    continue;
                }
                result.addDependencyArtifact(masterConf, IvyTranslatorToArtifact.toArtifactDependencyDescriptor(
                        result, classifierAndType.classifier, classifierAndType.type));
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
        return JkIvyConfigurationMapping.of(ivyExpression).getLeft();
    }

    static DefaultExcludeRule toExcludeRule(JkDependencyExclusion depExclude, String... configurationNames) {
        String type = depExclude.getClassifier() == null ? PatternMatcher.ANY_EXPRESSION : depExclude.getClassifier();
        String ext = depExclude.getExtension() == null ? PatternMatcher.ANY_EXPRESSION : depExclude.getExtension();
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

    private static Map<JkVersionedModule, Set<ClassifierAndType>> mapModuleToClassifierType(
            List<JkQualifiedDependency> dependencies) {
        Map<JkVersionedModule, Set<ClassifierAndType>> result = new HashMap<>();
        for (JkQualifiedDependency qualifiedDependency : dependencies) {
            JkModuleDependency moduleDependency = (JkModuleDependency) qualifiedDependency.getDependency();
            JkVersionedModule versionedModule = moduleDependency.toVersionedModule();
            result.putIfAbsent(versionedModule, new HashSet<>());
            result.get(versionedModule).add(new ClassifierAndType(moduleDependency));
        }
        return result;
    }

    private static class ClassifierAndType {

        private String classifier;

        private String type;

        public ClassifierAndType(JkModuleDependency moduleDependency) {
            this.classifier = moduleDependency.getClassifier();
            this.type = moduleDependency.getType();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassifierAndType that = (ClassifierAndType) o;
            if (classifier != null ? !classifier.equals(that.classifier) : that.classifier != null) return false;
            return type != null ? type.equals(that.type) : that.type == null;
        }

        @Override
        public int hashCode() {
            int result = classifier != null ? classifier.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }

}
