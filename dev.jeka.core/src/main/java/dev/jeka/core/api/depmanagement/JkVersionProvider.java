package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.tooling.JkPom;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.*;

/**
 * Association between getModuleIds and version.
 *
 * @author Jerome Angibaud
 */
public final class JkVersionProvider {

    private final Map<JkModuleId, JkVersion> map;

    private final LinkedHashSet<JkCoordinate> boms;

    private JkVersionProvider(Map<JkModuleId, JkVersion> map, LinkedHashSet<JkCoordinate> boms) {
        super();
        this.map = map;
        this.boms = boms;
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
    public static JkVersionProvider of(JkModuleId jkModuleId, String version) {
        return of(jkModuleId, JkVersion.of(version));
    }

    /**
     * Creates a {@link JkVersionProvider} holding a single version providing.
     */
    public static JkVersionProvider of(JkModuleId jkModuleId, JkVersion version) {
        final Map<JkModuleId, JkVersion> result = JkUtilsIterable.mapOf(jkModuleId, version);
        return new JkVersionProvider(result, new LinkedHashSet<>());
    }

    /**
     * Creates an empty version provider.
     */
    public static JkVersionProvider of() {
        return new JkVersionProvider(Collections.emptyMap(), new LinkedHashSet<>());
    }

    /**
     * Creates a version provider from the specified versioned modules.
     */
    public static JkVersionProvider of(Iterable<JkCoordinate> coordinates) {
        final Map<JkModuleId, JkVersion> result = new HashMap<>();
        for (final JkCoordinate coordinate : coordinates) {
            result.put(coordinate.getModuleId(), coordinate.getVersion());
        }
        return new JkVersionProvider(result, new LinkedHashSet<>());
    }

    /**
     * Returns the version to use with specified module.
     */
    public JkVersion getVersionOf(JkModuleId jkModuleId) {
        return this.map.get(jkModuleId);
    }

    public JkVersion getVersionOfOrUnspecified(JkModuleId moduleId) {
        return Optional.ofNullable(getVersionOf(moduleId)).orElse(JkVersion.UNSPECIFIED);
    }

    /**
     * Returns the version to use with specified module.
     */
    public String getVersionOf(String moduleId) {
        JkVersion version = getVersionOf(JkModuleId.of(moduleId));
        if (version == null) {
            return null;
        }
        return version.getValue();
    }

    public JkDependency version(JkDependency dependency) {
        if (! (dependency instanceof JkCoordinateDependency)) {
            return dependency;
        }
        JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) dependency;
        JkCoordinate coordinate = coordinateDependency.getCoordinate();
        JkVersion providedVersion = this.getVersionOf(coordinate.getModuleId());
        if (coordinate.getVersion().isUnspecified() && providedVersion != null) {
            return coordinateDependency.withVersion(providedVersion);
        }
        return coordinateDependency;
    }

    /**
     * Returns <code>true</code> if this provider is empty.
     */
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    /**
     * Returns a {@link JkVersionProvider} that is a union of this provider and the specified one.
     * The versions present in the specified one will override versions specified in this one.
     */
    public JkVersionProvider and(JkVersionProvider other) {
        final Map<JkModuleId, JkVersion> newMap = new HashMap<>(this.map);
        newMap.putAll(other.map);
        LinkedHashSet<JkCoordinate> newBoms = new LinkedHashSet<>(this.boms);
        newBoms.addAll(other.boms);
        return new JkVersionProvider(newMap, newBoms);
    }

    /**
     * Returns a {@link JkVersionProvider} that is the union of this provider and the specified one.
     */
    public JkVersionProvider and(JkModuleId jkModuleId, JkVersion version) {
        final Map<JkModuleId, JkVersion> newMap = new HashMap<>(this.map);
        newMap.put(jkModuleId, version);
        return new JkVersionProvider(newMap, this.boms);
    }

    /**
     * @see JkVersionProvider#and(JkModuleId, JkVersion)
     */
    public JkVersionProvider and(JkModuleId jkModuleId, String version) {
        return and(jkModuleId, JkVersion.of(version));
    }

    /**
     * @param dependencyDescription Can be expressed as group:name::pom:version
     * or group:name:version. In last case, it will be converted in the first expression
     */
    public JkVersionProvider andBom(String dependencyDescription) {
        String[] items = dependencyDescription.split(":");
        final JkCoordinate coordinate;
        if (items.length == 5) {
            coordinate= JkCoordinate.of(dependencyDescription);
        } else if (items.length == 3) {
            JkModuleId jkModuleId = JkModuleId.of(items[0], items[1]);
            JkVersion version = JkVersion.of(items[2]);
            coordinate = JkCoordinate.of(jkModuleId, version).withClassifierAndType("", "pom");
        } else {
            throw new IllegalArgumentException("dependencyDescription must be expressed as 'group:name::pom:version' " +
                    "or 'group:name:version'. was " + dependencyDescription);
        }
        return andBom(coordinate);
    }

    public JkVersionProvider andBom(JkCoordinate coordinate) {
        LinkedHashSet<JkCoordinate> newBoms = new LinkedHashSet<>(this.boms);
        newBoms.add(coordinate);
        return new JkVersionProvider(this.map, newBoms);
    }

    /**
     * @param versionedModule module group, name and version expressed as 'group:name:version'
     * @see JkVersionProvider#and(JkModuleId, JkVersion)
     */
    public JkVersionProvider and(String versionedModule) {
        JkUtilsAssert.argument(versionedModule.split(":").length == 3,
                "versioned module should be expressed as 'group:name:version' was '%s'", versionedModule);
        String moduleId = JkUtilsString.substringBeforeLast(versionedModule, ":");
        String version = JkUtilsString.substringAfterLast(versionedModule, ":");
        return and(moduleId, version);
    }


    /**
     * @see JkVersionProvider#and(JkModuleId, JkVersion)
     */
    public JkVersionProvider and(String moduleId, String version) {
        return and(JkModuleId.of(moduleId), version);
    }

    /**
     * Returns all module ids that this object provides version for.
     */
    public Set<JkModuleId> getModuleIds() {
        return map.keySet();
    }

    @Override
    public String toString() {
        return this.map.toString() +  ", " + boms;
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
            JkModuleId jkModuleId = entry.getKey();
            JkVersion version = entry.getValue();
            builder.append("\n").append(indent).append(".and(\"")
                    .append(jkModuleId + "\", ")
                    .append("\"" + version + "\")");
        }
        return builder.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkVersionProvider that = (JkVersionProvider) o;
        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Returns an equivalent version provider of this one but resolving its boms into internal version map.
     */
    public JkVersionProvider withResolvedBoms(JkRepoSet repos) {
        JkVersionProvider provider = boms.stream()
                .distinct()
                .map(bom -> {
                    JkCoordinateFileProxy bomFile = JkCoordinateFileProxy.of(repos, bom);
                    return JkPom.of(bomFile.get()).getVersionProvider();
                })
                .reduce(this, (versionProvider1, versionProvider2) -> versionProvider1.and(versionProvider2));
        return new JkVersionProvider(provider.map, new LinkedHashSet<>());
    }

    public Set<JkCoordinate> getBoms() {
        return Collections.unmodifiableSet(boms);
    }


}
