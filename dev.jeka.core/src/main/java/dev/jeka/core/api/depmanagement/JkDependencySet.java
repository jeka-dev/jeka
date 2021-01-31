package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A set of {@link JkDependency} generally standing for the entire
 * dependencies of a project/module.
 *
 * @author Jerome Angibaud.
 */
public class JkDependencySet {

    private final List<JkDependency> dependencies;

    private final Set<JkDepExclude> globalExclusions;

    private final JkVersionProvider versionProvider;

    private JkDependencySet(Iterable<JkDependency> dependencies, Set<JkDepExclude> excludes, JkVersionProvider explicitVersions) {
        super();
        this.dependencies = Collections.unmodifiableList(JkUtilsIterable.listOf(dependencies));
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
    public static JkDependencySet of(Iterable<JkDependency> dependencies) {
        return new JkDependencySet(dependencies, Collections.emptySet(), JkVersionProvider.of());
    }

    public static JkDependencySet of(JkDependency dependency) {
        return of(Collections.singleton(dependency));
    }

    /**
     * Creates a {@link JkDependencySet} based on jars located under the specified directory. Jars are
     * supposed to lie in a directory structure standing for the different scopes they are intended.
     * So jars needed for compilation are supposed to be in <code>baseDir/compile</code>, jar needed for
     * test are supposed to be in <code>baseDir/test</code> and so on.
     */
    public static JkDependencySet ofLocal(Path baseDir) {
        final JkPathTree libDir = JkPathTree.of(baseDir);
        if (!libDir.exists()) {
            return JkDependencySet.of();
        }
        return JkDependencySet.of()
                .and(JkFileSystemDependency.of(libDir.andMatching(true, "*.jar", "compile/*.jar").getFiles()))
                .and(JkFileSystemDependency.of(libDir.andMatching(true,"compile+runtime/*.jar").getFiles()))
                .and(JkFileSystemDependency.of(libDir.andMatching(true,"runtime/*.jar").getFiles()))
                .and(JkFileSystemDependency.of(libDir.andMatching(true,"test/*.jar").getFiles()));
    }


    /**
     * Returns the unmodifiable list list of scoped dependencies for this object.
     */
    public List<JkDependency> toList() {
        return this.dependencies;
    }

    public JkVersion getVersion(JkModuleId moduleId) {
        final JkDependency dep = this.get(moduleId);
        if (dep == null) {
            throw new IllegalArgumentException("No module " + moduleId + " declared in this dependency set " + this.withModulesOnly());
        }
        final JkModuleDependency moduleDependency = (JkModuleDependency) dep;
        JkVersion version = moduleDependency.getVersion();
        if (!version.isUnspecified()) {
            return version;
        }
        version =  this.versionProvider.getVersionOf(moduleId);
        if (version != null) {
            return version;
        }
        return JkVersion.UNSPECIFIED;
    }

    /**
     * Returns a clone of this object plus the specified
     * {@link JkDependency}s.
     */
    public JkDependencySet and(Iterable<JkDependency> others) {
        final List<JkDependency> deps = JkUtilsIterable.concatLists(this.dependencies, others);
        return new JkDependencySet(deps, globalExclusions, versionProvider);
    }

    public JkDependencySet and(JkDependencySet other) {
        final List<JkDependency> deps = JkUtilsIterable.concatLists(this.dependencies, other.dependencies);
        Set<JkDepExclude> newGlobalExcludes = new HashSet<>(this.globalExclusions);
        newGlobalExcludes.addAll(other.globalExclusions);
        JkVersionProvider newVersionProvider = this.versionProvider.and(other.versionProvider);
        return new JkDependencySet(deps, newGlobalExcludes, newVersionProvider);
    }

    /**
     * Returns a clone of this object plus the specified scoped dependencies.
     */
    public JkDependencySet and(JkDependency... others) {
        return and(Arrays.asList(others));
    }


    public JkDependencySet and(String moduleDescription, JkTransitivity transitivity) {
        JkModuleDependency moduleDependency = JkModuleDependency.of(moduleDescription).withTransitivity(transitivity);
        if (moduleDependency.getClassifier() != null) {
            moduleDependency = moduleDependency.withTransitivity(JkTransitivity.RUNTIME);
        }
        return and(moduleDependency);
    }

    public JkDependencySet and(String moduleDescription) {
        return and(moduleDescription, null);
    }

    public JkDependencySet and(JkModuleId moduleId, String version, JkTransitivity transitivity) {
        return and(JkModuleDependency.of(moduleId, version).withTransitivity(transitivity));
    }

    public JkDependencySet and(JkModuleId moduleId, String version) {
        return and(moduleId, version, (JkTransitivity) null);
    }

    public JkDependencySet andFiles(Iterable<Path> paths) {
        return and(JkFileSystemDependency.of(paths));
    }

    public JkDependencySet andFiles(String ...paths) {
        return andFiles(Stream.of(paths).map(Paths::get).collect(Collectors.toList()));
    }

    public JkDependencySet andFiles(Path ... paths) {
        return andFiles(Arrays.asList(paths));
    }

    /**
     * Returns a dependency set identical to this one minus the dependencies on the given
     * {@link JkModuleId}. This is used to exclude a given module to all scope.
     */
    public JkDependencySet minus(JkModuleId jkModuleId) {
        final List<JkDependency> result = new LinkedList<>(dependencies);
        for (final Iterator<JkDependency> it = result.iterator(); it.hasNext();) {
            final JkDependency dependency = it.next();
            if (dependency instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) dependency;
                if (externalModule.getModuleId().equals(jkModuleId)) {
                    it.remove();
                }
            }
        }
        return new JkDependencySet(result, this.globalExclusions, this.versionProvider);
    }

