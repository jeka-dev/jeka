package dev.jeka.core.api.depmanagement;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A bunch of {@link JkQualifiedDependency}
 */
public class JkQualifiedDependencies {

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

    private final List<JkQualifiedDependency> qualifiedDependencies;

    // Transitive dependencies globally excluded
    private final Set<JkDependencyExclusion> globalExclusions;

    private final JkVersionProvider versionProvider;

    private JkQualifiedDependencies(List<JkQualifiedDependency> qualifiedDependencies, Set<JkDependencyExclusion>
            globalExclusions, JkVersionProvider versionProvider) {
        this.qualifiedDependencies = Collections.unmodifiableList(qualifiedDependencies);
        this.globalExclusions = Collections.unmodifiableSet(globalExclusions);
        this.versionProvider = versionProvider;
    }

    public static JkQualifiedDependencies of() {
        return new JkQualifiedDependencies(Collections.emptyList(), Collections.emptySet(), JkVersionProvider.of());
    }

    public static JkQualifiedDependencies ofDependencies(List<JkDependency> dependencies) {
        return of(dependencies.stream().map(dep -> JkQualifiedDependency.of(null, dep)).collect(Collectors.toList()));
    }

    public static JkQualifiedDependencies of(List<JkQualifiedDependency> qualifiedDependencies) {
        return new JkQualifiedDependencies(qualifiedDependencies, Collections.emptySet(), JkVersionProvider.of());
    }

    public static JkQualifiedDependencies of(JkDependencySet dependencySet) {
        return ofDependencies(dependencySet.getDependencies())
                .withGlobalExclusions(dependencySet.getGlobalExclusions())
                .withVersionProvider(dependencySet.getVersionProvider());
    }

    public List<JkQualifiedDependency> getQualifiedDependencies() {
        return qualifiedDependencies;
    }

    public List<JkDependency> getDependencies() {
        return qualifiedDependencies.stream()
                .map(JkQualifiedDependency::getDependency)
                .collect(Collectors.toList());
    }

    public List<JkModuleDependency> getModuleDependencies() {
        return qualifiedDependencies.stream()
                .map(JkQualifiedDependency::getDependency)
                .filter(JkModuleDependency.class::isInstance)
                .map(JkModuleDependency.class::cast)
                .collect(Collectors.toList());
    }

    public Set<JkDependencyExclusion> getGlobalExclusions() {
        return globalExclusions;
    }

    public JkVersionProvider getVersionProvider() {
        return versionProvider;
    }

    public List<JkQualifiedDependency> findByModule(String moduleId) {
        JkModuleDependency moduleDependency = JkModuleDependency.of(moduleId);
        return this.qualifiedDependencies.stream()
                .filter(qDep -> qDep.getDependency().equals(moduleDependency))
                .collect(Collectors.toList());
    }

    public JkQualifiedDependencies remove(JkDependency dependency) {
        List<JkQualifiedDependency> dependencies = qualifiedDependencies.stream()
                .filter(qDep -> !qDep.equals(dependency))
                .collect(Collectors.toList());
        return new JkQualifiedDependencies(dependencies, globalExclusions, versionProvider);
    }

    public JkQualifiedDependencies and(JkQualifiedDependency qualifiedDependency) {
        List<JkQualifiedDependency> result = new LinkedList<>(this.qualifiedDependencies);
        result.add(qualifiedDependency);
        return new JkQualifiedDependencies(result, globalExclusions, versionProvider);
    }

    public JkQualifiedDependencies and(String qualifier, JkDependency dependency) {
        return and(JkQualifiedDependency.of(qualifier, dependency));
    }

    public JkQualifiedDependencies and(String qualifier, String moduleDependencyDescriptor) {
        return and(qualifier, JkModuleDependency.of(moduleDependencyDescriptor));
    }

    public JkQualifiedDependencies remove(String dep) {
        return remove(JkModuleDependency.of(dep));
    }

    public JkQualifiedDependencies replaceQualifier(JkDependency dependency, String qualifier) {
        List<JkQualifiedDependency> dependencies = qualifiedDependencies.stream()
                .map(qDep -> qDep.getDependency().equals(dependency) ? qDep.withQualifier(qualifier) : qDep)
                .collect(Collectors.toList());
        return new JkQualifiedDependencies(dependencies, globalExclusions, versionProvider);
    }

    public JkQualifiedDependencies replaceQualifier(String dependency, String qualifier) {
        return replaceQualifier(JkModuleDependency.of(dependency), qualifier);
    }

    public JkQualifiedDependencies withModuleDependenciesOnly() {
        List<JkQualifiedDependency> dependencies = qualifiedDependencies.stream()
                .filter(qDep -> qDep.getDependency() instanceof JkModuleDependency)
                .collect(Collectors.toList());
        return new JkQualifiedDependencies(dependencies, globalExclusions, versionProvider);
    }

    /**
     * These exclusions only stands for dependencies that are retrieved transitively. This means that
     * this not involves dependencies explicitly declared here.
     */
    public JkQualifiedDependencies withGlobalExclusions(Set<JkDependencyExclusion> exclusions) {
        Set<JkDependencyExclusion> newExclusions = new HashSet<>(this.globalExclusions);
        newExclusions.addAll(exclusions);
        return new JkQualifiedDependencies(qualifiedDependencies, Collections.unmodifiableSet(newExclusions),
                versionProvider);
    }

