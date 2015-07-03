package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsIterable;

/**
 * Association between moduleIds and version.
 * 
 * @author Jerome Angibaud
 */
public final class JkVersionProvider implements Serializable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public static JkVersionProvider empty() {
		return new JkVersionProvider(Collections.EMPTY_MAP);
	}

	public static JkVersionProvider of(String moduleId, String version) {
		return of (JkModuleId.of(moduleId), version);
	}

	public static JkVersionProvider of(JkModuleId moduleId, String version) {
		return of(moduleId, JkVersion.ofName(version));
	}

	public static JkVersionProvider of(JkModuleId moduleId, JkVersion version) {
		final Map<JkModuleId, JkVersion> result = JkUtilsIterable.mapOf(moduleId, version);
		return new JkVersionProvider(result);
	}

	public static JkVersionProvider of(Iterable<JkVersionedModule> modules) {
		final Map<JkModuleId, JkVersion> result = new HashMap<JkModuleId, JkVersion>();
		for (final JkVersionedModule module : modules) {
			result.put(module.moduleId(), module.version());
		}
		return new JkVersionProvider(result);
	}

	public static JkVersionProvider mergeOf(Iterable<JkVersionProvider> versionProviders) {
		final Map<JkModuleId, JkVersion> result = new HashMap<JkModuleId, JkVersion>();
		for (final JkVersionProvider versionProvider : versionProviders) {
			result.putAll(versionProvider.map);
		}
		return new JkVersionProvider(result);
	}

	private final Map<JkModuleId, JkVersion> map;

	private JkVersionProvider(Map<JkModuleId, JkVersion> map) {
		super();
		this.map = map;
	}

	public JkVersion versionOf(JkModuleId moduleId) {
		return this.map.get(moduleId);
	}

	public JkVersionProvider and(JkVersionProvider other) {
		final Map<JkModuleId, JkVersion> newMap = new HashMap<JkModuleId, JkVersion>(this.map);
		newMap.putAll(other.map);
		return new JkVersionProvider(newMap);
	}

	public JkVersionProvider and(JkModuleId moduleId, JkVersion version) {
		final Map<JkModuleId, JkVersion> newMap = new HashMap<JkModuleId, JkVersion>(this.map);
		newMap.put(moduleId, version);
		return new JkVersionProvider(newMap);
	}

	public JkVersionProvider and(JkModuleId moduleId, String version) {
		return and(moduleId, JkVersion.ofName(version));
	}

	public JkVersionProvider and(String moduleId, String version) {
		return and(JkModuleId.of(moduleId), version);
	}



	public Set<JkModuleId> moduleIds() {
		return map.keySet();
	}





}
