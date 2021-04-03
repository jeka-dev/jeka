package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Association between getModuleIds and version.
 *
 * @author Jerome Angibaud
 */
public final class JkVersionProvider {

    private final Map<JkModuleId, JkVersion> map;

    private JkVersionProvider(Map<JkModuleId, JkVersion> map) {
        super();
        this.map = map;
    }

    /**
     * @see #of(JkModuleId, JkVersion)
     */
    public static JkVersionProvider of(String moduleId, String version) {
        return of(JkModuleId.of(moduleId), version);
    }

    /**
     * @see #of(JkModuleId, JkVersion)
     */
    public static JkVersionProvider of(JkModuleId moduleId, String version) {
        return of(moduleId, JkVersion.of(version));
    }

    /**
     * Creates a {@link JkVersionProvider} holding a single version providing.
     */
    public static JkVersionProvider of(JkModuleId moduleId, JkVersion version) {
        final Map<JkModuleId, JkVersion> result = JkUtilsIterable.mapOf(moduleId, version);
        return new JkVersionProvider(result);
    }

    /**
     * Creates an empty version provider.
     */
    public static JkVersionProvider of() {
        return new JkVersionProvider(Collections.emptyMap());
    }

    /**
     * Creates a version provider from the specified versioned modules.
     */
    public static JkVersionProvider of(Iterable<JkVersionedModule> modules) {
        final Map<JkModuleId, JkVersion> result = new HashMap<>();
        for (final JkVersionedModule module : modules) {
            result.put(module.getModuleId(), module.getVersion());
        }
        return new JkVersionProvider(result);
    }

    /**
     * Returns the version to use with specified module.
     */
    public JkVersion getVersionOf(JkModuleId moduleId) {
        return this.map.get(moduleId);
    }

    /**
     * Returns <code>true</code> if this providers is empty.
     */
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    /**
     * Returns a {@link JkVersionProvider} that is a union of this provider and the specified one.
     */
    public JkVersionProvider and(JkVersionProvider other) {
        final Map<JkModuleId, JkVersion> newMap = new HashMap<>(this.map);
        newMap.putAll(other.map);
        return new JkVersionProvider(newMap);
    }

    /**
     * Returns a {@link JkVersionProvider} that is the union of this provider and the specified one.
     */
    public JkVersionProvider and(JkModuleId moduleId, JkVersion version) {
        final Map<JkModuleId, JkVersion> newMap = new HashMap<>(this.map);
        newMap.put(moduleId, version);
        return new JkVersionProvider(newMap);
    }

    /**
     * @see JkVersionProvider#and(JkModuleId, JkVersion)
     */
    public JkVersionProvider and(JkModuleId moduleId, String version) {
        return and(moduleId, JkVersion.of(version));
    }

    /**
     * @see JkVersionProvider#and(JkModuleId, JkVersion)
     */
    public JkVersionProvider and(String moduleId, String version) {
        return and(JkModuleId.of(moduleId), version);
    }

    /**
     * @see JkVersionProvider#and(JkModuleId, JkVersion)
     */
    public JkVersionProvider and(String group, String name, String version) {
        return and(JkModuleId.of(group, name), version);
    }

    /**
     * Returns all modules that this object provides version for.
     */
    public Set<JkModuleId> getModuleIds() {
        return map.keySet();
    }

    @Override
    public String toString() {
        return this.map.toString();
    }

    public Map<JkModuleId, JkVersion> toMap() {
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns the java codes that declare these dependencies.
     */
    public String toJavaCode(int margin) {
        final String indent = JkUtilsString.repeat(" ", margin);
        final StringBuilder builder = new StringBuilder();
        builder.append("JkVersionProvider.of()");
        for (final Map.Entry<JkModuleId, JkVersion> entry : map.entrySet()) {
            JkModuleId moduleId = entry.getKey();
            JkVersion version = entry.getValue();
            builder.append("\n").append(indent).append(".and(\"")
                    .append(moduleId.getGroupAndName() + "\", ")
                    .append("\"" + version + "\")");
        }
        return builder.toString();
    }

}
