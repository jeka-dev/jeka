package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A set of {@link JkDependency} generally standing for a given purpose (compile, test, runtime) in a project build. <p>
 * {@link JkDependencySet} also holds a {@link JkVersionProvider} and a set of {@link JkDependencyExclusion}.
 *
 *
 * @author Jerome Angibaud.
 */
public class JkDependencySet {

    private final List<JkDependency> entries;

    private final Set<JkDependencyExclusion> globalExclusions;

    private final JkVersionProvider versionProvider;

    private JkDependencySet(List<? extends JkDependency> dependencies, Set<JkDependencyExclusion> exclusions,
                            JkVersionProvider explicitVersions) {
        super();
        this.entries = Collections.unmodifiableList(dependencies);
        this.globalExclusions = Collections.unmodifiableSet(exclusions);
        this.versionProvider = explicitVersions;
    }

    public static JkDependencySet of(String dependencyDesc) {
        final JkDependency dependency;
        if (JkCoordinate.isCoordinateDescription(dependencyDesc)) {
            dependency = JkCoordinateDependency.of(dependencyDesc);
        } else {
            dependency = JkFileSystemDependency.of(Paths.get(dependencyDesc));
        }
        return of(JkUtilsIterable.listOf(dependency));
    }

    public static JkDependencySet of() {
        return of(Collections.emptyList());
    }

    /**
     * Creates a {@link JkDependencySet} to the specified scoped dependencies.
     */
    public static JkDependencySet of(List<? extends JkDependency> dependencies) {
        return new JkDependencySet(dependencies, Collections.emptySet(), JkVersionProvider.of());
    }

    public static JkDependencySet of(JkDependency dependency) {
        return of(JkUtilsIterable.listOf(dependency));
    }

    /**
     * Returns the unmodifiable list of scoped dependencies for this object.
     */
    public List<JkDependency> getEntries() {
        return this.entries;
    }

    /**
     * Returns a clone of this object plus the specified {@link JkDependency}s, at the
     * specified place and condition.
     */
    public JkDependencySet and(Hint hint, JkDependencySet other) {
        List<JkDependency> others = other.entries;
        JkDependencySet proto = this.and(other);
        final List<JkDependency> result = new LinkedList<>(this.entries);
        if (hint == null) {
            result.addAll(others);
            return new JkDependencySet(result, globalExclusions, versionProvider);
        }
        if (hint.condition == false) {
            return this;
        }
        if (hint.first == true) {
            result.addAll(0, others);
            return new JkDependencySet(result, globalExclusions, versionProvider);
        }
        if (hint.before == null) {
            result.addAll(others);
            return new JkDependencySet(result, globalExclusions, versionProvider);
        }
        int index = firstIndexMatching(hint.before);
        if (index == -1) {
            throw new IllegalArgumentException("No dependency " + hint.before + " found on " + result);
        }
        result.addAll(index, others);
        return new JkDependencySet(result, proto.globalExclusions, proto.versionProvider);
    }

    public JkDependencySet and(Hint hint, List<? extends JkDependency> others) {
        return and(hint, JkDependencySet.of(others));
    }

