package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a set of qualified dependencies.
 */
public class JkQualifiedDependencySet {

    public static final String COMPILE_SCOPE = "compile"; // compile scope for published dependencies

    public static final String RUNTIME_SCOPE = "runtime";  // runtime scope for published dependencies

    public static final String PROVIDED_SCOPE = "provided";  // provided scope for published dependencies

    public static final String TEST_SCOPE = "test";  // provided scope for published dependencies

    public static final String MASTER_TARGET_CONF = "archives(master)";

    public static final String COMPILE_TARGET_CONF = "compile(default)";

    public static final String RUNTIME_TARGET_CONF = "runtime(default)";

    public static final String TEST_TARGET_CONF = "test(default)";

    private static final Map<JkTransitivity, String> TRANSITIVITY_TARGET_CONF_MAP = new HashMap<>();

    static {
        TRANSITIVITY_TARGET_CONF_MAP.put(JkTransitivity.NONE, MASTER_TARGET_CONF);
        TRANSITIVITY_TARGET_CONF_MAP.put(JkTransitivity.COMPILE, MASTER_TARGET_CONF + ", " + COMPILE_TARGET_CONF);
        TRANSITIVITY_TARGET_CONF_MAP.put(JkTransitivity.RUNTIME, MASTER_TARGET_CONF + ", " + COMPILE_TARGET_CONF
                + ", " + RUNTIME_TARGET_CONF);

    }

    private final List<JkQualifiedDependency> entries;

    // Transitive dependencies globally excluded
    private final LinkedHashSet<JkDependencyExclusion> globalExclusions;

    private final JkVersionProvider versionProvider;

    private JkQualifiedDependencySet(List<JkQualifiedDependency> qualifiedDependencies,
                                     LinkedHashSet<JkDependencyExclusion>
            globalExclusions, JkVersionProvider versionProvider) {
        this.entries = Collections.unmodifiableList(qualifiedDependencies);
        this.globalExclusions = globalExclusions;
        this.versionProvider = versionProvider;
    }

    /**
     * Creates a new empty instance of JkQualifiedDependencySet.
     */
    public static JkQualifiedDependencySet of() {
        return new JkQualifiedDependencySet(Collections.emptyList(), new LinkedHashSet<>(), JkVersionProvider.of());
    }

    /**
     * Returns a new JkQualifiedDependencySet based on the provided list of JkDependency objects.
     */
    public static JkQualifiedDependencySet ofDependencies(List<JkDependency> dependencies) {
        return of(dependencies.stream().map(dep -> JkQualifiedDependency.of(null, dep)).collect(Collectors.toList()));
    }

    /**
     * Creates a new JkQualifiedDependencySet based on the provided list of JkQualifiedDependencies.
     */
    public static JkQualifiedDependencySet of(List<JkQualifiedDependency> qualifiedDependencies) {
        return new JkQualifiedDependencySet(qualifiedDependencies,new LinkedHashSet<>(), JkVersionProvider.of());
    }

    /**
     * Creates a new JkQualifiedDependencySet based on the provided JkDependencySet.
     */
    public static JkQualifiedDependencySet of(JkDependencySet dependencySet) {
        return ofDependencies(dependencySet.getEntries())
                .withGlobalExclusions(dependencySet.getGlobalExclusions())
                .withVersionProvider(dependencySet.getVersionProvider());
    }

    /**
     * Retrieves the list of qualified dependencies.
     */
    public List<JkQualifiedDependency> getEntries() {
        return entries;
    }

