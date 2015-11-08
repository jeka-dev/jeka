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

    public static Builder builder() {
	return new Builder();
    }

    public Set<JkModuleId> moduleIds() {
	return this.exclusions.keySet();
    }

    public List<JkDepExclude> get(JkModuleId moduleId) {
	return exclusions.get(moduleId);
    }

    public boolean isEmpty() {
	return this.exclusions.isEmpty();
    }


    public static class Builder {

	Builder() {}

	private final Map<JkModuleId, List<JkDepExclude>> exclusions = new HashMap<JkModuleId, List<JkDepExclude>>();

	public Builder on(JkModuleId moduleId, JkDepExclude ...depExcludes) {
	    return on(moduleId, Arrays.asList(depExcludes));
	}

	public Builder on(String moduleId, String ... excludedModuleIds) {
	    final JkModuleId jkModuleId = JkModuleId.of(moduleId);
	    final List<JkDepExclude> depExcludes = new LinkedList<JkDepExclude>();
	    for (final String excludeId : excludedModuleIds) {
		depExcludes.add(JkDepExclude.of(excludeId));
	    }
	    return on(jkModuleId, depExcludes);
	}

	public Builder on(JkModuleId moduleId, Iterable<JkDepExclude> depExcludes) {
	    List<JkDepExclude> excludes = exclusions.get(moduleId);
	    if (excludes == null) {
		excludes = new LinkedList<JkDepExclude>();
		exclusions.put(moduleId, excludes);
	    }
	    excludes.addAll(JkUtilsIterable.listOf(depExcludes));
	    return this;
	}

	public JkDependencyExclusions build() {
	    final Map<JkModuleId, List<JkDepExclude>> map = new HashMap<JkModuleId, List<JkDepExclude>>();
	    for (final JkModuleId moduleId : exclusions.keySet()) {
		map.put(moduleId, Collections.unmodifiableList(exclusions.get(moduleId)));
	    }
	    return new JkDependencyExclusions(map);
	}


    }



}
