package dev.jeka.core.api.depmanagement;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.jeka.core.api.utils.JkUtilsIterable;

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

    public static JkDependencyExclusions of() {
        return new JkDependencyExclusions(Collections.emptyMap());
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
    public JkDependencyExclusions and(String groupAndName, String... excludedModuleIds) {
        return and(JkModuleId.of(groupAndName), excludedModuleIds);
    }

    /**
     * Adds specified exclusions on the specified module.
     */
    public JkDependencyExclusions and(JkModuleId moduleId, String... excludedModuleIds) {
        final List<JkDepExclude> depExcludes = new LinkedList<>();
        for (final String excludeId : excludedModuleIds) {
            depExcludes.add(JkDepExclude.of(excludeId));
        }
        return and(moduleId, depExcludes);
    }


    /**
     * Adds specified exclusions on the specified module.
     */
    public JkDependencyExclusions and(JkModuleId moduleId, Iterable<JkDepExclude> depExcludes) {
        List<JkDepExclude> excludes = exclusions.get(moduleId);
        if (excludes == null) {
            excludes = new LinkedList<>();
        }
        excludes.addAll(JkUtilsIterable.listOf(depExcludes));
        Map map = new HashMap(this.exclusions);
        map.put(moduleId, excludes);
        return new JkDependencyExclusions(map);
    }

}
