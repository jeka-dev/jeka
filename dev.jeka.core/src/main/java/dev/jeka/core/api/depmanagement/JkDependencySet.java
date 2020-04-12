package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.JkScopedDependency.ScopeType;
import dev.jeka.core.api.file.JkFileSystemLocalizable;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.*;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

/**
 * A set of {@link JkScopedDependency} generally standing for the entire
 * dependencies of a project/module.
 *
 * @author Jerome Angibaud.
 */
public class JkDependencySet implements Iterable<JkScopedDependency> {

    private final List<JkScopedDependency> dependencies;

    private final Set<JkDepExclude> globalExclusions;

    private final JkVersionProvider versionProvider;

    private JkDependencySet(Iterable<JkScopedDependency> dependencies, Set<JkDepExclude> excludes, JkVersionProvider explicitVersions) {
        super();
        this.dependencies = Collections.unmodifiableList(JkUtilsIterable.listOf(dependencies));
        this.globalExclusions = Collections.unmodifiableSet(excludes);
        this.versionProvider = explicitVersions;
    }

    public static JkDependencySet of(String dependencyDesc, JkScope... scopes) {
        final JkDependency dependency;
        if (JkModuleDependency.isModuleDependencyDescription(dependencyDesc)) {
            dependency = JkModuleDependency.of(dependencyDesc);
        } else {
            dependency = JkFileSystemDependency.of(Paths.get(dependencyDesc));
        }
        return of(JkUtilsIterable.listOf(JkScopedDependency.of(dependency, scopes)));
    }

    public static JkDependencySet of() {
        return of(Collections.emptyList());
    }

    /**
     * Creates a {@link JkDependencySet} to the specified scoped dependencies.
     */
    public static JkDependencySet of(Iterable<JkScopedDependency> scopedDependencies) {
        return new JkDependencySet(scopedDependencies, Collections.emptySet(), JkVersionProvider.of());
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
                .withDefaultScopes(JkJavaDepScopes.COMPILE)
                .and(JkFileSystemDependency.of(libDir.andMatching(true,"provided/*.jar").getFiles()))
                .withDefaultScopes(JkJavaDepScopes.PROVIDED)
                .and(JkFileSystemDependency.of(libDir.andMatching(true,"runtime/*.jar").getFiles()))
                .withDefaultScopes(JkJavaDepScopes.RUNTIME)
                .and(JkFileSystemDependency.of(libDir.andMatching(true,"test/*.jar").getFiles()))
                .withDefaultScopes(JkJavaDepScopes.TEST);
    }


    /**
     * Returns the unmodifiable list list of scoped dependencies for this object.
     */
    public List<JkScopedDependency> toList() {
        return this.dependencies;
    }

