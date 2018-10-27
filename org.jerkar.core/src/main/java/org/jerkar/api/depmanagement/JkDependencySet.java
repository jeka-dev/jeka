package org.jerkar.api.depmanagement;

import org.jerkar.api.depmanagement.JkScopedDependency.ScopeType;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final Set<JkDepExclude> globalExclusions;

    private final JkVersionProvider versionProvider;

    private JkDependencySet(Iterable<JkScopedDependency> dependencies, Set<JkDepExclude> excludes, JkVersionProvider explicitVersions) {
        super();
        this.dependencies = Collections.unmodifiableList(JkUtilsIterable.listOf(dependencies));
        this.globalExclusions = Collections.unmodifiableSet(excludes);
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
     * So jars needed for compilation are supposed to be in <code>baseDir/compile</code>, jar needed for
     * test are supposed to be in <code>baseDir/test</code> and so on.
     */
    public static JkDependencySet ofLocal(Path baseDir) {
        final JkPathTree libDir = JkPathTree.of(baseDir);
        if (!libDir.exists()) {
            return JkDependencySet.of();
        }
        return JkDependencySet.of()
                .and(JkFileSystemDependency.ofPaths(libDir.andAccept("*.jar", "compile/*.jar").getFiles()))
                .withDefaultScope(JkJavaDepScopes.COMPILE)
                .and(JkFileSystemDependency.ofPaths(libDir.andAccept("provided/*.jar").getFiles()))
                .withDefaultScope(JkJavaDepScopes.PROVIDED)
                .and(JkFileSystemDependency.ofPaths(libDir.andAccept("runtime/*.jar").getFiles()))
                .withDefaultScope(JkJavaDepScopes.RUNTIME)
                .and(JkFileSystemDependency.ofPaths(libDir.andAccept("test/*.jar").getFiles()))
                .withDefaultScope(JkJavaDepScopes.TEST);
    }


    /**
     * Returns the unmodifiable list list of scoped dependencies for this object.
     */
    public List<JkScopedDependency> toList() {
        return this.dependencies;
    }

    public JkVersion getVersion(JkModuleId moduleId) {
        JkScopedDependency dep = this.get(moduleId);
        if (dep == null) {
            return null;
        }
        JkModuleDependency moduleDependency = (JkModuleDependency) dep.withDependency();
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
        List<JkScopedDependency> deps = JkUtilsIterable.concatLists(this.dependencies, others);
        return new JkDependencySet(deps, this.globalExclusions, this.versionProvider);
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
        return and(JkFileSystemDependency.ofPaths(files), scopes);
    }


    public JkDependencySet andFile(String path, JkScope... scopes) {
        return andFile(Paths.get(path), scopes);
    }

    /**
     * If specified path is relative, JkDependencyResolver will resolve it upon its base dir.
     */
    public JkDependencySet andFile(Path file, JkScope... scopes) {
        return and(JkFileSystemDependency.ofPaths(file), scopes);
    }

    public JkDependencySet andScopelessDependencies(Iterable<? extends JkDependency> dependencies) {
        List<JkScopedDependency> deps = new LinkedList<>(this.dependencies);
        for(JkDependency dependency : dependencies) {
            deps.add(JkScopedDependency.of(dependency));
        }
        return new JkDependencySet(deps, this.globalExclusions, this.versionProvider);
    }

    /**
     * Returns a dependency set identical to this one minus the dependencies on the given
     * {@link JkModuleId}. This is used to exclude a given module to all
     * scope.
     */
    private JkDependencySet minus(JkModuleId jkModuleId) {
        final List<JkScopedDependency> result = new LinkedList<>(dependencies);
        for (final Iterator<JkScopedDependency> it = result.iterator(); it.hasNext();) {
            final JkDependency dependency = it.next().withDependency();
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
     * Returns a clone of this dependencySet but removing the last element if the specified condition is not met.
     */
    public JkDependencySet withLastIf(boolean condition) {
        if (condition) {
            return this;
        }
        LinkedList<JkScopedDependency> deps = new LinkedList<>(dependencies);
        deps.removeLast();
        return new JkDependencySet(deps, globalExclusions, versionProvider);
    }

    /**
     * Returns a clone of this dependencies but replacing the unscoped
     * dependencies with the specified ones.
     */
    public JkDependencySet withDefaultScope(JkScope... scopes) {
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
            if (dep.getScopeType().equals(ScopeType.UNSET) && (dep.withDependency() instanceof JkModuleDependency)) {
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
            if (scopedDependency.withDependency() instanceof JkModuleDependency) {
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
                depList.add(scopedDependency.withDependency());
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
            final JkDependency dependency = scopedDependency.withDependency();
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
            if (scopedDependency.withDependency() instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency
                        .withDependency();
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
            if (scopedDependency.withDependency() instanceof JkModuleDependency) {
                final JkModuleDependency externalModule = (JkModuleDependency) scopedDependency
                        .withDependency();
                final JkVersion version = externalModule.getVersion();
                if (version.isDynamicAndResovable()) {
                    return true;
                }
            }
        }
        return false;
    }

    public JkDependencySet withGlobalExclusion(JkDepExclude exclude) {
        Set<JkDepExclude> depExcludes = new HashSet<>(this.globalExclusions);
        depExcludes.add(exclude);
        return new JkDependencySet(this.dependencies, depExcludes, this.versionProvider);
    }

    /**
     * Returns a set a dependency set identical to this one but excluding the specified exclude
     * from the transitive dependencies of the specified module.
     */
    public JkDependencySet withLocalExclusion(JkModuleId fromModule, JkDepExclude exclude) {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (JkScopedDependency dep : this) {
            if (dep.withDependency() instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) dep.withDependency();
                JkScopedDependency scopedDep = dep.withDependency(moduleDependency.andExclude(exclude));
                list.add(scopedDep);
            } else {
                list.add(dep);
            }
        }
        return new JkDependencySet(list, this.globalExclusions, this.versionProvider);
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

    public JkDependencySet toResolvedModuleVersions() {
        final List<JkScopedDependency> list = new LinkedList<>();
        for (JkScopedDependency dep : this) {
            if (dep.withDependency() instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) dep.withDependency();
                if (moduleDependency.getVersion().isUnspecified()) {
                    JkVersion providedVersion = this.versionProvider.getVersionOf(moduleDependency.getModuleId());
                    if (providedVersion != null) {
                        moduleDependency = moduleDependency.withVersion(providedVersion);
                    }
                }
                JkScopedDependency scopedDep = dep.withDependency(moduleDependency);
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
            if (scopedDependency.withDependency() instanceof JkModuleDependency) {
                final JkModuleDependency moduleDep = (JkModuleDependency) scopedDependency
                        .withDependency();
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
            if (scopedDependency.withDependency() instanceof JkModuleDependency) {
                result.add(scopedDependency);
            }
        }
        return JkDependencySet.of(result);
    }

    private List<JkModuleDependency> extractModuleDependencies() {
        final List<JkModuleDependency> result = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : this) {
            if (scopedDependency.withDependency() instanceof JkModuleDependency) {
                result.add((JkModuleDependency) scopedDependency.withDependency());
            }
        }
        return result;
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static JkDependencySet ofTextDescription(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return ofTextDescription(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @see #ofTextDescription(String)
     */
    public static JkDependencySet ofTextDescription(InputStream inputStream) {
        String content = JkUtilsIO.readAsString(inputStream);
        return ofTextDescription(content);
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
        String[] lines = description.split(System.lineSeparator());
        JkScope[] currentScopes = JkJavaDepScopes.COMPILE_AND_RUNTIME;
        List<JkScopedDependency> list = new LinkedList<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (line.startsWith("-")) {
                currentScopes = translateToScopes(line);
                continue;
            }
            JkModuleDependency dependency = JkModuleDependency.of(line.trim());
            JkScopedDependency scopedDependency = JkScopedDependency.of(dependency, currentScopes);
            list.add(scopedDependency);
        }
        return JkDependencySet.of(list);
    }

    private static JkScope[] translateToScopes(String line) {
        String payload = JkUtilsString.substringAfterFirst(line,"-");
        String[] items = payload.split(" ");
        List<JkScope> result = new LinkedList<>();
        for (String item : items) {
            if (JkUtilsString.isBlank(item)) {
                continue;
            }
            JkScope javaDcope = JkJavaDepScopes.of(item.trim());
            if (javaDcope != null) {
                result.add(javaDcope);
            } else {
                result.add(JkScope.of(item.trim()));
            }
        }
        return result.toArray(new JkScope[0]);
    }

}
