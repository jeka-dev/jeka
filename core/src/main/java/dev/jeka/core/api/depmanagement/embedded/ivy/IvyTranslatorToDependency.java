/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class IvyTranslatorToDependency {

    static List<DefaultDependencyDescriptor> toDependencyDescriptors(JkQualifiedDependencySet dependencies) {
        return dependencies
                .getEntries().stream()
                    .map(qDep -> toDependencyDescriptor(qDep.getQualifier(), (JkCoordinateDependency) qDep.getDependency()))
                    .collect(Collectors.toList());
    }

    static void bind(DefaultModuleDescriptor moduleDescriptor, DependencyDescriptor dependencyDescriptor) {

        // If we don't set parent, force version on resolution won't work
        final Field field = JkUtilsReflect.getField(DefaultDependencyDescriptor.class, "parentId");
        JkUtilsReflect.setFieldValue(dependencyDescriptor, field, moduleDescriptor.getModuleRevisionId());
        DefaultDependencyDescriptor existingDep = (DefaultDependencyDescriptor) findDependency(moduleDescriptor, dependencyDescriptor);
        if (existingDep == null) {
            moduleDescriptor.addDependency(dependencyDescriptor);
        } else {
            Arrays.stream(moduleDescriptor.getConfigurationsNames()).forEach(confName ->
            existingDep.addDependencyArtifact(confName, dependencyDescriptor.getAllDependencyArtifacts()[0]));
        }
    }

    private static DependencyDescriptor findDependency(DefaultModuleDescriptor moduleDescriptor,
                                                       DependencyDescriptor dependencyDescriptor) {
        return Arrays.stream(moduleDescriptor.getDependencies())
                .filter(dep -> dep.getDependencyRevisionId().getModuleId().equals(
                        dependencyDescriptor.getDependencyRevisionId().getModuleId()))
                .findFirst().orElse(null);
    }

    private static DefaultDependencyDescriptor toDependencyDescriptor(String qualifier,
                                                                      JkCoordinateDependency coordinateDependency) {

        JkCoordinate coordinate = coordinateDependency.getCoordinate();
        JkVersion version = coordinate.getVersion();
        ModuleRevisionId moduleRevisionId = ModuleRevisionId.newInstance(
                coordinate.getModuleId().getGroup(), coordinate.getModuleId().getName(),
                version.getValue());
        boolean changing = version.isDynamic() || version.isSnapshot();
        JkTransitivity declaredTransitivity = coordinateDependency.getTransitivity();
        final boolean isTransitive;
        if (declaredTransitivity != null) {
            isTransitive  = coordinateDependency.getTransitivity() != JkTransitivity.NONE;
        } else {

            // If transitivity is not explicit then use transitivity only on default artifact
            isTransitive = JkCoordinate.JkArtifactSpecification.MAIN.equals(coordinate.getArtifactSpecification());
        }

        final boolean force = !version.isDynamic();
        DefaultDependencyDescriptor result = new DefaultDependencyDescriptor(null, moduleRevisionId, force, changing,
                isTransitive);
        List<IvyConfigurationMapping> configurationMappings = IvyConfigurationMapping.ofMultiple(qualifier);
       for (IvyConfigurationMapping configurationMapping : configurationMappings) {
        //IvyConfigurationMapping configurationMapping = IvyConfigurationMapping.of(qualifier);
           final Set<String> masterConfigurations = configurationMapping.getLeft().isEmpty() ?
                   Collections.singleton(IvyTranslatorToConfiguration.DEFAULT) : configurationMapping.getLeft();
           for (String masterConfiguration : masterConfigurations) {
               coordinateDependency.getExclusions().forEach(exclusion ->
                       result.addExcludeRule(masterConfiguration, toExcludeRule(exclusion)));
               final Set<String> dependencyConfigurations = configurationMapping.getRight().isEmpty() ?
                       Collections.singleton(null) : configurationMapping.getRight();
               for (String dependencyConfiguration : dependencyConfigurations) {
                   Set<String> effectiveDepConfs = dependencyConfs(dependencyConfiguration, coordinateDependency.getTransitivity());
                   effectiveDepConfs.forEach(depConf -> result.addDependencyConfiguration(masterConfiguration, depConf));
               }
               JkCoordinate.JkArtifactSpecification artifactSpecification = coordinate.getArtifactSpecification();
               result.addDependencyArtifact(masterConfiguration, IvyTranslatorToArtifact.toArtifactDependencyDescriptor(
                       result, artifactSpecification.getClassifier(), artifactSpecification.getType()));
           }
       }
        return result;
    }

    private static Set<String> dependencyConfs(String dependencyConf, JkTransitivity transitivity) {
        if (dependencyConf != null) {
            return Collections.singleton(dependencyConf);
        }
        JkTransitivity effectiveTransitivity = JkUtilsObject.firstNonNull(transitivity, JkTransitivity.RUNTIME);
        String ivyExpression = JkQualifiedDependencySet.getIvyTargetConfigurations(effectiveTransitivity);
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

    static ModuleId toModuleId(JkModuleId jkModuleId) {
        return new ModuleId(jkModuleId.getGroup(), jkModuleId.getName());
    }

    static ModuleRevisionId toModuleRevisionId(JkCoordinate coordinate) {
        return new ModuleRevisionId(toModuleId(coordinate.getModuleId()), coordinate
                .getVersion().getValue());
    }

    static JkCoordinate toJkCoordinate(ModuleRevisionId moduleRevisionId) {
        return JkModuleId.of(moduleRevisionId.getOrganisation(), moduleRevisionId.getName())
                        .toCoordinate(moduleRevisionId.getRevision());
    }

}
