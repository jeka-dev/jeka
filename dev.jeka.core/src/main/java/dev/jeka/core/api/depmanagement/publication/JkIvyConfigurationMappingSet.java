package dev.jeka.core.api.depmanagement.publication;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A Set of configuration mapping that help to create default configuratioins
 */
public final class JkIvyConfigurationMappingSet {

    /**
     * Configuratioin mapping used by default.
     */
    public static final JkIvyConfigurationMappingSet RESOLVE_MAPPING = JkIvyConfigurationMappingSet.of()
            .add("compile", "archives(master), compile(default)" )
            .add("runtime", "archives(master), runtime(default");


    private final List<JkIvyConfigurationMapping> configurationMappings;

    // -------- Factory methods ----------------------------

    private JkIvyConfigurationMappingSet(List<JkIvyConfigurationMapping> entries) {
        super();
        this.configurationMappings = entries;
    }

    /**
     * Creates an empty configuration mapping.
     */
    @SuppressWarnings("unchecked")
    public static JkIvyConfigurationMappingSet of() {
        return new JkIvyConfigurationMappingSet(Collections.emptyList());
    }

    // ---------------- Instance members ---------------------------

    public JkIvyConfigurationMappingSet minus(String ... froms) {
        List<JkIvyConfigurationMapping> entries = this.configurationMappings.stream()
                .filter(entry -> !entry.hasFromEqualsTo(froms))
                .collect(Collectors.toList());
        return new JkIvyConfigurationMappingSet(Collections.unmodifiableList(entries));
    }

    public JkIvyConfigurationMappingSet add(JkIvyConfigurationMapping entry) {
            List<JkIvyConfigurationMapping> newEntries = new LinkedList();
            newEntries.add(entry);
        return new JkIvyConfigurationMappingSet(configurationMappings);
    }

    public JkIvyConfigurationMappingSet add(String comaSeparatedFrom, String comaSeparatedTo) {
        Set<String> froms = Arrays.stream(comaSeparatedFrom.split(",")).map(String::trim).collect(Collectors.toSet());
        Set<String> toes = Arrays.stream(comaSeparatedTo.split(",")).map(String::trim).collect(Collectors.toSet());
        return add(JkIvyConfigurationMapping.of(froms, toes));
    }


    /**
     * Returns all configurations declared on the left side.
     */
    public List<JkIvyConfigurationMapping> getConfigurationMappings() {
        return configurationMappings;
    }

    public String toIvyExpression() {
        List<String> items = configurationMappings.stream().map(JkIvyConfigurationMapping::toIvyExpression)
                .collect(Collectors.toList());
        return String.join("; ", items);
    }

    @Override
    public String toString() {
        return configurationMappings.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkIvyConfigurationMappingSet that = (JkIvyConfigurationMappingSet) o;
        return configurationMappings.equals(that.configurationMappings);
    }

    @Override
    public int hashCode() {
        return configurationMappings.hashCode();
    }



}