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
import dev.jeka.core.api.depmanagement.publication.JkArtifactPublisher;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
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
            if (jkModuleId.toColonNotation().contains("${")) {
                throw new IllegalStateException("Invalid module id " + jkModuleId.toColonNotation());
            }

            final JkVersion version = versionProvider.getVersionOf(jkModuleId);
            result.addDependencyDescriptorMediator(toModuleId(jkModuleId),
                    ExactOrRegexpPatternMatcher.INSTANCE,
                    new FallbackVersionMediator(version.getValue()));

        }
        return result;
    }

    static DefaultModuleDescriptor toMavenPublishModuleDescriptor(JkCoordinate coordinate,
                                                                  JkDependencySet dependencies,
                                                                  JkArtifactPublisher artifactPublisher) {
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
        Map<String, Artifact> artifactMap = IvyTranslatorToArtifact.toMavenArtifacts(coordinate, artifactPublisher);
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

    private static class FallbackVersionMediator extends OverrideDependencyDescriptorMediator{

        public FallbackVersionMediator(String version) {
            super(null, version);
        }

        public DependencyDescriptor mediate(DependencyDescriptor dd) {
            ModuleRevisionId mrid = dd.getDependencyRevisionId();
            String fallbackVersion = getVersion();

            if (fallbackVersion == null || fallbackVersion.equals(mrid.getRevision()) || mrid.getRevision() != null) {
                return dd;
            }

            return dd.clone(ModuleRevisionId.newInstance(mrid.getOrganisation(), mrid.getName(),
                    null, fallbackVersion, mrid.getQualifiedExtraAttributes()));
        }
    }


}
