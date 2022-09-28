package dev.jeka.core.api.tooling;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkDependencyExclusion;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.util.*;

/**
 * @author Jerome Angibaud
 */
class DependencyExclusions {

    private final Map<JkModuleId, List<JkDependencyExclusion>> exclusions;

    private DependencyExclusions(Map<JkModuleId, List<JkDependencyExclusion>> exclusions) {
        super();
        this.exclusions = Collections.unmodifiableMap(exclusions);
    }

    public static DependencyExclusions of() {
        return new DependencyExclusions(Collections.emptyMap());
    }

    /**
     * Returns the modules on which some transitive dependencies are excluded.
     */
    public Set<JkModuleId> getModuleIds() {
        return this.exclusions.keySet();
    }

    /**
     * Returns the transitive dependency module to exclude to the specified module.
     */
    public List<JkDependencyExclusion> get(JkModuleId jkModuleId) {
        return exclusions.get(jkModuleId);
    }

    /**
     * Returns <code>true</code> if this object contains no exclusion.
     */
    public boolean isEmpty() {
        return this.exclusions.isEmpty();
    }

    /**
     * Adds specified exclusions on the specified module.
     */
    public DependencyExclusions and(String moduleId, String... excludedModuleIds) {
        return and(JkModuleId.of(moduleId), excludedModuleIds);
    }

    /**
     * Adds specified exclusions on the specified module.
     */
    public DependencyExclusions and(JkModuleId jkModuleId, String... excludedModuleIds) {
        final List<JkDependencyExclusion> depExcludes = new LinkedList<>();
        for (final String excludeId : excludedModuleIds) {
            depExcludes.add(JkDependencyExclusion.of(excludeId));
        }
        return and(jkModuleId, depExcludes);
    }


    /**
     * Adds specified exclusions on the specified module.
     */
    public DependencyExclusions and(JkModuleId jkModuleId, Iterable<JkDependencyExclusion> depExcludes) {
        List<JkDependencyExclusion> excludes = exclusions.get(jkModuleId);
        if (excludes == null) {
            excludes = new LinkedList<>();
        }
        excludes.addAll(JkUtilsIterable.listOf(depExcludes));
        Map map = new HashMap(this.exclusions);
        map.put(jkModuleId, excludes);
        return new DependencyExclusions(map);
    }

}
