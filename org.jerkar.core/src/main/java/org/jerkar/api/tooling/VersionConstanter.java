package org.jerkar.api.tooling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkVersionProvider;

class VersionConstanter {

    private final Map<String, String> groupToVersionConstant;

    private final JkVersionProvider versionProvider;

    private VersionConstanter(Map<String, String> groupToConstant, JkVersionProvider versionProvider) {
	super();
	this.groupToVersionConstant = groupToConstant;
	this.versionProvider = versionProvider;
    }

    public String versionLiteral(JkModuleId moduleId) {
	final String constant = groupToVersionConstant.get(moduleId.group());
	if (constant == null) {
	    return "\""+versionProvider.versionOf(moduleId).name()+"\"";
	}
	return constantName(moduleId.group());
    }

    public Map<String, String> constantNameValue() {
	final Map<String, String> result = new HashMap<String, String>();
	for (final String group : groupToVersionConstant.keySet()) {
	    result.put(constantName(group), groupToVersionConstant.get(group));
	}
	return result;
    }

    public static VersionConstanter of(JkVersionProvider jkVersionProvider) {
	final Map<String, String> electedGroupToVersion = new HashMap<String, String>();
	final Map<String, String> groupToVersion = new HashMap<String, String>();
	final Set<String> dismissedGroups = new HashSet<String>();
	for (final JkModuleId moduleId : jkVersionProvider.moduleIds()) {
	    final String group = moduleId.group();
	    if (dismissedGroups.contains(group)) {
		continue;
	    }
	    final String registeredVersion = groupToVersion.get(group);
	    final String currentVersion = jkVersionProvider.versionOf(moduleId).name();
	    if (registeredVersion == null) {
		groupToVersion.put(group, currentVersion);
	    } else if (!registeredVersion.equals(currentVersion)) {
		dismissedGroups.add(group);
		groupToVersion.remove(group);
		electedGroupToVersion.remove(group);
	    } else {
		electedGroupToVersion.put(group, registeredVersion);
	    }
	}
	return new VersionConstanter(electedGroupToVersion, jkVersionProvider);

    }

    private static String constantName(String group) {
	return group.toUpperCase().replace('.', '_') + "_VERSION";
    }

}
