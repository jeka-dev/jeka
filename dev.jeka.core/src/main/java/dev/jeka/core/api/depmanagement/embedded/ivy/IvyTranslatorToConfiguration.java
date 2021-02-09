package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.depmanagement.publication.JkIvyConfigurationMapping;
import dev.jeka.core.api.depmanagement.publication.JkIvyConfigurationMappingSet;
import org.apache.ivy.core.module.descriptor.Configuration;

import java.util.Set;
import java.util.stream.Collectors;

class IvyTranslatorToConfiguration {

    private final JkIvyConfigurationMappingSet ivyConfigurationMappingSet;

    private IvyTranslatorToConfiguration(JkIvyConfigurationMappingSet ivyConfigurationMappingSet) {
        this.ivyConfigurationMappingSet = ivyConfigurationMappingSet;
    }

    static Set<String> toConfigurationToDeclare(JkQualifiedDependencies dependencies) {
        return dependencies.getQualifiedDependencies().stream()
                .map(qDep -> qDep.getQualifier())
                .map(JkIvyConfigurationMapping::of)
                .flatMap(cm -> cm.getLeft().stream())
                .collect(Collectors.toSet());
    }

    static Configuration toSimpleConfiguration(String configurationName) {
        return new Configuration(configurationName);
    }
}
