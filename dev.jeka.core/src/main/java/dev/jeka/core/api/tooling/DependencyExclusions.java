package dev.jeka.core.api.tooling;

import dev.jeka.core.api.depmanagement.JkCoordinate.GroupAndName;
import dev.jeka.core.api.depmanagement.JkDependencyExclusion;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.util.*;

/**
 * @author Jerome Angibaud
 */
class DependencyExclusions {

    private final Map<GroupAndName, List<JkDependencyExclusion>> exclusions;

    private DependencyExclusions(Map<GroupAndName, List<JkDependencyExclusion>> exclusions) {
        super();
        this.exclusions = Collections.unmodifiableMap(exclusions);
    }

    public static DependencyExclusions of() {
        return new DependencyExclusions(Collections.emptyMap());
    }

    /**
     * Returns the modules on which some transitive dependencies are excluded.
     */
    public Set<GroupAndName> getModuleIds() {
        return this.exclusions.keySet();
    }

    /**
     * Returns the transitive dependency module to exclude to the specified module.
     */
    public List<JkDependencyExclusion> get(GroupAndName groupAndName) {
        return exclusions.get(groupAndName);
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
        return and(GroupAndName.of(groupAndName), excludedModuleIds);
    }

    /**
     * Adds specified exclusions on the specified module.
     */
    public DependencyExclusions and(GroupAndName groupAndName, String... excludedModuleIds) {
        final List<JkDependencyExclusion> depExcludes = new LinkedList<>();
        for (final String excludeId : excludedModuleIds) {
            depExcludes.add(JkDependencyExclusion.of(excludeId));
        }
        return and(groupAndName, depExcludes);
    }


    /**
     * Adds specified exclusions on the specified module.
     */
    public DependencyExclusions and(GroupAndName groupAndName, Iterable<JkDependencyExclusion> depExcludes) {
        List<JkDependencyExclusion> excludes = exclusions.get(groupAndName);
        if (excludes == null) {
            excludes = new LinkedList<>();
        }
        excludes.addAll(JkUtilsIterable.listOf(depExcludes));
        Map map = new HashMap(this.exclusions);
        map.put(groupAndName, excludes);
        return new DependencyExclusions(map);
    }

}
