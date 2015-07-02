package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

	private final Map<JkModuleId, JkVersion> map;

	private JkVersionProvider(Map<JkModuleId, JkVersion> map) {
		super();
		this.map = map;
	}

	public JkVersion getVersion(JkModuleId moduleId) {
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





}