    /**
     * These exclusions only stands for dependencies that are retrieved transitively. This means that
     * this not involves dependencies explicitly declared here.
     */
    public JkQualifiedDependencies withVersionProvider(JkVersionProvider versionProvider) {
        return new JkQualifiedDependencies(qualifiedDependencies, globalExclusions, this.versionProvider
            .and(versionProvider));
    }

    public JkQualifiedDependencies replaceUnspecifiedVersionsWithProvider() {
        List<JkQualifiedDependency> dependencies = qualifiedDependencies.stream()
                .map(qDep -> {
                    if (qDep.getDependency() instanceof JkModuleDependency) {
                        JkModuleDependency moduleDependency = (JkModuleDependency) qDep.getDependency();
                        JkVersion providedVersion = versionProvider.getVersionOf(moduleDependency.getModuleId());
                        if (moduleDependency.getVersion().isUnspecified() && providedVersion != null) {
                            return JkQualifiedDependency.of(qDep.getQualifier(),
                                    moduleDependency.withVersion(providedVersion));
                        }
                    }
                    return qDep;
                })
                .collect(Collectors.toList());
        return new JkQualifiedDependencies(dependencies, globalExclusions, versionProvider);
    }

    public static JkQualifiedDependencies computeMavenPublishDependencies(JkDependencySet compileDeps,
                                                                      JkDependencySet runtimeDeps,
                                                                      JkVersionedModule.ConflictStrategy strategy) {
        JkDependencySetMerge merge = compileDeps.merge(runtimeDeps);
        List<JkQualifiedDependency> result = new LinkedList<>();
        for (JkModuleDependency moduleDependency : merge.getResult().normalised(strategy)
                .assertNoUnspecifiedVersion().getVersionedModuleDependencies()) {
            String scope = RUNTIME_SCOPE;
            if (merge.getAbsentDependenciesFromRight().contains(moduleDependency)) {
                scope = COMPILE_SCOPE;
            }
            result.add(JkQualifiedDependency.of(scope, moduleDependency));
        }
        return new JkQualifiedDependencies(result, merge.getResult().getGlobalExclusions(),
                merge.getResult().getVersionProvider());
    }

    public static JkQualifiedDependencies computeIdeDependencies(JkDependencySet compileDeps,
                                                                 JkDependencySet runtimeDeps,
                                                                 JkDependencySet testDeps,
                                                                 JkVersionedModule.ConflictStrategy strategy) {
        JkDependencySetMerge mergeWithProd = compileDeps.merge(runtimeDeps);
        JkDependencySetMerge mergeWithTest = mergeWithProd.getResult().merge(testDeps);
        List<JkQualifiedDependency> result = new LinkedList<>();
        for (JkDependency dependency : mergeWithTest.getResult().normalised(strategy)
                .assertNoUnspecifiedVersion().getVersionedDependencies()) {
            final String scope;
            if (mergeWithProd.getResult().getDependencies().contains(dependency)) {
                if (mergeWithProd.getAbsentDependenciesFromRight().contains(dependency)) {
                    scope = PROVIDED_SCOPE;
                } else if (mergeWithProd.getAbsentDependenciesFromLeft().contains(dependency)) {
                    scope = RUNTIME_SCOPE;
                } else {
                    scope = COMPILE_SCOPE;
                }
            } else {
                scope = TEST_SCOPE;
            }
            result.add(JkQualifiedDependency.of(scope, dependency));
        }
        return new JkQualifiedDependencies(result, mergeWithTest.getResult().getGlobalExclusions(),
                mergeWithTest.getResult().getVersionProvider());
    }

    public static JkQualifiedDependencies computeIdeDependencies(JkDependencySet compileDeps,
                                                                 JkDependencySet runtimeDeps,
                                                                 JkDependencySet testDeps) {
        return computeIdeDependencies(compileDeps, runtimeDeps, testDeps, JkVersionedModule.ConflictStrategy.FAIL);
    }

    public static JkQualifiedDependencies computeIvyPublishDependencies(JkDependencySet compileDeps,
                                                                        JkDependencySet runtimeDeps,
                                                                        JkDependencySet testDeps,
                                                                        JkVersionedModule.ConflictStrategy strategy) {
        JkDependencySetMerge mergeWithProd = compileDeps.merge(runtimeDeps);
        JkDependencySetMerge mergeWithTest = mergeWithProd.getResult().merge(testDeps);
        List<JkQualifiedDependency> result = new LinkedList<>();
        for (JkModuleDependency dependency : mergeWithTest.getResult().normalised(strategy)
                .assertNoUnspecifiedVersion().getVersionedModuleDependencies()) {
            final String configurationSource;
            String configurationTarget;
            if (mergeWithProd.getResult().getDependencies().contains(dependency)) {
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
        return new JkQualifiedDependencies(result, mergeWithTest.getResult().getGlobalExclusions(),
                mergeWithTest.getResult().getVersionProvider());
    }

    public static String getIvyTargetConfigurations(JkTransitivity transitivity) {
        return TRANSITIVITY_TARGET_CONF_MAP.get(transitivity);
    }

    public List<JkDependency> getDependenciesHavingQualifier(String ... qualifiers) {
        List<String> list = Arrays.asList(qualifiers);
        return qualifiedDependencies.stream()
                .filter(qDep -> list.contains(qDep))
                .map(JkQualifiedDependency::getDependency)
                .collect(Collectors.toList());
    }



}
