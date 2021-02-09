package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A set of {@link JkDependency} generally standing for a given purpose (compile, test, runtime) in a project build. <p>
 * {@link JkDependencySet} also holds a {@link JkVersionProvider} and a set of JkDepe
 *
 *
 * @author Jerome Angibaud.
 */
public class JkDependencySet {

    private final List<JkDependency> dependencies;

    private final Set<JkDependencyExclusion> globalExclusions;

    private final JkVersionProvider versionProvider;

    private JkDependencySet(List<JkDependency> dependencies, Set<JkDependencyExclusion> excludes, JkVersionProvider explicitVersions) {
        super();
        this.dependencies = Collections.unmodifiableList(dependencies);
        this.globalExclusions = Collections.unmodifiableSet(excludes);
        this.versionProvider = explicitVersions;
    }

    public static JkDependencySet of(String dependencyDesc) {
        final JkDependency dependency;
        if (JkModuleDependency.isModuleDependencyDescription(dependencyDesc)) {
            dependency = JkModuleDependency.of(dependencyDesc);
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
    public static JkDependencySet of(List<JkDependency> dependencies) {
        return new JkDependencySet(dependencies, Collections.emptySet(), JkVersionProvider.of());
    }

    public static JkDependencySet of(JkDependency dependency) {
        return of(JkUtilsIterable.listOf(dependency));
    }

    /**
     * Returns the unmodifiable list list of scoped dependencies for this object.
     */
    public List<JkDependency> getDependencies() {
        return this.dependencies;
    }

    public JkVersion getVersion(JkModuleId moduleId) {
        JkModuleDependency moduleDependency = moduleDeps()
                .filter(dep -> moduleId.equals(dep.getModuleId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No module " + moduleId
                        + " declared in this dependency set " + this.withModuleDependenciesOnly()));
        JkVersion version = moduleDependency.getVersion();
        if (!version.isUnspecified()) {
            return version;
        }
        return version == null ? JkVersion.UNSPECIFIED : version;
    }

    /**
     * Returns a clone of this object plus the specifies {@link JkDependency}s, at the
     * specified place and condition.
     */
    public JkDependencySet and(Hint hint, List<JkDependency> others) {
        final List<JkDependency> result = new LinkedList<>(this.dependencies);
        if (hint == null) {
            result.addAll(others);
            return new JkDependencySet(result, globalExclusions, versionProvider);
        }
        if (hint.condition == false) {
            return this;
        }
        if (hint.first == false) {
            result.addAll(0, others);
            return new JkDependencySet(result, globalExclusions, versionProvider);
        }
        if (hint.before == null) {
            result.addAll(others);
            return new JkDependencySet(result, globalExclusions, versionProvider);
        }
        int index = result.indexOf(hint.before);
        if (index == -1) {
            throw new IllegalArgumentException("No dependency " + hint.before + " found on " + result);
        }
        result.addAll(index, others);
        return new JkDependencySet(result, globalExclusions, versionProvider);
    }

    /**
     * Returns a clone of this object plus the specified {@link JkDependency}s at the tail of
     * this one.
     */
    public JkDependencySet and(List<JkDependency> others) {
        return and(null, others);
    }

    public JkDependencySet and(JkDependencySet other) {
        final List<JkDependency> deps = JkUtilsIterable.concatLists(this.dependencies, other.dependencies);
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
        return and(hint, JkModuleDependency.of(moduleDescriptor));
    }

    public JkDependencySet and(Hint hint, JkModuleId moduleId) {
        return and(hint, moduleId.toString());
    }

    public JkDependencySet and(JkModuleId moduleId) {
        return and(null, moduleId.toString());
    }

    public JkDependencySet and(String moduleDescriptor) {
        return and(null, moduleDescriptor);
    }

    public JkDependencySet and(Hint hint, String moduleDescriptor, JkTransitivity transitivity) {
        return and(hint, JkModuleDependency.of(moduleDescriptor).withTransitivity(transitivity));
    }

    public JkDependencySet and(String moduleDescriptor, JkTransitivity transitivity) {
        return and(null, moduleDescriptor, transitivity);
    }

    public JkDependencySet andFiles(Hint hint, Iterable<Path> paths) {
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
        List<JkDependency> result = new LinkedList<>(dependencies);
        result.removeAll(dependencies);
        return new JkDependencySet(result, this.globalExclusions, this.versionProvider);
    }

    public JkDependencySet minus(JkDependency dependency) {
        List<JkDependency> list = JkUtilsIterable.listOf(dependency);
        return minus(list);
    }

    public JkDependencySet minus(JkModuleId moduleId) {
        return minus(JkModuleDependency.of(moduleId, JkVersion.UNSPECIFIED));
    }

    public JkDependencySet minus(String moduleId) {
        return minus(JkModuleId.of(moduleId));
    }

    public JkDependencySet withTransitivityReplacement(JkTransitivity formerTransitivity, JkTransitivity newTransitivity) {
        final List<JkDependency> list = new LinkedList<>();
        for (JkDependency dep : this.dependencies) {
            if (dep instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) dep;
                if (Objects.equals(moduleDependency.getTransitivity(), formerTransitivity)) {
                    dep = moduleDependency.withTransitivity(newTransitivity);
                }
            }
            list.add(dep);
        }
        return new JkDependencySet(list, this.globalExclusions, this.versionProvider);
    }

    /**
     * Returns a clone of this object but using specified version provider to override
     * versions of transitive dependencies. The previous version provider is replaced
     * by the specified one, there is no addition.
     */
    public JkDependencySet withVersionProvider(JkVersionProvider versionProvider) {
        return new JkDependencySet(this.dependencies, this.getGlobalExclusions(), versionProvider);
    }

    /**
     * Returns a clone of this object but using specified version provider to override
     * versions of transitive dependencies. The specified version provider is added
     * to the specified one.
     */
    public JkDependencySet andVersionProvider(JkVersionProvider versionProvider) {
        return new JkDependencySet(this.dependencies, this.getGlobalExclusions(), this.versionProvider.and(versionProvider));
    }

    /**
     * Returns <code>true</code> if this object contains dependencies whose are
     * {@link JkModuleDependency}.
     */
    public boolean hasModules() {
        return dependencies.stream().filter(JkModuleDependency.class::isInstance).findAny().isPresent();
    }

    /**
     * Returns overridden versions for transitive dependencies and direct dependencies with no version specified on. <p>
     * Versions present here will overwrite versions found in transitive dependencies and unversioned direct
     * dependencies. <p>
     * Versions present in direct dependencies won't be overridden.
     */
    public JkVersionProvider getVersionProvider() {
        return this.versionProvider;
    }

    @Override
    public String toString() {
        return dependencies.toString();
    }

    /**
     * Returns the {@link JkDependency} declared for the specified
     * {@link JkModuleId}. Returns <code>null</code> if no dependency on this
     * module exists in this object.
     */
    public JkDependency get(JkModuleId moduleId) {
        return moduleDeps()
                .filter(dep -> dep.getModuleId().equals(moduleId))
                .findFirst().orElse(null);
    }

    public List<JkModuleDependency> getModuleDependencies() {
        return moduleDeps().collect(Collectors.toList());
    }

    private Stream<JkModuleDependency> moduleDeps() {
        return this.dependencies.stream()
                .filter(JkModuleDependency.class::isInstance)
                .map(JkModuleDependency.class::cast);
    }

    /**
     * Returns <code>true</code> if this object contains dependency on external
     * module whose rely on dynamic version. It can be either dynamic version as
     * "1.3.+", "[1.0, 2.0[" ,... or snapshot version as defined in Maven (as
     * "1.0-SNAPSHOT).
     */
    public boolean hasDynamicVersions() {
        return moduleDeps().anyMatch(dep -> dep.getVersion().isDynamic());
    }

    /**
     * Returns <code>true</code> if this object contains dependency on external
     * module whose rely on dynamic version that are resolvable (Maven Snapshot
     * versions are dynamic but not resolvable). It only stands for dynamic
     * versions as "1.3.+", "[1.0, 2.0[" ,... If so, when resolving, dynamic
     * versions are replaced by fixed resolved ones.
     */
    public boolean hasDynamicAndResolvableVersions() {
        return moduleDeps().anyMatch(dep -> dep.getVersion().isDynamicAndResovable());
    }



    public JkDependencySet withIdeProjectDir(Path ideProjectDir) {
        List<JkDependency> result = new LinkedList<>();
        for (JkDependency dependency : this.dependencies) {
            result.add(dependency.withIdeProjectDir(ideProjectDir));
        }
        return new JkDependencySet(result, globalExclusions, versionProvider);
    }

    public JkDependencySet minusModuleDependenciesHavingIdeProjectDir() {
        List<JkDependency> result = new LinkedList<>();
        for (JkDependency dependency : this.dependencies) {
            if (dependency.getIdeProjectDir() == null || !(dependency instanceof JkModuleDependency)) {
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
        for (JkDependency dependency : this.dependencies) {
            if (dependency.getIdeProjectDir() != null) {
                result.add(dependency.getIdeProjectDir());
            }
        }
        return result;
    }

    /**
     * Removes duplicates and select a versoin according the specified strayegy in
     * case of duplicate with distinct versions.
     */
    public JkDependencySet normalised(JkVersionedModule.ConflictStrategy conflictStrategy) {
        Map<JkModuleId, JkVersion> moduleIdVersionMap = new HashMap<>();
        dependencies.stream()
                .filter(JkModuleDependency.class::isInstance)
                .map(JkModuleDependency.class::cast)
                .forEach(dep -> {
                    moduleIdVersionMap.putIfAbsent(dep.getModuleId(),dep.getVersion());
                    moduleIdVersionMap.computeIfPresent(dep.getModuleId(),
                            (moduleId, version) -> JkVersionedModule.of(moduleId, version)
                                    .resolveConflict(dep.getVersion(), conflictStrategy).getVersion());
        });
        List<JkDependency> result = dependencies.stream()
                .map(dependency -> {
                    if (dependency instanceof JkModuleDependency) {
                        JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                        return moduleDependency.withVersion(moduleIdVersionMap.get(moduleDependency.getModuleId()));
                    }
                    return dependency;
                })
                .collect(Collectors.toList());
        List<JkDependency> cleanedResult = result.stream().distinct().collect(Collectors.toList());
        return new JkDependencySet(cleanedResult, this.globalExclusions, this.versionProvider);
    }

    public JkDependencySet normalised() {
        return normalised(JkVersionedModule.ConflictStrategy.FAIL);
    }

    /**
     * Throws a <code>IllegalStateException</code> if one of the module dependencies has an unspecified version.
     */
    public JkDependencySet assertNoUnspecifiedVersion() {
        final List<JkModuleDependency> unspecifieds = getVersionedDependencies().stream()
                .filter(JkModuleDependency.class::isInstance)
                .map(JkModuleDependency.class::cast)
                .filter(dep -> dep.getVersion().isUnspecified())
                .collect(Collectors.toList());
        JkUtilsAssert.state(unspecifieds.isEmpty(), "Following module does not specify projectVersion : "
                + unspecifieds);
        return this;
    }

    /**
     * Returns all dependencies, adding <code>versionProvider</code> versions to module dependencies
     * that does not specify one.
     */
    public List<JkDependency> getVersionedDependencies() {
        return dependencies.stream()
                .map(dependency -> {
                    if (dependency instanceof JkModuleDependency) {
                        JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                        JkVersion providedVersion = versionProvider.getVersionOf(moduleDependency.getModuleId());
                        if (moduleDependency.getVersion().isUnspecified() && providedVersion != null) {
                            return moduleDependency.withVersion(providedVersion);
                        }
                    }
                    return dependency;
                })
                .collect(Collectors.toList());
    }

    public List<JkModuleDependency> getVersionedModuleDependencies() {
        return getVersionedDependencies().stream()
                .filter(JkModuleDependency.class::isInstance)
                .map(JkModuleDependency.class::cast)
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
     * Returns a clone of this dependencySet but adding dependency exclusion on the the last element.
     */
    public JkDependencySet withLocalExclusion(JkDependencyExclusion... exclusions) {
        if (dependencies.isEmpty()) {
            return this;
        }
        final LinkedList<JkDependency> deps = new LinkedList<>(dependencies);
        final JkDependency last = deps.getLast();
        if (last instanceof JkModuleDependency) {
            JkModuleDependency moduleDependency = (JkModuleDependency) last;
            for (JkDependencyExclusion exclusion : exclusions) {
                moduleDependency = moduleDependency.andExclude(exclusion);
            }
            deps.removeLast();
            deps.add(moduleDependency);
            return new JkDependencySet(deps, globalExclusions, versionProvider);
        }
        return this;
    }

    /**
     * @param groupAndNames moduleIds to exclude (e.g. "a.group:a.name", "another.group:another.name", ...).
     * @See #withLocalExclusion
     */
    public JkDependencySet withLocalExclusions(String... groupAndNames) {
        JkDependencyExclusion[] excludes = Arrays.stream(groupAndNames).map(JkDependencySet::of)
                .toArray(JkDependencyExclusion[]::new);
        return withLocalExclusion(excludes);
    }

    /**
     * Returns the dependencies to be excluded to the transitive chain when using this dependency.
     */
    public Set<JkDependencyExclusion> getGlobalExclusions() {
        return this.globalExclusions;
    }

    public JkDependencySet andGlobalExclusion(JkDependencyExclusion exclude) {
        final Set<JkDependencyExclusion> depExcludes = new HashSet<>(this.globalExclusions);
        depExcludes.add(exclude);
        return new JkDependencySet(this.dependencies, depExcludes, this.versionProvider);
    }

    public JkDependencySet withGlobalExclusion(Set<JkDependencyExclusion> excludes) {
        final Set<JkDependencyExclusion> depExcludes = new HashSet<>(excludes);
        return new JkDependencySet(this.dependencies, Collections.unmodifiableSet(depExcludes), this.versionProvider);
    }

    /**
     * Returns the java codes that declare these dependencies.
     */
    public static String toJavaCode(int indentCount, List<JkDependency> dependencies) {
        final String indent = JkUtilsString.repeat(" ", indentCount);
        final StringBuilder builder = new StringBuilder();
        builder.append("JkDependencySet.of()");
        for (final JkDependency dependency : dependencies) {
            if (dependency instanceof JkModuleDependency) {
                final JkModuleDependency moduleDep = (JkModuleDependency) dependency;
                builder.append("\n").append(indent).append(".and(\"")
                .append(moduleDep.getModuleId().getGroup()).append(":")
                .append(moduleDep.getModuleId().getName());
                if (!moduleDep.getVersion().isUnspecified()) {
                    builder.append(":" + moduleDep.getVersion().getValue());
                }
                builder.append('"');
                builder.append(")");
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
            return new Hint(null, true, true);
        }

        public static Hint lastAndIf(boolean condition) {
            return new Hint(null, true, false);
        }

        public static Hint beforeAndIf(JkDependency dependency, boolean condition) {
            return new Hint(dependency, condition, false);
        }

    }
}
