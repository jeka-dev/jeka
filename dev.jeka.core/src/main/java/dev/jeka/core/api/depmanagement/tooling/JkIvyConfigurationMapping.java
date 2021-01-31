package dev.jeka.core.api.depmanagement.tooling;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A mapping to scopes to scopes acting when declaring dependencies. The goal of a scope mapping is to determine :<ul>
 * <li>which scopes a dependency is declared for</li>
 * <li>for each scope a dependency is declared, which scopes of its transitive dependencies to retrieve</li>
 * </ul>.
 *
 * For example, Your component 'A' depends of component 'B' for compiling. You can declare 'A' depends of 'B' with scope 'compile'. <br/>
 * Now imagine that for compiling, 'A' needs also the test class of 'B' along the dependencies 'B' needs for testing. For such, you
 * can declare a scope mapping as 'compile->compile, test'.
 *
 * This concept matches strictly with the <i>configuration</i> concept found in Ivy : <a href="http://wrongnotes.blogspot.be/2014/02/simplest-explanation-of-ivy.html">see here.</a>.
 */
public final class JkIvyConfigurationMapping {

    /**
     * Useful when using scope mapping. As documented in Ivy, it stands for the main archive.
     */
    public static final String ARCHIVE_MASTER = "archives(master)";

    public static final String COMPILE = "compile";

    public static final String RUNTIME = "runtime";

    public static final String TEST = "test";


    /**
     * Scope mapping used by default.
     */
    public static final JkIvyConfigurationMapping RESOLVE_MAPPING = JkIvyConfigurationMapping.of()
            .add("compile", "archives(master), compile(default)" )
            .add("runtime", "archives(master), runtime(default");


    private final List<Entry> entries;

    // -------- Factory methods ----------------------------

    private JkIvyConfigurationMapping(List<Entry> entries) {
        super();
        this.entries = entries;
    }

    /**
     * Creates an empty scope mapping.
     */
    @SuppressWarnings("unchecked")
    public static JkIvyConfigurationMapping of() {
        return new JkIvyConfigurationMapping(Collections.emptyList());
    }

    // ---------------- Instance members ---------------------------

    public JkIvyConfigurationMapping minus(String ... froms) {
        List<Entry> entries = this.entries.stream()
                .filter(entry -> !entry.hasFromEqualsTo(froms))
                .collect(Collectors.toList());
        return new JkIvyConfigurationMapping(Collections.unmodifiableList(entries));
    }

    public JkIvyConfigurationMapping add(Entry entry) {
            List<Entry> newEntries = new LinkedList();
            newEntries.add(entry);
        return new JkIvyConfigurationMapping(entries);
    }

    public JkIvyConfigurationMapping add(String comaSeparatedFrom, String comaSeparatedTo) {
        Set<String> froms = Arrays.stream(comaSeparatedFrom.split(",")).map(String::trim).collect(Collectors.toSet());
        Set<String> toes = Arrays.stream(comaSeparatedTo.split(",")).map(String::trim).collect(Collectors.toSet());
        return add(new Entry(froms, toes));
    }


    /**
     * Returns all the scopes declared on the left side of this scope mapping.
     */
    public List<Entry> getEntries() {
        return entries;
    }

    public String toIvyExpression() {
        List<String> items = entries.stream().map(Entry::toIvyExpressioin).collect(Collectors.toList());
        return String.join("; ", items);
    }

    @Override
    public String toString() {
        return entries.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkIvyConfigurationMapping that = (JkIvyConfigurationMapping) o;
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    public static class Entry {
        
        private final Set<String> from;
        
        private final Set<String> to;

        private Entry(Set<String> from, Set<String> to) {
            this.from = from;
            this.to = to;
        }
        
        public static Entry of(String from, String ... toes) {
            return new Entry(Collections.singleton(from), Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList(toes))));
        } 

        public Set<String> getFrom() {
            return from;
        }

        public Set<String> getTo() {
            return to;
        }

        public boolean hasFromEqualsTo(String... froms) {
            return this.from.equals(new HashSet<>(Arrays.asList(froms)));
        }

        public boolean hasFrom(String from) {
            return this.from.contains(from);
        }

        public String toIvyExpressioin() {
            return String.join(", ", from) + " -> " + String.join(", ", to);
        }


    }

}