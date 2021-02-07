package dev.jeka.core.api.depmanagement.tooling;

import dev.jeka.core.api.depmanagement.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

    public static final String TEST_TARGET_CONF = "runtime(default)";



    private final List<JkQualifiedDependency> entries;

    private JkQualifiedDependencies(List<JkQualifiedDependency> entries) {
        this.entries = entries;
    }

    public static JkQualifiedDependencies of() {
        return new JkQualifiedDependencies(Collections.emptyList());
    }

    public static JkQualifiedDependencies ofDependencies(List<JkDependency> dependencies) {
        return of(dependencies.stream().map(dep -> JkQualifiedDependency.of(null, dep)).collect(Collectors.toList()));
    }

    public static JkQualifiedDependencies of(List<JkQualifiedDependency> qualifiedDependencies) {
        return new JkQualifiedDependencies(qualifiedDependencies);
    }

    public List<JkQualifiedDependency> getEntries() {
        return entries;
    }

    public List<JkModuleDependency> getModuleDependencies() {
        return entries.stream()
                .map(JkQualifiedDependency::getDependency)
                .filter(JkModuleDependency.class::isInstance)
                .map(JkModuleDependency.class::cast)
                .collect(Collectors.toList());
    }

    public JkQualifiedDependencies remove(JkDependency dependency) {
        return of(entries.stream()
                .filter(qDep -> !qDep.equals(dependency))
                .collect(Collectors.toList())
        );
    }

    public JkQualifiedDependencies remove(String dep) {
        return remove(JkModuleDependency.of(dep));
    }

    public JkQualifiedDependencies replaceQualifier(JkDependency dependency, String qualifier) {
        return of(entries.stream()
                .map(qDep -> qDep.getDependency().equals(dependency) ? qDep.withQualifier(qualifier) : qDep)
                .collect(Collectors.toList())
        );
    }

    public JkQualifiedDependencies replaceQualifier(String dependency, String qualifier) {
        return replaceQualifier(JkModuleDependency.of(dependency), qualifier);
    }


    public JkQualifiedDependencies withModuleDependenciesOnly() {
        return of(entries.stream()
                .filter(qDep -> qDep.getDependency() instanceof JkModuleDependency)
                .collect(Collectors.toList())
        );
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
        return JkQualifiedDependencies.of(result);
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
        return JkQualifiedDependencies.of(result);
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
                        + RUNTIME_TARGET_CONF + ", " + TEST_SCOPE;
            }
            if (dependency.getTransitivity() == JkTransitivity.NONE) {
                configurationTarget = MASTER_TARGET_CONF;
            } else if (dependency.getTransitivity() == JkTransitivity.COMPILE) {
                configurationTarget = MASTER_TARGET_CONF + ", " + COMPILE_TARGET_CONF;
            } else if (dependency.getTransitivity() == JkTransitivity.RUNTIME) {
                configurationTarget = MASTER_TARGET_CONF + ", " + COMPILE_TARGET_CONF + ", " + RUNTIME_TARGET_CONF;
            }
            String configuration = configurationSource + " -> " + configurationTarget;
            result.add(JkQualifiedDependency.of(configuration, dependency));
        }
        return JkQualifiedDependencies.of(result);
    }

    public List<JkDependency> getDependenciesHavingQualifier(String ... qualifiers) {
        List<String> list = Arrays.asList(qualifiers);
        return entries.stream()
                .filter(qDep -> list.contains(qDep))
                .map(JkQualifiedDependency::getDependency)
                .collect(Collectors.toList());
    }



}
