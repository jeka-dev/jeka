package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import org.jerkar.api.depmanagement.JkScopedDependency.ScopeType;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A set of {@link JkScopedDependency} generally standing for the entire
 * dependencies of a project/module.
 *
 * @author Jerome Angibaud.
 */
public class JkDependencies implements Iterable<JkScopedDependency>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link JkDependencies} to the specified scoped dependencies.
     */
    public static JkDependencies of(JkScopedDependency... scopedDependencies) {
        return of(Arrays.asList(scopedDependencies));
    }

    /**
     * Creates a {@link JkDependencies} to the specified scoped dependencies.
     */
    public static JkDependencies of(List<JkScopedDependency> scopedDependencies) {
        return new JkDependencies(scopedDependencies, Collections.emptySet(), JkVersionProvider.of());
    }

    /**
     * Creates a {@link JkDependencies} based on jars located under the specified directory. Jars are
     * supposed to lie in a directory structure standing for the different scopes they are intended.
     * So jars needed for compilation are supposed to be in <code>baseTree/compile</code>, jar needed for
     * test are supposed to be in <code>baseTree/test</code> and so on.
     */
    public static JkDependencies ofLocal(Path baseDir) {
        final JkPathTree libDir = JkPathTree.of(baseDir);
        if (!libDir.exists()) {
            return JkDependencies.of();
        }
        return JkDependencies.of()
                .and(JkFileSystemDependency.ofPaths(libDir.accept("*.jar", "compile/*.jar").files()))
                    .withDefaultScope(JkJavaDepScopes.COMPILE)
                .and(JkFileSystemDependency.ofPaths(libDir.accept("provided/*.jar").files()))
                    .withDefaultScope(JkJavaDepScopes.PROVIDED)
                .and(JkFileSystemDependency.ofPaths(libDir.accept("runtime/*.jar").files()))
                    .withDefaultScope(JkJavaDepScopes.RUNTIME)
                .and(JkFileSystemDependency.ofPaths(libDir.accept("test/*.jar").files()))
                    .withDefaultScope(JkJavaDepScopes.TEST);
    }

    private final List<JkScopedDependency> dependencies;

    private final Set<JkDepExclude> depExcludes;

    private final JkVersionProvider explicitVersions;

    private JkDependencies(List<JkScopedDependency> dependencies, Set<JkDepExclude> excludes, JkVersionProvider explicitVersions) {
        super();
        this.dependencies = Collections.unmodifiableList(dependencies);
        this.depExcludes = Collections.unmodifiableSet(excludes);
        this.explicitVersions = explicitVersions;
    }

    /**
     * Returns the unmodifiable list list of scoped dependencies for this object.
     */
    public List<JkScopedDependency> list() {
        return this.dependencies;
    }

    /**
     * Returns a clone of this object minus the dependencies on the given
     * {@link JkModuleId}. This is used to exclude a given module to all
     * scope.
     */
    private JkDependencies without(JkModuleId jkModuleId) {
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
        return new JkDependencies(result, this.depExcludes, this.explicitVersions);
    }

    /**
     * Returns a clone of this dependencies but replacing the unscoped
     * dependencies with the specified ones.
     */
    public JkDependencies withDefaultScope(JkScope... scopes) {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (JkScopedDependency dep : this) {
            if (dep.scopeType().equals(ScopeType.UNSET)) {
                dep = dep.withScopes(scopes);
            }
            list.add(dep);
        }
        return new JkDependencies(list, this.depExcludes, this.explicitVersions);
    }

    /**
     * Returns a clone of this dependencies but replacing the unscoped
     * dependencies with the specified scope mapping.
     */
    public JkDependencies withDefaultScope(JkScopeMapping scopeMapping) {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (JkScopedDependency dep : this) {
            if (dep.scopeType().equals(ScopeType.UNSET) && (dep.dependency() instanceof JkModuleDependency)) {
                dep = dep.withScopeMapping(scopeMapping);
            }
            list.add(dep);
        }
        return new JkDependencies(list, this.depExcludes, this.explicitVersions);
    }

    /**
     * Returns a clone of this object but using specified projectVersion provider to override
     * versions of transitive dependencies.
     */
    public JkDependencies withExplicitVersions(JkVersionProvider explicitVersions) {
        return new JkDependencies(this.dependencies, this.excludes(), explicitVersions);
    }

    /**
     * Returns a clone of this object plus the specified
     * {@link JkScopedDependency}s.
     */
    public JkDependencies and(Iterable<JkScopedDependency> others) {
        if (!others.iterator().hasNext()) {
            return this;
        }
        return JkDependencies.builder().on(this).on(others).build();
    }

    /**
     * Returns a clone of this object plus the specified scoped dependencies.
     */
    public JkDependencies and(JkScopedDependency... others) {
        return and(Arrays.asList(others));
    }

    /**
     * Returns a clone of this object plus the specified scoped dependencies.
     */
    public JkDependencies and(JkDependency dependency, JkScope ... scopes) {
        return this.and(JkScopedDependency.of(dependency, scopes));
    }

    /**
     * Creates a {@link JkDependencies} to the specified artifact producer
     */
    public JkDependencies and(JkArtifactProducer artifactProducer, JkArtifactId... artifactFileIds) {
        final ArtifactProducerDependency dependency = new ArtifactProducerDependency(artifactProducer, artifactFileIds);
        final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency);
        return and(scopedDependency);
    }

    /**
     * Creates a {@link JkDependencies} to the specified artifact producer supplier
     */
    public JkDependencies and(Supplier<JkArtifactProducer> artifactProducerSupplier, JkArtifactId... artifactFileIds) {
        return and(artifactProducerSupplier.get(), artifactFileIds);
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
    public JkVersionProvider explicitVersions() {
        return this.explicitVersions;
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
    public JkDependencies onlyModules() {
        final List<JkScopedDependency> deps = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                deps.add(scopedDependency);
            }
        }
        return new JkDependencies(deps, excludes(), this.explicitVersions);
    }

    /**
     * Creates a clone of these dependencies replacing the dynamic versions by
     * the static ones specified in the {@link JkVersionedModule}s passed as
     * argument. <br/>
     */
    public JkDependencies resolvedWith(Iterable<JkVersionedModule> resolvedModules) {
        return resolvedWith(JkVersionProvider.of(resolvedModules));
    }


    /**
     * @see #resolvedWith(Iterable)
     */
    public JkDependencies resolvedWith(JkVersionProvider provider) {

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
        return new JkDependencies(result, this.depExcludes, this.explicitVersions);
    }

    public JkDependencies excludeGlobally(JkDepExclude exclude) {
        Set<JkDepExclude> depExcludes = new HashSet<>(this.depExcludes);
        depExcludes.add(exclude);
        return new JkDependencies(this.dependencies, depExcludes, this.explicitVersions);
    }

    /**
     * Creates a Builder for {@link JkDependencies}
     */
    public static Builder builder() {
        return new Builder(new LinkedList<>());
    }

    /** Builder for <code>JkDependencies</code> */
    public static class Builder {

        /** Dependencies declared in this builder **/
        protected final LinkedList<JkScopedDependency> dependencies;

        /** Default scopes used on this builder */
        protected Set<JkScope> defaultScopes;

        /** Default scope mapping used on this builder */
        protected JkScopeMapping defaultMapping;

        /**
         * Construct a builder.
         */
        protected Builder(LinkedList<JkScopedDependency> dependencies) {
            super();
            this.dependencies = dependencies;
        }

        /**
         * Adds the specified dependency with the specified scopes to this
         * builder.
         */
        public JkFluentScopeableBuilder on(JkDependency dependency, JkScope... scopes) {
            if (dependency instanceof JkFileSystemDependency) {
                final JkFileSystemDependency fileDeps = (JkFileSystemDependency) dependency;
                if (fileDeps.paths().isEmpty()) {
                    return new JkFluentScopeableBuilder(this);
                }
            }
            final JkScopedDependency scopedDependency;
            if (scopes.length == 0 && defaultScopes != null) {
                scopedDependency = JkScopedDependency.of(dependency, defaultScopes);
            } else if (scopes.length == 0 && defaultMapping != null
                    && dependency instanceof JkModuleDependency) {
                scopedDependency = JkScopedDependency.of((JkModuleDependency) dependency,
                        defaultMapping);
            } else {
                scopedDependency = JkScopedDependency.of(dependency, scopes);
            }
            dependencies.add(scopedDependency);
            if (this instanceof JkFluentScopeableBuilder) {
                return (JkFluentScopeableBuilder) this;
            }
            return new JkFluentScopeableBuilder(this);
        }

        /**
         * Adds a module dependency on this builder.
         */
        public JkFluentModuleDepBuilder on(JkModuleDependency dependency) {
            return on(dependency, new JkScope[0]);
        }

        /**
         * Adds module dependencies on this builder without mentioning scope.
         */
        public Builder onScopeless(Iterable<? extends JkDependency> dependencies) {
            final List<JkScopedDependency> jkScopedDependencies = new LinkedList<>();
            for (final JkDependency dependency : dependencies) {
                jkScopedDependencies.add(JkScopedDependency.of(dependency));
            }
            return on(jkScopedDependencies);
        }

        /**
         * Adds a module dependency on this builder.
         */
        public JkFluentModuleDepBuilder on(JkModuleDependency dependency, JkScope... scopes) {
            final JkScopedDependency scopedDependency;
            if (scopes.length == 0 && defaultScopes != null) {
                scopedDependency = JkScopedDependency.of(dependency, defaultScopes);
            } else if (scopes.length == 0 && defaultMapping != null) {
                scopedDependency = JkScopedDependency.of(dependency, defaultMapping);
            } else {
                scopedDependency = JkScopedDependency.of(dependency, scopes);
            }
            dependencies.add(scopedDependency);
            if (this instanceof JkFluentModuleDepBuilder) {
                return (JkFluentModuleDepBuilder) this;
            }
            return new JkFluentModuleDepBuilder(this);
        }

        /**
         * Adds the specified scoped dependency to this builder.
         */
        public Builder on(JkScopedDependency dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        /**
         * Add the specified files as dependencies.
         */
        public JkFluentScopeableBuilder on(File... files) {
            return on(JkUtilsPath.toPaths(files));
        }

        /**
         * Add the specified files as dependencies.
         */
        public JkFluentScopeableBuilder on(Path... files) {
            return on(JkFileSystemDependency.ofPaths(Arrays.asList(files)));
        }

        /**
         * Add the specified files as dependencies.
         */
        public JkFluentScopeableBuilder onFiles(Iterable<Path> files, JkScope... scopes) {
            final JkFileSystemDependency dependency = JkFileSystemDependency.ofPaths(JkUtilsPath.disambiguate(files));
            return on(dependency, scopes);
        }

        /**
         * Same as {@link #onFiles(Iterable, JkScope...)} )} but effective only if the
         * specified condition is true.
         */
        public JkFluentScopeableBuilder onFilesIf(boolean condition, Iterable<Path> files, JkScope... scopes) {
            if (condition) {
                return onFiles(JkUtilsPath.disambiguate(files), scopes);
            }
            if (this instanceof JkFluentScopeableBuilder) {
                return (JkFluentScopeableBuilder) this;
            }
            return new JkFluentScopeableBuilder(this);
        }

        /**
         * @see #on(JkModuleDependency, JkScope...)
         */
        public JkFluentModuleDepBuilder on(JkModuleId module, JkVersionRange version,
                JkScope... scopes) {
            return on(JkModuleDependency.of(module, version), scopes);
        }

        /**
         * Adds a module dependency with unspecified projectVersion on this builder.
         */
        public JkFluentModuleDepBuilder on(JkModuleId module, JkScope... scopes) {
            return on(JkModuleDependency.of(module, JkVersionRange.UNSPECIFIED), scopes);
        }

        /**
         * @see #on(JkModuleDependency, JkScope...)
         */
        public JkFluentModuleDepBuilder on(JkModuleId module, String version, JkScope... scopes) {
            return on(module, JkVersionRange.of(version), scopes);
        }

        /**
         * @see #on(JkModuleDependency, JkScope...)
         */
        public JkFluentModuleDepBuilder on(String group, String name, String version,
                JkScope... scopes) {
            return on(JkModuleId.of(group, name), version, scopes);
        }

        /**
         * @see #on(JkModuleDependency, JkScope...)
         */
        public JkFluentModuleDepBuilder on(String description, JkScope... scopes) {
            return on(JkModuleDependency.of(description), scopes);
        }

        /**
         * Adds the specified scoped dependencies to this builder.
         */
        public Builder on(Iterable<JkScopedDependency> dependencies) {
            if (!dependencies.iterator().hasNext()) {
                return this;
            }
            for (final JkScopedDependency dependency : dependencies) {
                this.dependencies.add(dependency);
            }
            return this;
        }

        /**
         * Adds the specified computed dependency to this builder. If the specified
         * file is not present then the specified process is launched prior to try
         * to get the file again.
         */
        public Builder on(JkProcess jkProcess, Path file, JkScope ...scopes) {
            if (!dependencies.iterator().hasNext()) {
                return this;
            }
            final JkComputedDependency dependency = JkComputedDependency.of(jkProcess, file);
            final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, scopes);
            this.dependencies.add(scopedDependency);
            return this;
        }

        /**
         * Same as {@link #on(JkProcess, Path, JkScope...)} but you can specify project base dir in order
         * to generate IDE metadata with dependencies on project rather than the generated files..
         */
        public Builder on(JkProcess jkProcess, Path projectBaseDir, Path file, JkScope ...scopes) {
            if (!dependencies.iterator().hasNext()) {
                return this;
            }
            final JkComputedDependency dependency = JkComputedDependency.of(jkProcess, file)
                    .withIdeProjectBaseDir(projectBaseDir);
            final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, scopes);
            this.dependencies.add(scopedDependency);
            return this;
        }

        /**
         * Same as {@link #on(JkProcess, Path, Path, JkScope...)} but it will take the working dir
         * of the specified process as the ide project base dir. When generating IDE metadata, if
         * useIdeProjectDep flag is <code>false</code>,
         * the project dependency won't be taken in account and regular file dependency will apply.
         */
        public Builder on(JkProcess process, boolean useIdeProjectDep, Path file, JkScope ...scopes) {
            if (!dependencies.iterator().hasNext()) {
                return this;
            }
            JkComputedDependency dependency = JkComputedDependency.of(process, file);
            if (useIdeProjectDep) {
                dependency = dependency.withIdeProjectBaseDir(process.workingDir());
            }
            final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, scopes);

            this.dependencies.add(scopedDependency);
            return this;
        }

        /**
         * Adds a dependency on the specified artifact producer.
         */
        public JkFluentScopeableBuilder on(JkArtifactProducer artifactProducer, JkArtifactId... artifactFileIds) {
            final ArtifactProducerDependency dependency = new ArtifactProducerDependency(artifactProducer,
                    Arrays.asList(artifactFileIds));
            final JkScopedDependency scopedDependency = JkScopedDependency.of(dependency);
            this.dependencies.add(scopedDependency);
            return new JkFluentScopeableBuilder(this);
        }

        /**
         * Adds a dependency on the specified artifact producer.
         */
        public JkFluentScopeableBuilder on(Supplier<JkArtifactProducer> artifactProducerSupplier, JkArtifactId... artifactFileIds) {
            return on(artifactProducerSupplier.get(), artifactFileIds);
        }


        /**
         * Constructs a {@link JkDependencies} to scoped dependencies declared
         * in this builder.
         */
        public JkDependencies build() {
          return new JkDependencies(new LinkedList<>(dependencies), new HashSet<>(), JkVersionProvider.of());
        }

        /**
         * Returned type after an addition of a {@link JkDependency}. This type
         * allows to chain definition of scopes.
         */
        public static class JkFluentScopeableBuilder extends Builder {

            /**
             * Construct a JkFluentScopeableBuilder to a basic builder.
             */
            protected JkFluentScopeableBuilder(Builder builder) {
                super(builder.dependencies);
                this.defaultMapping = builder.defaultMapping;
                this.defaultScopes = builder.defaultScopes;
            }

            /**
             * Applies a scope mapping to the right previously added dependency
             * on this builder.
             */
            public Builder scope(JkScope... scopes) {
                final JkScopedDependency lastScopedDep = dependencies.pollLast();
                if (lastScopedDep == null) {
                    return this;
                }
                final JkDependency dependency = lastScopedDep.dependency();
                dependencies.add(JkScopedDependency.of(dependency, JkUtilsIterable.setOf(scopes)));
                return this;
            }

        }

        /**
         * Returned type after an addition of a {@link JkModuleDependency}. This
         * type allows to chain redefinition of scope, scope mapping,
         * transitivity and exclusions.
         */
        public static final class JkFluentModuleDepBuilder extends JkFluentScopeableBuilder {

            private JkFluentModuleDepBuilder(Builder builder) {
                super(builder);
            }

            /**
             * Applies a scope to the right previously added dependency on this
             * builder.
             */
            public JkFluentModuleDepBuilder scope(JkScopeMapping scopeMapping) {
                final JkModuleDependency dependency = (JkModuleDependency) dependencies.pollLast()
                        .dependency();
                dependencies.add(JkScopedDependency.of(dependency, scopeMapping));
                return this;
            }

            /**
             * Applies a scope mapping to the right previously added dependency
             * on this builder.
             */
            public JkFluentAfterMapScopeBuilder mapScope(JkScope... scopes) {
                return new JkFluentAfterMapScopeBuilder(dependencies, JkUtilsIterable.setOf(scopes));
            }

            /**
             * Applies specified transitive flag to the right previously added
             * dependency on this builder.
             */
            public JkFluentModuleDepBuilder transitive(boolean transitive) {
                final JkScopedDependency scopedDependency = dependencies.pollLast();
                final JkModuleDependency dependency = (JkModuleDependency) scopedDependency
                        .dependency();
                dependencies.add(scopedDependency.dependency(dependency.transitive(transitive)));
                return this;
            }

            /**
             * Excludes the specified dependency to the transitive
             * dependencies of the right previously added dependency on this
             * builder.
             */
            public JkFluentModuleDepBuilder excludeLocally(JkDepExclude depExclude) {
                final JkScopedDependency scopedDependency = dependencies.pollLast();
                final JkModuleDependency dependency = (JkModuleDependency) scopedDependency
                        .dependency();
                dependencies.add(scopedDependency.dependency(dependency.andExclude(depExclude)));
                return this;
            }

            /**
             * @see #excludeLocally(JkDepExclude)
             */
            public JkFluentModuleDepBuilder excludeLocally(String group, String name) {
                return excludeLocally(JkDepExclude.of(group, name));
            }

            /**
             * @see #excludeLocally(JkDepExclude)
             */
            public JkFluentModuleDepBuilder excludeLocally(String groupAndName) {
                return excludeLocally(JkDepExclude.of(groupAndName));
            }

            /**
             * @see #excludeLocally(JkDepExclude)
             */
            public JkFluentModuleDepBuilder excludeLocally(JkModuleId moduleId) {
                return excludeLocally(moduleId.group(), moduleId.name());
            }

        }

        /**
         * Type returned after the left part of the scope mapping has been
         * declared. It allows chaining with the right part of the mapping.
         */
        public static class JkFluentAfterMapScopeBuilder {

            private final LinkedList<JkScopedDependency> dependencies;

            private final Iterable<JkScope> from;

            private JkFluentAfterMapScopeBuilder(LinkedList<JkScopedDependency> dependencies,
                    Iterable<JkScope> from) {
                this.dependencies = dependencies;
                this.from = from;
            }

            /**
             * Defines the right part of the mapping.
             */
            public JkFluentAfterToBuilder to(JkScope... jkScopes) {
                final JkScopedDependency scopedDependency = dependencies.pollLast();
                final JkScopeMapping mapping;
                final JkModuleDependency dependency = (JkModuleDependency) scopedDependency
                        .dependency();
                if (scopedDependency.scopeType() == JkScopedDependency.ScopeType.UNSET) {
                    mapping = JkScopeMapping.of(from).to(jkScopes);
                } else {
                    mapping = scopedDependency.scopeMapping().and(from).to(jkScopes);
                }
                dependencies.add(JkScopedDependency.of(dependency, mapping));
                return new JkFluentAfterToBuilder(dependencies);
            }

            /**
             * Defines the right part of the mapping.Specified scope string are
             * internally turned to {@link JkScope}
             */
            public JkFluentAfterToBuilder to(String... scopeNames) {
                final JkScope[] scopes = new JkScope[scopeNames.length];
                for (int i = 0; i < scopeNames.length; i++) {
                    scopes[i] = JkScope.of(scopeNames[i]);
                }
                return to(scopes);
            }

        }

        /**
         * Type returned after the right side declaration of a scope mapping. It
         * gives the opportunity to complete the mapping with another pair of
         * (left->right) mapping.
         */
        public static class JkFluentAfterToBuilder extends Builder {

            private JkFluentAfterToBuilder(LinkedList<JkScopedDependency> dependencies) {
                super(dependencies);
            }

            /** Add specific scopes tho this builder */
            public JkFluentAfterMapScopeBuilder and(JkScope... scopes) {
                return new JkFluentAfterMapScopeBuilder(dependencies, Arrays.asList(scopes));
            }

        }
    }

    /**
     * Throws a <code>IllegalStateException</code> if one of the module
     * dependencies has an unspecified projectVersion.
     */
    public JkDependencies assertNoUnspecifiedVersion() {
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
    public JkDependencies modulesOnly() {
        return JkDependencies.builder().onScopeless(extractModuleDependencies()).build();
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