    /**
     * Same as #minus(JkModuleId) but using a string as <code>my.group:my.name</code> to specify the moduleId.
     */
    public JkDependencySet minus(String moduleId) {
        return minus(JkModuleId.of(moduleId));
    }

    /**
     * Returns a dependency set identical to this one minus the dependencies on the given
     * {@link JkModuleId}. This is used to exclude a given module to all scope.
     */
    public JkDependencySet minusFiles(Predicate<Path> pathPredicate) {
        final List<JkDependency> result = new LinkedList<>();
        for (JkDependency dependency : this.dependencies) {
            if (dependency instanceof JkFileSystemDependency) {
                JkFileSystemDependency fileDependency = (JkFileSystemDependency) dependency;
                JkFileSystemDependency resultDependency = fileDependency;
                for (Path path : fileDependency.getFiles()) {
                    if (pathPredicate.test(path)) {
                        resultDependency = resultDependency.minusFile(path);
                    }
                }
                if (!resultDependency.getFiles().isEmpty()) {
                    result.add(resultDependency);
                }
            } else {
                result.add(dependency);
            }
        }
        return new JkDependencySet(result, this.globalExclusions, this.versionProvider);
    }

    /**
     * Returns a clone of this dependencySet but removing the last element if the specified condition is met.
     */
    public JkDependencySet minusLastIf(boolean condition) {
        if (!condition) {
            return this;
        }
        final LinkedList<JkDependency> deps = new LinkedList<>(dependencies);
        deps.removeLast();
        return new JkDependencySet(deps, globalExclusions, versionProvider);
    }

    /**
     * Returns a clone of this dependencySet but adding dependency exclusion on the the last element.
     */
    public JkDependencySet withLocalExclusion(JkDepExclude exclusion) {
        if (dependencies.isEmpty()) {
            return this;
        }
        final LinkedList<JkDependency> deps = new LinkedList<>(dependencies);
        final JkDependency last = deps.getLast();
        if (last instanceof JkModuleDependency) {
            JkModuleDependency moduleDependency = (JkModuleDependency) last;
            moduleDependency = moduleDependency.andExclude(exclusion);
            deps.removeLast();
            deps.add(moduleDependency);
            return new JkDependencySet(deps, globalExclusions, versionProvider);
        }
        return this;
    }

    /**
     * @See #withLocalExclusion
     * @param groupAndNames moduleIds to exclude (e.g. "a.group:a.name", "another.group:another.name", ...).
     */
    public JkDependencySet withLocalExclusions(String ... groupAndNames) {
        if (dependencies.isEmpty()) {
            return this;
        }
        final LinkedList<JkDependency> deps = new LinkedList<>(dependencies);
        final JkDependency last = deps.getLast();
        if (last instanceof JkModuleDependency) {
            JkModuleDependency moduleDependency = (JkModuleDependency) last;
            final List<JkDepExclude> excludes = new LinkedList<>();
            for (final String groupAndName : groupAndNames) {
                excludes.add(JkDepExclude.of(groupAndName));
            }
            moduleDependency = moduleDependency.andExclude(excludes);
            deps.removeLast();
            deps.add(moduleDependency);
            return new JkDependencySet(deps, globalExclusions, versionProvider);
        }
        return this;
    }

