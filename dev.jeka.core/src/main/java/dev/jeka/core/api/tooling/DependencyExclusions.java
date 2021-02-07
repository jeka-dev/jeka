package dev.jeka.core.api.tooling;

import dev.jeka.core.api.depmanagement.JkDepExclude;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.util.*;

/**
 * @author Jerome Angibaud
 */
class DependencyExclusions {

    private final Map<JkModuleId, List<JkDepExclude>> exclusions;

    private DependencyExclusions(Map<JkModuleId, List<JkDepExclude>> exclusions) {
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
     * Adds specified exclusions on the specified module.
     */
    public DependencyExclusions and(String groupAndName, String... excludedModuleIds) {
        return and(JkModuleId.of(groupAndName), excludedModuleIds);
    }

    /**
     * Adds specified exclusions on the specified module.
     */
    public DependencyExclusions and(JkModuleId moduleId, String... excludedModuleIds) {
        final List<JkDepExclude> depExcludes = new LinkedList<>();
        for (final String excludeId : excludedModuleIds) {
            depExcludes.add(JkDepExclude.of(excludeId));
        }
        return and(moduleId, depExcludes);
    }


    /**
     * Adds specified exclusions on the specified module.
     */
    public DependencyExclusions and(JkModuleId moduleId, Iterable<JkDepExclude> depExcludes) {
        List<JkDepExclude> excludes = exclusions.get(moduleId);
        if (excludes == null) {
            excludes = new LinkedList<>();
        }
        excludes.addAll(JkUtilsIterable.listOf(depExcludes));
        Map map = new HashMap(this.exclusions);
        map.put(moduleId, excludes);
        return new DependencyExclusions(map);
    }

}
