package dev.jeka.core.api.depmanagement.embedded.ivy;

import dev.jeka.core.api.depmanagement.JkQualifiedDependencySet;
import org.apache.ivy.core.module.descriptor.Configuration;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

class IvyTranslatorToConfiguration {

    static final String DEFAULT = "default";

    static Set<Configuration> toMasterConfigurations(JkQualifiedDependencySet dependencies) {
        Set<Configuration> result = dependencies.getEntries().stream()
                .map(qDep -> qDep.getQualifier())
                .flatMap(qualifier -> IvyConfigurationMapping.ofMultiple(qualifier).stream())
                .flatMap(cm -> cm.getLeft().stream())
                .map(confName -> new Configuration(confName))
                .collect(Collectors.toSet());
        return result.isEmpty() ? Collections.singleton(new Configuration(DEFAULT)) : result;

    }
}