    public JkDependencySet withReplacingTransitivity(JkTransitivity formerTransitivity, JkTransitivity newTransitivity) {
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
     * Returns the dependencies to be excluded to the transitive chain when using this dependency.
     */
    public Set<JkDepExclude> getGlobalExclusions() {
        return this.globalExclusions;
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

    public JkDependencySet withGlobalExclusion(JkDepExclude exclude) {
        final Set<JkDepExclude> depExcludes = new HashSet<>(this.globalExclusions);
        depExcludes.add(exclude);
        return new JkDependencySet(this.dependencies, depExcludes, this.versionProvider);
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
            if (dependency.getIdeProjectDir() == null || ! (dependency instanceof JkModuleDependency)) {
                result.add(dependency);
            }
        }
        return new JkDependencySet(result, globalExclusions, versionProvider);
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
     * Returns a set a dependency set identical to this one but excluding the specified exclude
     * from the transitive dependencies of the specified module.
     */
    public JkDependencySet withLocalExclusion(JkModuleId fromModule, JkDepExclude exclude) {
        final List<JkDependency> list = new LinkedList<>();
        for (final JkDependency dep : this.dependencies) {
            if (dep instanceof JkModuleDependency) {
                final JkModuleDependency moduleDependency = (JkModuleDependency) dep;
                list.add(moduleDependency.andExclude(exclude));
            } else {
                list.add(dep);
            }
        }
        return new JkDependencySet(list, this.globalExclusions, this.versionProvider);
    }

    /**
     * Returns a JkDependencySet similar to this one but removing duplicates on module dependencies. Such duplicates
     * occurres when two module dependencies have been declared with the same JkModuleId. The removed dependency is
     * the one with the lower or unspecified version.
     * It has been introduced to satisfy https://github.com/jerkar/jeka/issues/135
     */
    public JkDependencySet minusDuplicates() {
        Map<JkModuleId, JkVersion> moduleIdVersionMap = new HashMap<>();
        for (JkDependency dependency : this.dependencies) {
            if (dependency instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                JkModuleId moduleId = moduleDependency.getModuleId();
                JkVersion version = moduleDependency.getVersion();
                JkVersion mapVersion = moduleIdVersionMap.get(moduleId);
                if (mapVersion == null || mapVersion.isUnspecified() || (version != null && version.isGreaterThan(mapVersion))) {
                    moduleIdVersionMap.put(moduleId, version);
                }
            }
        }
        List<JkDependency> result = new LinkedList<>();
        Set<JkModuleId> moduleIds = new HashSet<>();
        for (JkDependency dependency : this.dependencies) {
            if (dependency instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                JkModuleId moduleId = moduleDependency.getModuleId();
                if (moduleIds.contains(moduleId)) {
                    continue;
                }
                moduleIds.add(moduleId);
                JkModuleDependency replacingModuleDep = moduleDependency.withVersion(moduleIdVersionMap.get(moduleId));
                result.add(replacingModuleDep);
            } else {
                result.add(dependency);
            }
        }
        return new JkDependencySet(result, this.globalExclusions, this.versionProvider);
    }

    /**
     * Throws a <code>IllegalStateException</code> if one of the module
     * dependencies has an unspecified projectVersion.
     */
    public JkDependencySet assertNoUnspecifiedVersion() {
        final List<JkModuleDependency> unspecifieds = this.unspecifiedVersionDependencies();
        JkUtilsAssert.state(unspecifieds.isEmpty(), "Following module does not specify projectVersion : "
                + unspecifieds);
        return this;
    }

    public JkDependencySet toResolvedModuleVersions() {
        final List<JkDependency> list = new LinkedList<>();
        for (final JkDependency dep : this.dependencies) {
            if (dep instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) dep;
                if (moduleDependency.getVersion().isUnspecified()) {
                    final JkVersion providedVersion = this.versionProvider.getVersionOf(moduleDependency.getModuleId());
                    if (providedVersion != null) {
                        moduleDependency = moduleDependency.withVersion(providedVersion);
                    }
                }
                list.add(moduleDependency);
            } else {
                list.add(dep);
            }
        }
        return new JkDependencySet(list, this.globalExclusions, this.versionProvider);
    }

    private List<JkModuleDependency> unspecifiedVersionDependencies() {
        final List<JkModuleDependency> result = new LinkedList<>();
        for (final JkModuleDependency moduleDependency : this.extractModuleDependencies()) {
            if (moduleDependency.hasUnspecifedVersion()) {
                result.add(moduleDependency);
            }
        }
        return result;
    }

    /**
     * Returns the java codes that declare these dependencies.
     *
     * @formatter:off
     */
    public String toJavaCode(int indentCount) {
        final String indent = JkUtilsString.repeat(" ", indentCount);
        final StringBuilder builder = new StringBuilder();
        builder.append("JkDependencySet.of()");
        for (final JkDependency dependency : this.dependencies) {
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

    /**
     * Returns all dependencies declared as {@link JkModuleDependency}.
     */
    public JkDependencySet withModulesOnly() {
        final List<JkDependency> result = moduleDeps().collect(Collectors.toList());
        return new JkDependencySet(result, globalExclusions, versionProvider);
    }

    private List<JkModuleDependency> extractModuleDependencies() {
        return moduleDeps().collect(Collectors.toList());
    }

}