    public JkVersion getVersion(JkModuleId moduleId) {
        final JkScopedDependency dep = this.get(moduleId);
        if (dep == null) {
            throw new IllegalArgumentException("No module " + moduleId + " declared in this dependency set " + this.withModulesOnly());
        }
        final JkModuleDependency moduleDependency = (JkModuleDependency) dep.getDependency();
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
     * {@link JkScopedDependency}s.
     */
    public JkDependencySet and(Iterable<JkScopedDependency> others) {
        if (!others.iterator().hasNext()) {
            return this;
        }
        final List<JkScopedDependency> deps = JkUtilsIterable.concatLists(this.dependencies, others);
        Set<JkDepExclude> globalExcludes = this.globalExclusions;
        JkVersionProvider versionProvider = this.versionProvider;
        if (others instanceof JkDependencySet) {
            JkDependencySet other = (JkDependencySet) others;
            globalExcludes = new HashSet<>(this.globalExclusions);
            globalExcludes.addAll(other.globalExclusions);
            versionProvider = versionProvider.and(other.versionProvider);
        }
        return new JkDependencySet(deps, globalExcludes, versionProvider);
    }

    /**
     * Returns a clone of this object plus the specified scoped dependencies.
     */
    public JkDependencySet and(JkScopedDependency... others) {
        return and(Arrays.asList(others));
    }

    /**
     * Returns a clone of this object plus the specified scoped dependencies.
     */
    public JkDependencySet and(JkDependency dependency, JkScope ... scopes) {
        return this.and(JkScopedDependency.of(dependency, scopes));
    }

    /**
     * Returns a clone of this object plus the specified scoped dependencies.
     */
    public JkDependencySet and(JkModuleDependency dependency, JkScopeMapping scopeMapping) {
        return this.and(JkScopedDependency.of(dependency, scopeMapping));
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer.
     * @param baseDir optional argument for indicating if the artifact procducer can be materialised by a
     *                project/module in a ide. Can be null.
     */
    public JkDependencySet and(JkArtifactProducer artifactProducer, Path baseDir, List<JkArtifactId> artifactFileIds, JkScope ... scopes) {
        final ArtifactProducerDependency dependency = new ArtifactProducerDependency(artifactProducer, baseDir
                ,artifactFileIds);
        final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, scopes);
        return and(scopedDependency);
    }

    private static Path baseDir(Object object) {
        if (object instanceof JkFileSystemLocalizable) {
            return ((JkFileSystemLocalizable) object).getBaseDir();
        }
        return null;
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer
     */
    public JkDependencySet and(JkArtifactProducer artifactProducer, JkScope... scopes) {
        return and(artifactProducer, baseDir(artifactProducer), Collections.emptyList(), scopes);
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer supplier
     */
    public JkDependencySet and(JkArtifactProducer.JkSupplier artifactProducerSupplier, List<JkArtifactId> artifactFileIds,
            JkScope... scopes) {
        return and(artifactProducerSupplier.getArtifactProducer(), baseDir(artifactProducerSupplier),
                artifactFileIds, scopes);
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer supplier
     */
    public JkDependencySet and(JkArtifactProducer.JkSupplier artifactProducerSupplier, JkArtifactId artifactFileIds,
            JkScope... scopes) {
        return and(artifactProducerSupplier.getArtifactProducer(), baseDir(artifactProducerSupplier),
                Arrays.asList(artifactFileIds), scopes);
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer supplier
     */
    public JkDependencySet and(JkArtifactProducer.JkSupplier artifactProducerSupplier,
            JkScope... scopes) {
        return and(artifactProducerSupplier.getArtifactProducer(), baseDir(artifactProducerSupplier),
                Collections.emptyList(), scopes);
    }

    public JkDependencySet and(String moduleDescription, JkScope ... scopes) {
        JkModuleDependency moduleDependency = JkModuleDependency.of(moduleDescription);
        if (moduleDependency.getClassifier() != null) {
            moduleDependency = moduleDependency.isTransitive(false);
        }
        return and(moduleDependency, scopes);
    }

    public JkDependencySet and(String moduleDescription, JkScopeMapping scopeMapping) {
        return and(JkModuleDependency.of(moduleDescription), scopeMapping);
    }

    public JkDependencySet and(JkModuleId moduleId, String version, JkScope ... scopes) {
        return and(JkModuleDependency.of(moduleId, version), scopes);
    }

    public JkDependencySet and(JkModuleId moduleId, JkScope ... scopes) {
        return and(JkModuleDependency.of(moduleId, JkVersion.UNSPECIFIED), scopes);
    }

    public JkDependencySet and(JkModuleId moduleId, String version, JkScopeMapping scopeMapping) {
        return and(JkModuleDependency.of(moduleId, version), scopeMapping);
    }

    public JkDependencySet andFiles(Iterable<Path> files, JkScope... scopes) {
        return and(JkFileSystemDependency.of(files), scopes);
    }


    public JkDependencySet andFile(String path, JkScope... scopes) {
        return andFile(Paths.get(path), scopes);
    }

    /**
     * If specified path is relative, JkDependencyResolver will resolve it upon its base dir.
     */
    public JkDependencySet andFile(Path file, JkScope... scopes) {
        return and(JkFileSystemDependency.of(file), scopes);
    }

    public JkDependencySet andScopelessDependencies(Iterable<? extends JkDependency> dependencies) {
        final List<JkScopedDependency> deps = new LinkedList<>(this.dependencies);
        for(final JkDependency dependency : dependencies) {
            deps.add(JkScopedDependency.of(dependency));
        }
        return new JkDependencySet(deps, this.globalExclusions, this.versionProvider);
    }

    /**
     * Returns a dependency set identical to this one minus the dependencies on the given
     * {@link JkModuleId}. This is used to exclude a given module to all scope.
     */
    public JkDependencySet minus(JkModuleId jkModuleId) {
        final List<JkScopedDependency> result = new LinkedList<>(dependencies);
        for (final Iterator<JkScopedDependency> it = result.iterator(); it.hasNext();) {
            final JkDependency dependency = it.next().getDependency();
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
     * Returns a dependency set identical to this one minus the dependencies on the given
     * {@link JkModuleId}. This is used to exclude a given module to all scope.
     */
    public JkDependencySet minusFiles(Predicate<Path> pathPredicate) {
        final List<JkScopedDependency> result = new LinkedList<>();
        for (JkScopedDependency scopedDependency : this.dependencies) {
            final JkDependency dependency = scopedDependency.getDependency();
            if (dependency instanceof JkFileSystemDependency) {
                JkFileSystemDependency fileDependency = (JkFileSystemDependency) dependency;
                JkFileSystemDependency resultDependency = fileDependency;
                for (Path path : fileDependency.getFiles()) {
                    if (pathPredicate.test(path)) {
                        resultDependency = resultDependency.minusFile(path);
                    }
                }
                if (!resultDependency.getFiles().isEmpty()) {
                    result.add(scopedDependency.withDependency(resultDependency));
                }
            } else {
                result.add(scopedDependency);
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
        final LinkedList<JkScopedDependency> deps = new LinkedList<>(dependencies);
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
        final LinkedList<JkScopedDependency> deps = new LinkedList<>(dependencies);
        final JkScopedDependency last = deps.getLast();
        if (last.getDependency() instanceof JkModuleDependency) {
            JkModuleDependency moduleDependency = (JkModuleDependency) last.getDependency();
            moduleDependency = moduleDependency.andExclude(exclusion);
            deps.removeLast();
            deps.add(last.withDependency(moduleDependency));
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
        final LinkedList<JkScopedDependency> deps = new LinkedList<>(dependencies);
        final JkScopedDependency last = deps.getLast();
        if (last.getDependency() instanceof JkModuleDependency) {
            JkModuleDependency moduleDependency = (JkModuleDependency) last.getDependency();
            final List<JkDepExclude> excludes = new LinkedList<>();
            for (final String groupAndName : groupAndNames) {
                excludes.add(JkDepExclude.of(groupAndName));
            }
            moduleDependency = moduleDependency.andExclude(excludes);
            deps.removeLast();
            deps.add(last.withDependency(moduleDependency));
            return new JkDependencySet(deps, globalExclusions, versionProvider);
        }
        return this;
    }

    /**
     * Returns a clone of this dependencies but replacing the unscoped
     * dependencies with the specified ones.
     */
    public JkDependencySet withDefaultScopes(JkScope... scopes) {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (JkScopedDependency dep : this) {
            if (dep.getScopeType().equals(ScopeType.UNSET)) {
                dep = dep.withScopes(scopes);
            }
            list.add(dep);
        }
        return new JkDependencySet(list, this.globalExclusions, this.versionProvider);
    }

    /**
     * Returns a clone of this dependencies but replacing the unscoped
     * dependencies with the specified scope mapping.
     */
    public JkDependencySet withDefaultScope(JkScopeMapping scopeMapping) {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (JkScopedDependency dep : this) {
            if (dep.getScopeType().equals(ScopeType.UNSET) && (dep.getDependency() instanceof JkModuleDependency)) {
                dep = dep.withScopeMapping(scopeMapping);
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
        for (final JkScopedDependency scopedDependency : dependencies) {
            if (scopedDependency.getDependency() instanceof JkModuleDependency) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<JkScopedDependency> iterator() {
        return dependencies.iterator();
    }

    /**
     * Returns the dependencies to be excluded to the transitive chain when using this dependency.
     */
    public Set<JkDepExclude> getGlobalExclusions() {
        return this.globalExclusions;
    }

    /**
     * Returns overridden versions for transitive dependencies. Versions present here will
     * overwrite versions found in transitive dependencies.
     */
    public JkVersionProvider getVersionProvider() {
        return this.versionProvider;
    }

    @Override
    public String toString() {
        return dependencies.toString();
    }

    /**
     * Returns the set of {@link JkDependency} involved for the specified
     * {@link JkScope}.
     */
    public Set<JkDependency> getDependenciesDeclaredWith(JkScope scope) {
        final Set<JkDependency> depList = new HashSet<>();
        for (final JkScopedDependency scopedDependency : this) {
            if ((scopedDependency.getScopeType().equals(ScopeType.SIMPLE)
                    && scopedDependency.getScopes().contains(scope))
                    ||
                    (scopedDependency.getScopeType().equals(ScopeType.MAPPED)
                            && scopedDependency.getScopeMapping().getEntries().contains(scope))) {
                depList.add(scopedDependency.getDependency());
            }
        }
        return depList;
    }

    /**
     * Returns the {@link JkScopedDependency} declared for the specified
     * {@link JkModuleId}. Returns <code>null</code> if no dependency on this
     * module exists in this object.
     */
    public JkScopedDependency get(JkModuleId moduleId) {
        for (final JkScopedDependency scopedDependency : this) {
            final JkDependency dependency = scopedDependency.getDependency();
            if (dependency instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) dependency;
                if (externalModule.getModuleId().equals(moduleId)) {
                    return scopedDependency;
                }
            }
        }
        return null;
    }

    /**
     * Returns the set of scopes involved in these dependencies.
     */
    public Set<JkScope> getDeclaredScopes() {
        final Set<JkScope> result = new HashSet<>();
        for (final JkScopedDependency dep : this.dependencies) {
            if (dep.getScopeType() == ScopeType.MAPPED) {
                result.addAll(dep.getScopeMapping().getEntries());
            } else if (dep.getScopeType() == ScopeType.SIMPLE) {
                result.addAll(dep.getScopes());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns all scopes that are involved in these dependencies. That means if one of these scoped
     * dependencies is declared with scope 'FOO' and scope 'BAR' extends scope 'FOO', then 'FOO' andPrepending 'BAR' is
     * part of involved scopes.
     */
    public Set<JkScope> getInvolvedScopes() {
        return JkScope.getInvolvedScopes(getDeclaredScopes());
    }

    /**
     * Returns <code>true</code> if this object contains dependency on external
     * module whose rely on dynamic version. It can be either dynamic version as
     * "1.3.+", "[1.0, 2.0[" ,... or snapshot version as defined in Maven (as
     * "1.0-SNAPSHOT).
     */
    public boolean hasDynamicVersions() {
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.getDependency() instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency
                        .getDependency();
                final JkVersion version = externalModule.getVersion();
                if (version.isDynamic()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if this object contains dependency on external
     * module whose rely on dynamic version that are resolvable (Maven Snapshot
     * versions are dynamic but not resolvable). It only stands for dynamic
     * versions as "1.3.+", "[1.0, 2.0[" ,... If so, when resolving, dynamic
     * versions are replaced by fixed resolved ones.
     */
    public boolean hasDynamicAndResolvableVersions() {
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.getDependency() instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency
                        .getDependency();
                final JkVersion version = externalModule.getVersion();
                if (version.isDynamicAndResovable()) {
                    return true;
                }
            }
        }
        return false;
    }

    public JkDependencySet withGlobalExclusion(JkDepExclude exclude) {
        final Set<JkDepExclude> depExcludes = new HashSet<>(this.globalExclusions);
        depExcludes.add(exclude);
        return new JkDependencySet(this.dependencies, depExcludes, this.versionProvider);
    }

    /**
     * Returns a set a dependency set identical to this one but excluding the specified exclude
     * from the transitive dependencies of the specified module.
     */
    public JkDependencySet withLocalExclusion(JkModuleId fromModule, JkDepExclude exclude) {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (final JkScopedDependency dep : this) {
            if (dep.getDependency() instanceof JkModuleDependency) {
                final JkModuleDependency moduleDependency = (JkModuleDependency) dep.getDependency();
                final JkScopedDependency scopedDep = dep.withDependency(moduleDependency.andExclude(exclude));
                list.add(scopedDep);
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
        for (JkScopedDependency scopedDependency : this.dependencies) {
            JkDependency dependency = scopedDependency.getDependency();
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
        List<JkScopedDependency> result = new LinkedList<>();
        Set<JkModuleId> moduleIds = new HashSet<>();
        for (JkScopedDependency scopedDependency : this.dependencies) {
            JkDependency dependency = scopedDependency.getDependency();
            if (dependency instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                JkModuleId moduleId = moduleDependency.getModuleId();
                if (moduleIds.contains(moduleId)) {
                    continue;
                }
                moduleIds.add(moduleId);
                JkModuleDependency replacingModuleDep = moduleDependency.withVersion(moduleIdVersionMap.get(moduleId));
                JkScopedDependency replacingScopedDep = scopedDependency.withDependency(replacingModuleDep);
                result.add(replacingScopedDep);
            } else {
                result.add(scopedDependency);
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
        final List<JkScopedDependency> list = new LinkedList<>();
        for (final JkScopedDependency dep : this) {
            if (dep.getDependency() instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) dep.getDependency();
                if (moduleDependency.getVersion().isUnspecified()) {
                    final JkVersion providedVersion = this.versionProvider.getVersionOf(moduleDependency.getModuleId());
                    if (providedVersion != null) {
                        moduleDependency = moduleDependency.withVersion(providedVersion);
                    }
                }
                final JkScopedDependency scopedDep = dep.withDependency(moduleDependency);
                list.add(scopedDep);
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
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.getDependency() instanceof JkModuleDependency) {
                final JkModuleDependency moduleDep = (JkModuleDependency) scopedDependency
                        .getDependency();
                builder.append("\n").append(indent).append(".and(\"")
                .append(moduleDep.getModuleId().getGroup()).append(":")
                .append(moduleDep.getModuleId().getName());
                if (!moduleDep.getVersion().isUnspecified()) {
                    builder.append(":" + moduleDep.getVersion().getValue());
                }
                builder.append('"');
                if (!scopedDependency.getScopes().isEmpty()) {
                    builder.append(", ");
                    for (final JkScope scope : scopedDependency.getScopes()) {
                        builder.append(scope.getName().toUpperCase()).append(", ");
                    }
                    builder.delete(builder.length() - 2, builder.length());
                }
                builder.append(")");
            }
        }
        return builder.toString();
    }

    /**
     * Returns all dependencies declared as {@link JkModuleDependency}.
     */
    public JkDependencySet withModulesOnly() {
        final List<JkScopedDependency> result = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.getDependency() instanceof JkModuleDependency) {
                result.add(scopedDependency);
            }
        }
        return new JkDependencySet(result, globalExclusions, versionProvider);
    }

    private List<JkModuleDependency> extractModuleDependencies() {
        final List<JkModuleDependency> result = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.getDependency() instanceof JkModuleDependency) {
                result.add((JkModuleDependency) scopedDependency.getDependency());
            }
        }
        return result;
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static JkDependencySet ofTextDescription(Path path) {
        return ofTextDescription(JkUtilsPath.toUrl(path));
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static JkDependencySet ofTextDescription(URL url) {
        return ofTextDescription(JkUtilsIO.read(url));
    }

    /**
     * Creates a dependencySet from a flat file formatted as :
     * <pre>
     * - COMPILE RUNTIME
     * org.springframework.boot:spring-boot-starter-thymeleaf
     * org.springframework.boot:spring-boot-starter-data-jpa
     *
     * - RUNTIME
     * com.h2database:h2
     * org.liquibase:liquibase-core
     * com.oracle:ojdbc6:12.1.0
     *
     * - TEST
     * org.springframework.boot:spring-boot-starter-test
     * org.seleniumhq.selenium:selenium-chrome-driver:3.4.0
     * org.fluentlenium:fluentlenium-assertj:3.2.0
     * org.fluentlenium:fluentlenium-junit:3.2.0
     *
     * - PROVIDED
     * org.projectlombok:lombok:1.16.16
     * </pre>
     */
    public static JkDependencySet ofTextDescription(String description) {
        final String[] lines = description.split(System.lineSeparator());
        JkScope[] currentScopes = JkJavaDepScopes.COMPILE_AND_RUNTIME;
        final List<JkScopedDependency> list = new LinkedList<>();
        for (final String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (line.startsWith("-")) {
                currentScopes = translateToScopes(line);
                continue;
            }
            final JkModuleDependency dependency = JkModuleDependency.of(line.trim());
            final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, currentScopes);
            list.add(scopedDependency);
        }
        return JkDependencySet.of(list);
    }

    private static JkScope[] translateToScopes(String line) {
        final String payload = JkUtilsString.substringAfterFirst(line,"-");
        final String[] items = payload.split(" ");
        final List<JkScope> result = new LinkedList<>();
        for (final String item : items) {
            if (JkUtilsString.isBlank(item)) {
                continue;
            }
            final JkScope javaDcope = JkJavaDepScopes.of(item.trim());
            if (javaDcope != null) {
                result.add(javaDcope);
            } else {
                result.add(JkScope.of(item.trim()));
            }
        }
        return result.toArray(new JkScope[0]);
    }

}
