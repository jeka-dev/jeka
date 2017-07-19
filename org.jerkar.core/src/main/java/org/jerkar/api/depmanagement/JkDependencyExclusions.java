package org.jerkar.api.depmanagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsIterable;

/**
 * Holds information about transitive dependencies to exclude.
 * 
 * @author Jerome Angibaud
 */
public class JkDependencyExclusions {

    private final Map<JkModuleId, List<JkDepExclude>> exclusions;

    private JkDependencyExclusions(Map<JkModuleId, List<JkDepExclude>> exclusions) {
        super();
        this.exclusions = Collections.unmodifiableMap(exclusions);
    }

    /**
     * Creates a builder for {@link JkDependencyExclusions}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the modules on which some transitive dependencies are excluded.
     */
    public Set<JkModuleId> moduleIds() {
        return this.exclusions.keySet();
    }

    /**
     * Returns the transitive dependency module to exclude go the specified module.
     */
    public List<JkDepExclude> get(JkModuleId moduleId) {
        return exclusions.get(moduleId);
    }

    /**
     * Returns <code>true</code> if this object contains no exclusion.
     */
    public boolean isEmpty() {
        return this.exclusions.isEmpty();
    }

    /**
     * A builder for {@link JkDependencyExclusions}.
     */
    public static class Builder {

        Builder() {
        }

        private final Map<JkModuleId, List<JkDepExclude>> exclusions = new HashMap<JkModuleId, List<JkDepExclude>>();

        /**
         * Adds specified exclusions on the specified module.
         */
        public Builder on(JkModuleId moduleId, JkDepExclude... depExcludes) {
            return on(moduleId, Arrays.asList(depExcludes));
        }

        /**
         * Adds specified exclusions on the specified module.
         */
        public Builder on(JkModuleId moduleId, String... excludedModuleIds) {
            final List<JkDepExclude> depExcludes = new LinkedList<JkDepExclude>();
            for (final String excludeId : excludedModuleIds) {
                depExcludes.add(JkDepExclude.of(excludeId));
            }
            return on(moduleId, depExcludes);
        }

        /**
         * Adds specified exclusions on the specified module.
         */
        public Builder on(String groupAndName, String... excludedModuleIds) {
            return on(JkModuleId.of(groupAndName), excludedModuleIds);
        }

        /**
         * Adds specified exclusions on the specified module.
         */
        public Builder on(JkModuleId moduleId, Iterable<JkDepExclude> depExcludes) {
            List<JkDepExclude> excludes = exclusions.get(moduleId);
            if (excludes == null) {
                excludes = new LinkedList<JkDepExclude>();
                exclusions.put(moduleId, excludes);
            }
            excludes.addAll(JkUtilsIterable.listOf(depExcludes));
            return this;
        }

        /**
         * Creates a {@link JkDependencyExclusions} based on this builder content.
         */
        public JkDependencyExclusions build() {
            final Map<JkModuleId, List<JkDepExclude>> map = new HashMap<JkModuleId, List<JkDepExclude>>();
            for (final JkModuleId moduleId : exclusions.keySet()) {
                map.put(moduleId, Collections.unmodifiableList(exclusions.get(moduleId)));
            }
            return new JkDependencyExclusions(map);
        }

    }

}
