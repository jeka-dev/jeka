package dev.jeka.core.api.depmanagement.tooling;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkModuleDependency;

import java.util.*;
import java.util.stream.Collectors;

public class JkScopedDependencies {

    public enum Scope {
        COMPILE, RUNTIME, TEST, PROVIDED
    }

    private List<Entry> entries;

    private JkScopedDependencies(List<Entry> entries) {
        this.entries = entries;
    }

    public static JkScopedDependencies of(List<Entry> entries) {
        return new JkScopedDependencies(Collections.unmodifiableList(new LinkedList<>(entries)));
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public List<JkModuleDependency> getModuleDependencies() {
        return entries.stream()
                .map(JkScopedDependencies.Entry::getDependency)
                .filter(JkModuleDependency.class::isInstance)
                .map(JkModuleDependency.class::cast)
                .collect(Collectors.toList());
    }

    public static class Entry {

        private final Scope scope;

        private final JkDependency dependency;

        private Entry(Scope scope, JkDependency dependency) {
            this.scope = scope;
            this.dependency = dependency;
        }

        public static Entry of(Scope scope, JkDependency dependency) {
            return new Entry(scope, dependency);
        }

        public Scope getScope() {
            return scope;
        }

        public JkDependency getDependency() {
            return dependency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            if (scope != entry.scope) return false;
            return dependency.equals(entry.dependency);
        }

        @Override
        public int hashCode() {
            int result = scope != null ? scope.hashCode() : 0;
            result = 31 * result + dependency.hashCode();
            return result;
        }
    }


}