    private int firstIndexMatching(JkDependency dependency) {
        int i = 0;
        for (JkDependency dep : entries) {
            if (dep.matches(dependency)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * Returns a clone of this object plus the specified {@link JkDependency}s at the tail of
     * this one.
     */
    public JkDependencySet and(List<? extends JkDependency> others) {
        return and(null, others);
    }

    public JkDependencySet and(JkDependencySet other) {
        final List<JkDependency> deps = JkUtilsIterable.concatLists(this.entries, other.entries);
        Set<JkDependencyExclusion> newGlobalExcludes = new HashSet<>(this.globalExclusions);
        newGlobalExcludes.addAll(other.globalExclusions);
        JkVersionProvider newVersionProvider = this.versionProvider.and(other.versionProvider);
        return new JkDependencySet(deps, newGlobalExcludes, newVersionProvider);
    }

    public JkDependencySet and(Hint hint, JkDependency... others) {
        return and(hint, Arrays.asList(others));
    }

    public JkDependencySet and(JkDependency... others) {
        return and(null, others);
    }

    public JkDependencySet and(Hint hint, String moduleDescriptor) {
        return and(hint, JkCoordinateDependency.of(moduleDescriptor));
    }

    public JkDependencySet and(Hint hint, JkCoordinate.GroupAndName groupAndName) {
        return and(hint, groupAndName.toString());
    }

    public JkDependencySet and(JkCoordinate.GroupAndName groupAndName) {
        return and(null, groupAndName.toString());
    }

    public JkDependencySet and(@JkDepSuggest String moduleDescriptor) {
        return and(null, moduleDescriptor);
    }

    public JkDependencySet and(Hint hint, String moduleDescriptor, JkTransitivity transitivity) {
        return and(hint, JkCoordinateDependency.of(moduleDescriptor).withTransitivity(transitivity));
    }

    public JkDependencySet and(String moduleDescriptor, JkTransitivity transitivity) {
        return and(null, moduleDescriptor, transitivity);
    }

    public JkDependencySet andFiles(Hint hint, Iterable<Path> paths) {
        if (JkUtilsPath.disambiguate(paths).isEmpty()) {
            return this;
        }
        return and(hint, JkFileSystemDependency.of(paths));
    }

    public JkDependencySet andFiles(Iterable<Path> paths) {
        return andFiles(null, paths);
    }

    public JkDependencySet andFiles(Hint hint, String... paths) {
        return andFiles(hint, Stream.of(paths).map(Paths::get).collect(Collectors.toList()));
    }

    public JkDependencySet andFiles(String... paths) {
        return andFiles(null, paths);
    }

    public JkDependencySet minus(List<JkDependency> dependencies) {
        List<JkDependency> result = new LinkedList<>(this.getEntries());
        for (JkDependency dependency : dependencies) {
            JkDependency matchingDependency = this.getMatching(dependency);
            if (matchingDependency != null) {
                result.remove(matchingDependency);
            }
        }
        return new JkDependencySet(result, this.globalExclusions, this.versionProvider);
    }

    public JkDependencySet minus(JkDependency dependency) {
        List<JkDependency> list = JkUtilsIterable.listOf(dependency);
        return minus(list);
    }

    public JkDependencySet minus(Path path) {
        return minus(JkFileSystemDependency.of(path));
    }

    public JkDependencySet minus(JkCoordinate.GroupAndName groupAndName) {
        return minus(JkCoordinateDependency.of(JkCoordinate.of(groupAndName)));
    }

    public JkDependencySet minus(String groupAndName) {
        return minus(JkCoordinate.GroupAndName.of(groupAndName));
    }

    public JkDependencySet withGlobalTransitivityReplacement(JkTransitivity formerTransitivity, JkTransitivity newTransitivity) {
        final List<JkDependency> list = new LinkedList<>();
        for (JkDependency dep : this.entries) {
            if (dep instanceof JkCoordinateDependency) {
                JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) dep;
                if (Objects.equals(coordinateDependency.getTransitivity(), formerTransitivity)) {
                    dep = coordinateDependency.withTransitivity(newTransitivity);
                }
            }
            list.add(dep);
        }
        return new JkDependencySet(list, this.globalExclusions, this.versionProvider);
    }

    public JkDependencySet withTransitivity(String moduleId, JkTransitivity newTransitivity) {
        final List<JkDependency> list = new LinkedList<>();
        for (JkDependency dep : this.entries) {
            if (dep instanceof JkCoordinateDependency) {
                JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) dep;
                if (JkCoordinate.GroupAndName.of(moduleId).equals(coordinateDependency.getCoordinate().getGroupAndName())) {
                    dep = coordinateDependency.withTransitivity(newTransitivity);
                }
            }
            list.add(dep);
        }
        return new JkDependencySet(list, this.globalExclusions, this.versionProvider);
    }

    public JkDependencySet mergeLocalProjectExportedDependencies() {
        List<JkDependency> result = new LinkedList<>();
        JkVersionProvider mergedVersionProvider = this.versionProvider;
        for (JkDependency dependency : entries) {
            if (dependency instanceof JkLocalProjectDependency) {
                JkLocalProjectDependency localProjectDependency = (JkLocalProjectDependency) dependency;
                result.add(localProjectDependency.withoutExportedDependencies());
                JkDependencySet exportedDependencies = localProjectDependency.getExportedDependencies();
                JkDependencySet recursiveExportedDependencies =
                        exportedDependencies.mergeLocalProjectExportedDependencies();
                mergedVersionProvider = recursiveExportedDependencies.versionProvider.and(mergedVersionProvider);
                for (JkDependency exportedDependency : recursiveExportedDependencies.entries) {
                    JkDependency matchedDependency = getMatching(exportedDependency);
                    if (matchedDependency == null) {
                        result.add(exportedDependency);
                    }
                }
            } else {
                result.add(dependency);
            }
        }
        return new JkDependencySet(result, this.globalExclusions, mergedVersionProvider);
    }

    /**
     * Returns a clone of this object but using specified version provider to override
     * versions of transitive dependencies. The previous version provider is replaced
     * by the specified one, there is no addition.
     */
    public JkDependencySet withVersionProvider(JkVersionProvider versionProvider) {
        return new JkDependencySet(this.entries, this.getGlobalExclusions(), versionProvider);
    }

    /**
     * Returns a clone of this object but using this version provider with bom resolved.
     */
    public JkDependencySet withResolvedBoms(JkRepoSet repos) {
        return withVersionProvider(this.versionProvider.withResolvedBoms(repos));
    }

    /**
     * @param dependencyDescription Can be expressed as group:name::pom:version
     * or group:name:version. In last case, it will be converted in the first expression
     */
    public JkDependencySet andBom(String dependencyDescription) {
        return withVersionProvider(this.versionProvider.andBom(dependencyDescription));
    }

    /**
     * Returns a clone of this object but using specified version provider to override
     * versions of transitive dependencies. The specified version provider is added
     * to the specified one.
     */
    public JkDependencySet andVersionProvider(JkVersionProvider versionProvider) {
        return new JkDependencySet(this.entries, this.getGlobalExclusions(), this.versionProvider.and(versionProvider));
    }

    /**
     * Returns <code>true</code> if this object contains dependencies whose are
     * {@link JkModuleDependency}.
     */
    public boolean hasModules() {
        return entries.stream().filter(JkCoordinateDependency.class::isInstance).findAny().isPresent();
    }

    /**
     * Returns overridden versions for transitive dependencies and direct dependencies with no version specified on. <p>
     * Versions present here will overwrite versions found in transitive dependencies and un-versioned direct
     * dependencies. <p>
     * Versions present in direct dependencies won't be overridden.
     */
    public JkVersionProvider getVersionProvider() {
        return this.versionProvider;
    }

    @Override
    public String toString() {
        return entries.toString();
    }

    /**
     * Returns the {@link JkDependency} declared for the specified
     * {@link JkModuleId}. Returns <code>null</code> if no dependency on this
     * module exists in this object.
     */
    public JkCoordinateDependency get(String moduleId) {
        return moduleDeps()
                .filter(dep -> dep.getCoordinate().getGroupAndName().toString().equals(moduleId))
                .findFirst().orElse(null);
    }

    public <T extends JkDependency> T getMatching(T dependency) {
        return (T) this.entries.stream()
                .filter(dep -> dep.matches(dependency))
                .findFirst().orElse(null);
    }

    public List<JkCoordinateDependency> getCoordinateDependencies() {
        return moduleDeps().collect(Collectors.toList());
    }

    private Stream<JkCoordinateDependency> moduleDeps() {
        return this.entries.stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .map(JkCoordinateDependency.class::cast);
    }

    /**
     * Returns <code>true</code> if this object contains dependency on external
     * module whose rely on dynamic version. It can be either dynamic version as
     * "1.3.+", "[1.0, 2.0[" ,... or snapshot version as defined in Maven (as
     * "1.0-SNAPSHOT").
     */
    public boolean hasDynamicVersions() {
        return moduleDeps().anyMatch(dep -> dep.getCoordinate().getVersion().isDynamic());
    }

    /**
     * Returns <code>true</code> if this object contains dependency on external
     * module whose rely on dynamic version that are resolvable (Maven Snapshot
     * versions are dynamic but not resolvable). It only stands for dynamic
     * versions as "1.3.+", "[1.0, 2.0[" ,... If so, when resolving, dynamic
     * versions are replaced by fixed resolved ones.
     */
    public boolean hasDynamicAndResolvableVersions() {
        return moduleDeps().anyMatch(dep -> dep.getCoordinate().getVersion().isDynamicAndResolvable());
    }



    public JkDependencySet withIdeProjectDir(Path ideProjectDir) {
        List<JkDependency> result = new LinkedList<>();
        for (JkDependency dependency : this.entries) {
            result.add(dependency.withIdeProjectDir(ideProjectDir));
        }
        return new JkDependencySet(result, globalExclusions, versionProvider);
    }

    public JkDependencySet minusModuleDependenciesHavingIdeProjectDir() {
        List<JkDependency> result = new LinkedList<>();
        for (JkDependency dependency : this.entries) {
            if (dependency.getIdeProjectDir() == null || !(dependency instanceof JkCoordinateDependency)) {
                result.add(dependency);
            }
        }
        return new JkDependencySet(result, globalExclusions, versionProvider);
    }

    public JkDependencySetMerge merge(JkDependencySet other) {
        return JkDependencySetMerge.of(this, other);
    }

    public Set<Path> getIdePathDirs() {
        Set<Path> result = new LinkedHashSet<>();
        for (JkDependency dependency : this.entries) {
            if (dependency.getIdeProjectDir() != null) {
                result.add(dependency.getIdeProjectDir());
            }
        }
        return result;
    }

    /**
     * Removes duplicates and select a version according the specified strategy in
     * case of duplicate with distinct versions.
     */
    public JkDependencySet normalised(JkCoordinate.ConflictStrategy conflictStrategy) {
        Map<JkCoordinate.GroupAndName, JkVersion> moduleIdVersionMap = new HashMap<>();
        entries.stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .map(JkCoordinateDependency.class::cast)
                .map(JkCoordinateDependency::getCoordinate)
                .forEach(coordinate -> {
                    moduleIdVersionMap.putIfAbsent(coordinate.getGroupAndName(), coordinate.getVersion());
                    moduleIdVersionMap.computeIfPresent(coordinate.getGroupAndName(),
                            (groupAndName, version) -> groupAndName.toCoordinate(version)
                                    .resolveConflict(coordinate.getVersion(), conflictStrategy).getVersion());
        });
        List<JkDependency> result = entries.stream()
                .map(dependency -> {
                    if (dependency instanceof JkCoordinateDependency) {
                        JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) dependency;
                        JkCoordinate coordinate = coordinateDependency.getCoordinate();
                        JkVersion newVersion = moduleIdVersionMap.get(coordinate.getGroupAndName());
                        JkCoordinate newCoordinate = coordinate.withVersion(newVersion);
                        return JkCoordinateDependency.of(newCoordinate)
                                .andExclusions(coordinateDependency.getExclusions())
                                .withTransitivity(coordinateDependency.getTransitivity())
                                .withIdeProjectDir(coordinateDependency.getIdeProjectDir());
                    }
                    return dependency;
                })
                .collect(Collectors.toList());
        //List<JkDependency> cleanedResult = result.stream().distinct().collect(Collectors.toList());
        return new JkDependencySet(result, this.globalExclusions, this.versionProvider);
    }

