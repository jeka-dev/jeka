package org.jerkar.api.depmanagement;

import org.jerkar.api.depmanagement.JkScopedDependency.ScopeType;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

/**
 * A set of {@link JkScopedDependency} generally standing for the entire
 * dependencies of a project/module.
 *
 * @author Jerome Angibaud.
 */
public class JkDependencySet implements Iterable<JkScopedDependency>, Serializable {

    private static final long serialVersionUID = 1L;

    private final List<JkScopedDependency> dependencies;

    private final Set<JkDepExclude> depExcludes;

    private final JkVersionProvider versionProvider;

    private JkDependencySet(Iterable<JkScopedDependency> dependencies, Set<JkDepExclude> excludes, JkVersionProvider explicitVersions) {
        super();
        this.dependencies = Collections.unmodifiableList(JkUtilsIterable.listOf(dependencies));
        this.depExcludes = Collections.unmodifiableSet(excludes);
        this.versionProvider = explicitVersions;
    }

    /**
     * Creates a {@link JkDependencySet} to the specified scoped dependencies.
     */
    public static JkDependencySet of(JkScopedDependency... scopedDependencies) {
        return of(Arrays.asList(scopedDependencies));
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
     * So jars needed for compilation are supposed to be in <code>baseTree/compile</code>, jar needed for
     * test are supposed to be in <code>baseTree/test</code> and so on.
     */
    public static JkDependencySet ofLocal(Path baseDir) {
        final JkPathTree libDir = JkPathTree.of(baseDir);
        if (!libDir.exists()) {
            return JkDependencySet.of();
        }
        return JkDependencySet.of()
                .and(JkFileSystemDependency.ofPaths(libDir.accept("*.jar", "compile/*.jar").files()))
                .withDefaultScope(JkJavaDepScopes.COMPILE)
                .and(JkFileSystemDependency.ofPaths(libDir.accept("provided/*.jar").files()))
                .withDefaultScope(JkJavaDepScopes.PROVIDED)
                .and(JkFileSystemDependency.ofPaths(libDir.accept("runtime/*.jar").files()))
                .withDefaultScope(JkJavaDepScopes.RUNTIME)
                .and(JkFileSystemDependency.ofPaths(libDir.accept("test/*.jar").files()))
                .withDefaultScope(JkJavaDepScopes.TEST);
    }


    /**
     * Returns the unmodifiable list list of scoped dependencies for this object.
     */
    public List<JkScopedDependency> list() {
        return this.dependencies;
    }

    /**
     * Returns a clone of this object plus the specified
     * {@link JkScopedDependency}s.
     */
    public JkDependencySet and(Iterable<JkScopedDependency> others) {
        if (!others.iterator().hasNext()) {
            return this;
        }
        List<JkScopedDependency> deps = JkUtilsIterable.concatLists(this.dependencies, others);
        return new JkDependencySet(deps, this.depExcludes, this.versionProvider);
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
     * Creates a {@link JkDependencySet} to the specified artifact producer
     */
    public JkDependencySet and(JkArtifactProducer artifactProducer, List<JkArtifactId> artifactFileIds, JkScope ... scopes) {
        final ArtifactProducerDependency dependency = new ArtifactProducerDependency(artifactProducer, artifactFileIds);
        final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, scopes);
        return and(scopedDependency);
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer
     */
    public JkDependencySet and(JkArtifactProducer artifactProducer, JkScope... scopes) {
        return and(artifactProducer, Collections.emptyList(), scopes);
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer supplier
     */
    public JkDependencySet and(Supplier<JkArtifactProducer> artifactProducerSupplier, List<JkArtifactId> artifactFileIds,
                               JkScope... scopes) {
        return and(artifactProducerSupplier.get(), artifactFileIds, scopes);
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer supplier
     */
    public JkDependencySet and(Supplier<JkArtifactProducer> artifactProducerSupplier, JkArtifactId artifactFileIds,
                               JkScope... scopes) {
        return and(artifactProducerSupplier.get(), Arrays.asList(artifactFileIds), scopes);
    }

    /**
     * Creates a {@link JkDependencySet} to the specified artifact producer supplier
     */
    public JkDependencySet and(Supplier<JkArtifactProducer> artifactProducerSupplier,
                               JkScope... scopes) {
        return and(artifactProducerSupplier.get(), Collections.emptyList(), scopes);
    }

    public JkDependencySet and(String moduleDescription, JkScope ... scopes) {
        return and(JkModuleDependency.of(moduleDescription), scopes);
    }

    public JkDependencySet and(String moduleDescription, JkScopeMapping scopeMapping) {
        return and(JkModuleDependency.of(moduleDescription), scopeMapping);
    }

    public JkDependencySet and(JkModuleId moduleId, String version, JkScope ... scopes) {
        return and(JkModuleDependency.of(moduleId, version), scopes);
    }

    public JkDependencySet and(JkModuleId moduleId, JkScope ... scopes) {
        return and(JkModuleDependency.of(moduleId, JkVersionRange.UNSPECIFIED), scopes);
    }

    public JkDependencySet and(JkModuleId moduleId, String version, JkScopeMapping scopeMapping) {
        return and(JkModuleDependency.of(moduleId, version), scopeMapping);
    }

    public JkDependencySet andFiles(Iterable<Path> files, JkScope... scopes) {
        return and(JkFileSystemDependency.ofPaths(files), scopes);
    }

    public JkDependencySet and(Path files, JkScope... scopes) {
        return and(JkFileSystemDependency.ofPaths(files), scopes);
    }

    public JkDependencySet andUnscoped(Iterable<? extends JkDependency> dependencies) {
        List<JkScopedDependency> deps = new LinkedList<>(this.dependencies);
        for(JkDependency dependency : dependencies) {
            deps.add(JkScopedDependency.of(dependency));
        }
        return new JkDependencySet(deps, this.depExcludes, this.versionProvider);
    }

    /**
     * Returns a clone of this object minus the dependencies on the given
     * {@link JkModuleId}. This is used to exclude a given module to all
     * scope.
     */
    private JkDependencySet minus(JkModuleId jkModuleId) {
        final List<JkScopedDependency> result = new LinkedList<>(dependencies);
        for (final Iterator<JkScopedDependency> it = result.iterator(); it.hasNext();) {
            final JkDependency dependency = it.next().dependency();
            if (dependency instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) dependency;
                if (externalModule.moduleId().equals(jkModuleId)) {
                    it.remove();
                }
            }
        }
        return new JkDependencySet(result, this.depExcludes, this.versionProvider);
    }

    public JkDependencySet onlyIf(boolean condition) {
        if (condition) {
            return this;
        }
        LinkedList<JkScopedDependency> deps = new LinkedList<>(dependencies);
        deps.removeLast();
        return new JkDependencySet(dependencies, depExcludes, versionProvider);
    }

    /**
     * Returns a clone of this dependencies but replacing the unscoped
     * dependencies with the specified ones.
     */
    public JkDependencySet withDefaultScope(JkScope... scopes) {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (JkScopedDependency dep : this) {
            if (dep.scopeType().equals(ScopeType.UNSET)) {
                dep = dep.withScopes(scopes);
            }
            list.add(dep);
        }
        return new JkDependencySet(list, this.depExcludes, this.versionProvider);
    }

    /**
     * Returns a clone of this dependencies but replacing the unscoped
     * dependencies with the specified scope mapping.
     */
    public JkDependencySet withDefaultScope(JkScopeMapping scopeMapping) {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (JkScopedDependency dep : this) {
            if (dep.scopeType().equals(ScopeType.UNSET) && (dep.dependency() instanceof JkModuleDependency)) {
                dep = dep.withScopeMapping(scopeMapping);
            }
            list.add(dep);
        }
        return new JkDependencySet(list, this.depExcludes, this.versionProvider);
    }

    /**
     * Returns a clone of this object but using specified projectVersion provider to override
     * versions of transitive dependencies. The previous version provider is replaced
     * by the specified one, there is no addition.
     */
    public JkDependencySet withVersionProvider(JkVersionProvider versionProvider) {
        return new JkDependencySet(this.dependencies, this.excludes(), versionProvider);
    }

    /**
     * Returns a clone of this object but using specified projectVersion provider to override
     * versions of transitive dependencies. The specified version provider is added
     * to the specified one.
     */
    public JkDependencySet andVersionProvider(JkVersionProvider versionProvider) {
        return new JkDependencySet(this.dependencies, this.excludes(), this.versionProvider.and(versionProvider));
    }

    /**
     * Returns <code>true</code> if this object contains dependencies whose are
     * {@link JkModuleDependency}.
     */
    public boolean containsModules() {
        for (final JkScopedDependency scopedDependency : dependencies) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
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
    public Set<JkDepExclude> excludes() {
        return this.depExcludes;
    }

    /**
     * Returns overridden versions for transitive dependencies. Versions present here will
     * overwrite versions found in transitive dependencies.
     */
    public JkVersionProvider versionProvider() {
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
    public Set<JkDependency> dependenciesDeclaredWith(JkScope scope) {
        final Set<JkDependency> depList = new HashSet<>();
        for (final JkScopedDependency scopedDependency : this) {
            if ((scopedDependency.scopeType().equals(ScopeType.SIMPLE)
                    && scopedDependency.scopes().contains(scope))
                    ||
                    (scopedDependency.scopeType().equals(ScopeType.MAPPED)
                            && scopedDependency.scopeMapping().entries().contains(scope))) {
                depList.add(scopedDependency.dependency());
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
            final JkDependency dependency = scopedDependency.dependency();
            if (dependency instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) dependency;
                if (externalModule.moduleId().equals(moduleId)) {
                    return scopedDependency;
                }
            }
        }
        return null;
    }

    /**
     * Returns the set of scopes involved in these dependencies.
     */
    public Set<JkScope> declaredScopes() {
        final Set<JkScope> result = new HashSet<>();
        for (final JkScopedDependency dep : this.dependencies) {
            if (dep.scopeType() == ScopeType.MAPPED) {
                result.addAll(dep.scopeMapping().entries());
            } else if (dep.scopeType() == ScopeType.SIMPLE) {
                result.addAll(dep.scopes());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns all scopes that are involved in these dependencies. That means if one of these scoped
     * dependencies is declared with scope 'FOO' and scope 'BAR' extends scope 'FOO', then 'FOO' and 'BAR' is
     * part of involved scopes.
     */
    public Set<JkScope> involvedScopes() {
        return JkScope.involvedScopes(declaredScopes());
    }

    /**
     * Returns <code>true</code> if this object contains dependency on external
     * module whose rely on dynamic projectVersion. It can be either dynamic projectVersion as
     * "1.3.+", "[1.0, 2.0[" ,... or snapshot projectVersion as defined in Maven (as
     * "1.0-SNAPSHOT).
     */
    public boolean hasDynamicVersions() {
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency
                        .dependency();
                final JkVersionRange versionRange = externalModule.versionRange();
                if (versionRange.isDynamic()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if this object contains dependency on external
     * module whose rely on dynamic projectVersion that are resolvable (Maven Snapshot
     * versions are dynamic but not resolvable). It only stands for dynamic
     * versions as "1.3.+", "[1.0, 2.0[" ,... If so, when resolving, dynamic
     * versions are replaced by fixed resolved ones.
     */
    public boolean hasDynamicAndResolvableVersions() {
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency
                        .dependency();
                final JkVersionRange versionRange = externalModule.versionRange();
                if (versionRange.isDynamicAndResovable()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a set of dependencies that contains all and only module
     * dependencies declared in this object.
     */
    public JkDependencySet onlyModules() {
        final List<JkScopedDependency> deps = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                deps.add(scopedDependency);
            }
        }
        return new JkDependencySet(deps, excludes(), this.versionProvider);
    }

    /**
     * Creates a clone of these dependencies replacing the dynamic versions by
     * the static ones specified in the {@link JkVersionedModule}s passed as
     * argument. <br/>
     */
    public JkDependencySet resolvedWith(Iterable<JkVersionedModule> resolvedModules) {
        return resolvedWith(JkVersionProvider.of(resolvedModules));
    }


    /**
     * @see #resolvedWith(Iterable)
     */
    public JkDependencySet resolvedWith(JkVersionProvider provider) {

        final List<JkScopedDependency> result  = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : this) {
            if (! (scopedDependency.dependency() instanceof JkModuleDependency)) {
                result.add(scopedDependency);
                continue;
            }
            final JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency.dependency();
            final JkModuleId moduleId = moduleDependency.moduleId();
            final JkScopedDependency toAdd;
            if (moduleDependency.versionRange().isDynamicAndResovable()
                    || moduleDependency.hasUnspecifedVersion()) {
                final JkVersion resolvedVersion = provider.versionOf(moduleId);
                if (resolvedVersion != null) {
                    final JkModuleDependency resolvedModule = moduleDependency
                            .resolvedTo(resolvedVersion);
                    toAdd = scopedDependency
                            .dependency(resolvedModule);
                } else {
                    toAdd = scopedDependency;
                }
            } else {
                toAdd = scopedDependency;
            }
            result.add(toAdd);
        }
        return new JkDependencySet(result, this.depExcludes, this.versionProvider);
    }

    public JkDependencySet excludeGlobally(JkDepExclude exclude) {
        Set<JkDepExclude> depExcludes = new HashSet<>(this.depExcludes);
        depExcludes.add(exclude);
        return new JkDependencySet(this.dependencies, depExcludes, this.versionProvider);
    }

    /**
     * Throws a <code>IllegalStateException</code> if one of the module
     * dependencies has an unspecified projectVersion.
     */
    public JkDependencySet assertNoUnspecifiedVersion() {
        final List<JkModuleDependency> unspecifieds = this.unspecifiedVersionDependencies();
        JkUtilsAssert.isTrue(unspecifieds.isEmpty(), "Following module does not specify projectVersion : "
                + unspecifieds);
        return this;
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
        builder.append("JkDependencies.builder()");
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                final JkModuleDependency moduleDep = (JkModuleDependency) scopedDependency
                        .dependency();
                builder.append("\n").append(indent).append(".on(\"")
                .append(moduleDep.moduleId().group()).append("\", \"")
                .append(moduleDep.moduleId().name()).append("\", \"")
                .append(moduleDep.versionRange().definition()).append("\"");
                if (!scopedDependency.scopes().isEmpty()) {
                    builder.append(", ");
                    for (final JkScope scope : scopedDependency.scopes()) {
                        builder.append(scope.name().toUpperCase()).append(", ");
                    }
                    builder.delete(builder.length() - 2, builder.length());
                }
                builder.append(")");
            }
        }
        builder.append(".build();");
        return builder.toString();
    }

    /**
     * Returns all dependencies declared as {@link JkModuleDependency}.
     */
    public JkDependencySet modulesOnly() {
        final List<JkScopedDependency> result = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                result.add(scopedDependency);
            }
        }
        return JkDependencySet.of(result);
    }

    private List<JkModuleDependency> extractModuleDependencies() {
        final List<JkModuleDependency> result = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                result.add((JkModuleDependency) scopedDependency.dependency());
            }
        }
        return result;
    }

}