    /**
     * Retrieves the list of dependencies.
     */
    public List<JkDependency> getDependencies() {
        return entries.stream()
                .map(JkQualifiedDependency::getDependency)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the list of coordinate dependencies from the JkQualifiedDependencySet.
     * Coordinate dependencies are dependencies that have module coordinates and can be used for dependency resolution.
     */
    public List<JkCoordinateDependency> getCoordinateDependencies() {
        return entries.stream()
                .map(JkQualifiedDependency::getDependency)
                .filter(JkCoordinateDependency.class::isInstance)
                .map(JkCoordinateDependency.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns the set of global exclusions for this JkQualifiedDependencySet.
     * These exclusions only apply to transitive dependencies and do not affect directly declared dependencies.
     */
    public Set<JkDependencyExclusion> getGlobalExclusions() {
        return globalExclusions;
    }

    /**
     * Returns the version provider associated with this {@link JkQualifiedDependencySet}.
     */
    public JkVersionProvider getVersionProvider() {
        return versionProvider;
    }

    /**
     * Finds and returns a list of qualified dependencies based on the specified module ID.
     */
    public List<JkQualifiedDependency> findByCoordinateGroupId(String moduleId) {
        return this.entries.stream()
                .filter(qDep -> qDep.getDependency() instanceof JkCoordinateDependency)
                .filter(qDep -> qDep.getCoordinateDependency().getCoordinate().getModuleId().toColonNotation()
                        .equals(moduleId))
                .collect(Collectors.toList());
    }

    /**
     * Removes the specified {@link JkDependency} from the {@link JkQualifiedDependencySet}.
     */
    public JkQualifiedDependencySet remove(JkDependency dependency) {
        List<JkQualifiedDependency> dependencies = entries.stream()
                .filter(qDep -> !qDep.equals(dependency))  // TODO dDep is always != dependency as they are not from same class
                .collect(Collectors.toList());
        return new JkQualifiedDependencySet(dependencies, globalExclusions, versionProvider);
    }

    /**
     * Removes the specified {@link JkQualifiedDependency} from the {@link JkQualifiedDependencySet}.
     */
    public JkQualifiedDependencySet remove(JkQualifiedDependency dependency) {
        List<JkQualifiedDependency> dependencies = entries.stream()
                .filter(qDep -> !qDep.equals(dependency))
                .collect(Collectors.toList());
        return new JkQualifiedDependencySet(dependencies, globalExclusions, versionProvider);
    }

    /**
     * Adds a new qualified dependency to the existing dependencies.
     * The qualifier is used to differentiate between multiple dependencies with the same module coordinates.
     */
    public JkQualifiedDependencySet and(JkQualifiedDependency qualifiedDependency) {
        List<JkQualifiedDependency> result = new LinkedList<>(this.entries);
        result.add(qualifiedDependency);
        return new JkQualifiedDependencySet(result, globalExclusions, versionProvider);
    }

    /**
     * Adds a new qualified dependency to the existing dependencies.
     * The qualifier is used to differentiate between multiple dependencies with the same module coordinates.
     */
    public JkQualifiedDependencySet and(String qualifier, JkDependency dependency) {
        return and(JkQualifiedDependency.of(qualifier, dependency));
    }

    /**
     * Adds a new qualified dependency to the existing dependencies. The qualifier is used to
     * differentiate between multiple dependencies with the same module coordinates.
     */
    public JkQualifiedDependencySet and(String qualifier, String moduleDependencyDescriptor) {
        return and(qualifier, JkCoordinateDependency.of(moduleDependencyDescriptor));
    }

    /**
     * Removes the specified dependency from the JkQualifiedDependencySet.
     */
    public JkQualifiedDependencySet remove(String dep) {
        return remove(JkCoordinateDependency.of(dep));
    }

    /**
     * Replaces the qualifier of the specified dependency in the {@link JkQualifiedDependencySet}.
     */
    public JkQualifiedDependencySet replaceQualifier(JkDependency dependency, String qualifier) {
        List<JkQualifiedDependency> dependencies = entries.stream()
                .map(qDep -> qDep.getDependency().equals(dependency) ? qDep.withQualifier(qualifier) : qDep)
                .collect(Collectors.toList());
        return new JkQualifiedDependencySet(dependencies, globalExclusions, versionProvider);
    }

    /**
     * Replaces the qualifier of the specified dependency in the {@link JkQualifiedDependencySet}.
     */
    public JkQualifiedDependencySet replaceQualifier(String dependency, String qualifier) {
        return replaceQualifier(JkCoordinateDependency.of(dependency), qualifier);
    }

    /**
     * Filters and returns a new JkQualifiedDependencySet containing only the JkQualifiedDependencies
     * that have the specified qualifiers.
     */
    public JkQualifiedDependencySet withQualifiersOnly(String ... qualifiers) {
        List<JkQualifiedDependency> dependencies = entries.stream()
                .filter(dep -> Arrays.asList(qualifiers).contains(dep.getQualifier()))
                .collect(Collectors.toList());
        return new JkQualifiedDependencySet(dependencies, globalExclusions, versionProvider);
    }

    /**
     * Filters and Returns a new JkQualifiedDependencySet containing only the JkQualifiedDependencies
     * whose underlying JkDependency is an instance of JkCoordinateDependency.
     */
    public JkQualifiedDependencySet withCoordinateDependenciesOnly() {
        List<JkQualifiedDependency> dependencies = entries.stream()
                .filter(qDep -> qDep.getDependency() instanceof JkCoordinateDependency)
                .collect(Collectors.toList());
        return new JkQualifiedDependencySet(dependencies, globalExclusions, versionProvider);
    }

    /**
     * These exclusions only stands for dependencies that are retrieved transitively. This means that
     * this not involves dependencies explicitly declared here.
     */
    public JkQualifiedDependencySet withGlobalExclusions(Set<JkDependencyExclusion> exclusions) {
        Set<JkDependencyExclusion> newExclusions = new HashSet<>(this.globalExclusions);
        newExclusions.addAll(exclusions);
        return new JkQualifiedDependencySet(entries, new LinkedHashSet<>(newExclusions), versionProvider);
    }

    /**
     * These exclusions only stands for dependencies that are retrieved transitively. This means that
     * this not involves dependencies explicitly declared here.
     */
    public JkQualifiedDependencySet withVersionProvider(JkVersionProvider versionProvider) {
        return new JkQualifiedDependencySet(entries, globalExclusions, this.versionProvider
            .and(versionProvider));
    }

    /**
     * Replace the version provider of this object
     */
    public JkQualifiedDependencySet withResolvedBoms(JkRepoSet repos) {
        JkVersionProvider resolvedVersionProvider = versionProvider.withResolvedBoms(repos);
        return new JkQualifiedDependencySet(this.entries, globalExclusions, resolvedVersionProvider);
    }

    /**
     * Computes the set of IDE dependencies based on the given compile, runtime, and test dependencies.
     */
    public static JkQualifiedDependencySet computeIdeDependencies(
            JkDependencySet allCompileDeps,
            JkDependencySet allRuntimeDeps,
            JkDependencySet allTestDeps,
            JkCoordinate.ConflictStrategy strategy) {
        JkDependencySetMerge prodMerge = allCompileDeps.merge(allRuntimeDeps);
        JkDependencySetMerge testMerge = prodMerge.getResult().merge(allTestDeps);
        List<JkQualifiedDependency> result = new LinkedList<>();
        List<JkDependency> dependencies = testMerge.getResult()
                .normalised(strategy)
                //.assertNoUnspecifiedVersion()
                .getEntries();
        for (JkDependency dependency : dependencies) {
            final String scope;
            if (prodMerge.getResult().getEntries().contains(dependency)) {
                if (prodMerge.getAbsentDependenciesFromRight().contains(dependency)) {
                    scope = PROVIDED_SCOPE;
                } else if (prodMerge.getAbsentDependenciesFromLeft().contains(dependency)) {
                    scope = RUNTIME_SCOPE;
                } else {
                    scope = COMPILE_SCOPE;
                }
            } else {
                scope = TEST_SCOPE;
            }
            JkDependency versionedDependency = testMerge.getResult().getVersionProvider().version(dependency);
            result.add(JkQualifiedDependency.of(scope, versionedDependency));

        }
        return new JkQualifiedDependencySet(result, testMerge.getResult().getGlobalExclusions(),
                testMerge.getResult().getVersionProvider());
    }

    /**
     * Computes the set of IDE dependencies based on the given compile, runtime, and test dependencies.
     */
    public static JkQualifiedDependencySet computeIdeDependencies(
            JkDependencySet allCompileDeps,
            JkDependencySet allRuntimeDeps,
            JkDependencySet allTestDeps) {
        return computeIdeDependencies(allCompileDeps, allRuntimeDeps, allTestDeps, JkCoordinate.ConflictStrategy.FAIL);
    }

    /**
     * Computes the set of qualified dependencies for publishing using Ivy.
     */
    public static JkQualifiedDependencySet computeIvyPublishDependencies(
            JkDependencySet allCompileDeps,
            JkDependencySet allRuntimeDeps,
            JkDependencySet allTestDeps,
            JkCoordinate.ConflictStrategy strategy) {
        JkDependencySetMerge mergeWithProd = allCompileDeps.merge(allRuntimeDeps);
        JkDependencySetMerge mergeWithTest = mergeWithProd.getResult().merge(allTestDeps);
        List<JkQualifiedDependency> result = new LinkedList<>();
        for (JkCoordinateDependency dependency : mergeWithTest.getResult().normalised(strategy)
                .assertNoUnspecifiedVersion().getVersionResolvedCoordinateDependencies()) {
            final String configurationSource;
            String configurationTarget;
            if (mergeWithProd.getResult().getMatching(dependency) != null) {
                if (mergeWithProd.getAbsentDependenciesFromRight().contains(dependency)) {
                    configurationSource = COMPILE_SCOPE;
                    configurationTarget = MASTER_TARGET_CONF + ", " + COMPILE_TARGET_CONF;
                } else if (mergeWithProd.getAbsentDependenciesFromLeft().contains(dependency)) {
                    configurationSource = RUNTIME_SCOPE;
                    configurationTarget = MASTER_TARGET_CONF + ", " + RUNTIME_TARGET_CONF;
                } else {
                    configurationSource = COMPILE_SCOPE + "," + RUNTIME_SCOPE;
                    configurationTarget = MASTER_TARGET_CONF + ", " + COMPILE_TARGET_CONF + ", " + RUNTIME_TARGET_CONF;
                }
            } else {
                configurationSource = TEST_SCOPE;
                configurationTarget = MASTER_TARGET_CONF + ", " + COMPILE_TARGET_CONF + ", "
                        + RUNTIME_TARGET_CONF + ", " + TEST_TARGET_CONF;
            }
            if (dependency.getTransitivity() != null) {
                configurationTarget = getIvyTargetConfigurations(dependency.getTransitivity());
            }
            String configuration = configurationSource + " -> " + configurationTarget;
            result.add(JkQualifiedDependency.of(configuration, dependency));
        }
        return new JkQualifiedDependencySet(result, mergeWithTest.getResult().getGlobalExclusions(),
                mergeWithTest.getResult().getVersionProvider());
    }

    /**
     * Returns the Ivy target configurations for the given transitivity.
     */
    public static String getIvyTargetConfigurations(JkTransitivity transitivity) {
        return TRANSITIVITY_TARGET_CONF_MAP.get(transitivity);
    }

    /**
     * Returns a List of JkDependency objects that have the specified qualifiers.
     */
    public List<JkDependency> getDependenciesHavingQualifier(String ... qualifiers) {
        List<String> list = Arrays.asList(qualifiers);
        return entries.stream()
                .filter(qDep -> list.contains(qDep.getQualifier()))
                .map(JkQualifiedDependency::getDependency)
                .collect(Collectors.toList());
    }

    /**
     * Checks if any module in the dependency set has an unspecified version.
     */
    public JkQualifiedDependencySet assertNoUnspecifiedVersion() {
        final List<JkCoordinateDependency> unspecifiedVersionModules = getCoordinateDependencies().stream()
                .filter(dep -> this.versionProvider.getVersionOfOrUnspecified(
                        dep.getCoordinate().getModuleId()).isUnspecified())
                .filter(dep -> dep.getCoordinate().getVersion().isUnspecified())
                .collect(Collectors.toList());
        JkUtilsAssert.state(unspecifiedVersionModules.isEmpty(), "Following module does not specify version : "
                + unspecifiedVersionModules);
        return this;
    }

    /**
     * Fills the dependencies without specified version with the version supplied by the {@link JkVersionProvider}.
     */
    public JkQualifiedDependencySet toResolvedModuleVersions() {
        List<JkQualifiedDependency> dependencies = entries.stream()
                .map(qDep -> {
                    if (qDep.getDependency() instanceof JkCoordinateDependency) {
                        JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) qDep.getDependency();
                        JkVersion providedVersion = this.versionProvider
                                .getVersionOfOrUnspecified(coordinateDependency.getCoordinate().getModuleId());
                        if (coordinateDependency.getCoordinate().getVersion().isUnspecified()
                                && !providedVersion.isUnspecified()) {
                            return JkQualifiedDependency.of(qDep.getQualifier(),
                                    coordinateDependency.withVersion(providedVersion));
                        }
                    }
                    return qDep;
                })
                .collect(Collectors.toList());
        return new JkQualifiedDependencySet(dependencies, this.globalExclusions, this.versionProvider);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JkQualifiedDependencySet that = (JkQualifiedDependencySet) o;

        if (!entries.equals(that.entries)) return false;
        if (!globalExclusions.equals(that.globalExclusions)) return false;
        return versionProvider.equals(that.versionProvider);
    }

    @Override
    public int hashCode() {
        int result = entries.hashCode();
        result = 31 * result + globalExclusions.hashCode();
        result = 31 * result + versionProvider.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName()).append("\n");
        sb.append("Dependencies :\n");
        this.entries.forEach(entry -> sb.append(entry).append("\n"));
        sb.append("global exclusions :\n");
        this.globalExclusions.forEach(exclusion -> sb.append(exclusion).append("\n"));
        sb.append("Version provider :\n");
        sb.append(versionProvider);
        return sb.toString();
    }

    /**
     * Computes the MD5 hash of the current instance and returns it as a string.
     * The string is suitable to be used as a file name.
     */
    public String md5() {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            this.entries.forEach(dep -> digest.update(toHashByteArray(dep)));
            this.globalExclusions.forEach(exclusion -> digest.update(toHashByteArray(exclusion)));
            digest.update(toHashByteArray(this.versionProvider));
            return Base64.getEncoder().encodeToString(digest.digest()).replace('/', '-');
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] toHashByteArray(Object object) {
        if (object == null) {
            return new byte[0];
        }
        return Integer.toString(object.hashCode()).getBytes();
    }
}
