package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.depmanagement.publication.JkIvyConfigurationMapping;
import org.apache.ivy.core.module.descriptor.Configuration;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

class IvyTranslatorToConfiguration {

    static final String DEFAULT = "default";

    static final String ALL = "*";

    static Set<Configuration> toMasterConfigurations(JkQualifiedDependencies dependencies) {
        Set<Configuration> result = dependencies.getEntries().stream()
                .map(qDep -> qDep.getQualifier())
                .map(JkIvyConfigurationMapping::of)
                .flatMap(cm -> cm.getLeft().stream())
                .map(confName -> new Configuration(confName))
                .collect(Collectors.toSet());
        return result.isEmpty() ? Collections.singleton(new Configuration(DEFAULT)) : result;

    }
}