    public JkDependencySet normalised() {
        return normalised(JkCoordinate.ConflictStrategy.FAIL);
    }

    /**
     * Throws a <code>IllegalStateException</code> if one of the module dependencies has an unspecified version.
     */
    public JkDependencySet assertNoUnspecifiedVersion() {
        final List<JkCoordinateDependency> unspecifiedVersionModules = getCoordinateDependencies().stream()
                .filter(dep -> this.versionProvider.getVersionOfOrUnspecified(
                                    dep.getCoordinate().getGroupAndName()).isUnspecified())
                .filter(dep -> dep.getCoordinate().getVersion().isUnspecified())
                .collect(Collectors.toList());
        JkUtilsAssert.state(unspecifiedVersionModules.isEmpty(), "Following module does not specify version : "
                + unspecifiedVersionModules);
        return this;
    }

    /**
     * Fills the dependencies without specified version with the version supplied by the {@link JkVersionProvider}.
     */
    public JkDependencySet toResolvedModuleVersions() {
        List<JkDependency> dependencies = entries.stream()
                .map(dep -> {
                    if (dep instanceof JkCoordinateDependency) {
                        JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) dep;
                        JkCoordinate coordinate = coordinateDependency.getCoordinate();
                        JkVersion providedVersion = this.versionProvider
                                .getVersionOfOrUnspecified(coordinate.getGroupAndName());
                        if (coordinate.getVersion().isUnspecified() && !providedVersion.isUnspecified()) {
                            return coordinateDependency.withVersion(providedVersion);
                        }
                    }
                    return dep;
                })
                .collect(Collectors.toList());
        return new JkDependencySet(dependencies, this.globalExclusions, this.versionProvider);
    }

    /**
     * Returns all dependencies, adding <code>versionProvider</code> versions to module dependencies
     * that does not specify one.
     */
    public List<JkDependency> getVersionedDependencies() {
        return entries.stream()
                .map(versionProvider::version)
                .collect(Collectors.toList());
    }

    public List<JkCoordinateDependency> getVersionResolvedCoordinateDependencies() {
        return getVersionedDependencies().stream()
                .filter(JkCoordinateDependency.class::isInstance)
                .map(JkCoordinateDependency.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Returns all dependencies declared as {@link JkModuleDependency}.
     */
    public JkDependencySet withModuleDependenciesOnly() {
        final List<JkDependency> result = moduleDeps().collect(Collectors.toList());
        return new JkDependencySet(result, globalExclusions, versionProvider);
    }

    /**
     * Returns a clone of this dependencySet but adding dependency exclusion on the last element.
     */
    public JkDependencySet withLocalExclusions(JkDependencyExclusion... exclusions) {
        if (entries.isEmpty()) {
            return this;
        }
        final LinkedList<JkDependency> deps = new LinkedList<>(entries);
        final JkDependency last = deps.getLast();
        if (last instanceof JkCoordinateDependency) {
            JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) last;
            for (JkDependencyExclusion exclusion : exclusions) {
                coordinateDependency = coordinateDependency.andExclusions(exclusion);
            }
            deps.removeLast();
            deps.add(coordinateDependency);
            return new JkDependencySet(deps, globalExclusions, versionProvider);
        }
        return this;
    }

    /**
     * @param groupAndNames moduleIds to exclude (e.g. "a.group:a.name", "another.group:another.name", ...).
     * @See #withLocalExclusion
     */
    public JkDependencySet withLocalExclusions(String... groupAndNames) {
        JkDependencyExclusion[] excludes = Arrays.stream(groupAndNames).map(JkDependencyExclusion::of)
                .toArray(JkDependencyExclusion[]::new);
        return withLocalExclusions(excludes);
    }

    /**
     * Returns the dependencies to be excluded to the transitive chain when using this dependency.
     */
    public Set<JkDependencyExclusion> getGlobalExclusions() {
        return this.globalExclusions;
    }

    public JkDependencySet andGlobalExclusion(JkDependencyExclusion exclusion) {
        final Set<JkDependencyExclusion> depExclusion = new HashSet<>(this.globalExclusions);
        depExclusion.add(exclusion);
        return new JkDependencySet(this.entries, depExclusion, this.versionProvider);
    }

    public JkDependencySet andGlobalExclusion(String groupAndName) {
        JkDependencyExclusion depExclusion = JkDependencyExclusion.of(groupAndName);
        return andGlobalExclusion(depExclusion);
    }

    public JkDependencySet withGlobalExclusion(Set<JkDependencyExclusion> excludes) {
        final Set<JkDependencyExclusion> depExcludes = new HashSet<>(excludes);
        return new JkDependencySet(this.entries, Collections.unmodifiableSet(depExcludes), this.versionProvider);
    }

    /**
     * Returns the java codes that declare these dependencies.
     */
    public static String toJavaCode(int indentCount, List<JkDependency> dependencies, boolean and) {
        String method = and ? "and" : "minus";
        final String indent = JkUtilsString.repeat(" ", indentCount);
        final StringBuilder builder = new StringBuilder();
        for (final JkDependency dependency : dependencies) {
            if (dependency instanceof JkCoordinateDependency) {
                final JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) dependency;
                JkCoordinate coordinate = coordinateDependency.getCoordinate();
                String dependencyString = coordinate.getGroupAndName().getColonNotation();
                if (and && !coordinate.getVersion().isUnspecified()) {
                    dependencyString = dependencyString + ":" + coordinate.getVersion().getValue();
                }
                builder.append(indent).append(".").append(method).append("(\"")
                        .append(dependencyString)
                        .append('"')
                        .append(")\n");
            }
        }
        return builder.toString();
    }

    public static class Hint {

        private final JkDependency before;

        private final boolean condition;

        private final boolean first;

        private Hint(JkDependency before, boolean condition, boolean first) {
            this.before = before;
            this.condition = condition;
            this.first = first;
        }

        public static Hint before(JkDependency dependency) {
            return new Hint(dependency, true, false);
        }

        public static Hint firstAndIf(boolean condition) {
            return new Hint(null, condition, true);
        }

        public static Hint first() {
            return firstAndIf(true);
        }

        public static Hint lastAndIf(boolean condition) {
            return new Hint(null, true, false);
        }

        public static Hint beforeAndIf(JkDependency dependency, boolean condition) {
            return new Hint(dependency, condition, false);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Hint hint = (Hint) o;

            if (condition != hint.condition) return false;
            if (first != hint.first) return false;
            return before != null ? before.equals(hint.before) : hint.before == null;
        }

        @Override
        public int hashCode() {
            int result = before != null ? before.hashCode() : 0;
            result = 31 * result + (condition ? 1 : 0);
            result = 31 * result + (first ? 1 : 0);
            return result;
        }
    }
}
