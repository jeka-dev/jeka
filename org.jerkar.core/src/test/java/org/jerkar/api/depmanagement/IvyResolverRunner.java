package org.jerkar.api.depmanagement;

import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;
import static org.jerkar.api.depmanagement.JkScopedDependencyTest.COMPILE;
import static org.jerkar.api.depmanagement.JkScopedDependencyTest.TEST;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.system.JkLog;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * @formatter:off
 */
@SuppressWarnings("javadoc")
public class IvyResolverRunner {

    // static final JkRepos REPOS = JkRepos
    //         .maven(JkOptions.get("repo.download.url"));

    static final JkRepos REPOS = JkRepos.mavenCentral();

    public static void main(String[] args) {
        JkLog.verbose(true);
        //spring();
        // hibernate();
        // jogl();
        // joglWithSource();
        // testPublishIvy();
        //springJdbc();
        //sourceAndJavadoc();
        get();
    }

    public static void spring() {
        final JkRepos repos = REPOS;
        // JkRepos repos = JkRepos.mavenCentral();
        final JkDependencies deps = JkDependencies.builder()
                .on("org.springframework", "spring-jdbc", "3.0.+").mapScope(COMPILE)
                .to("compile", "default").and(JkScopedDependencyTest.PROVIDED).to("provided")
                .excludeGlobally("org.springframework", "spring-core").build();
        final JkResolutionParameters params = JkResolutionParameters.of();
        final JkResolveResult resolveResult = IvyResolver.of(repos).resolve(null,deps,
                params, JkVersionProvider.empty(), COMPILE);
        for (final File file : resolveResult.localFiles()) {
            System.out.println(file.getAbsolutePath());
        }
        System.out.println(deps.resolvedWith(resolveResult.involvedModules()));
    }

    public static void hibernate() {
        final JkDependencies deps = JkDependencies.builder()
                .on("org.hibernate:hibernate-core:4.3.7.Final")
                .excludeLocally("org.jboss.logging", "*").excludeLocally("antlr", "*")
                .scope(COMPILE).excludeGlobally("dom4j", "*").build();
        final JkResolveResult resolveResult = IvyResolver.of(REPOS).resolve(null,deps,
                JkResolutionParameters.of(), JkVersionProvider.empty(), COMPILE);
        for (final File file : resolveResult.localFiles()) {
            System.out.println(file.getAbsolutePath());
        }
        System.out.println(deps.resolvedWith(resolveResult.involvedModules()));
    }

    public static void jogl() {
        final JkRepos repos = JkRepos.mavenCentral().andMavenCentral();
        final JkDependencies deps = JkDependencies.builder()
                .on("org.apache.cocoon.all:cocoon-all:3.0.0-alpha-3").scope(COMPILE).build();
        final JkResolveResult resolveResult = IvyResolver.of(repos).resolve(null, deps,
                JkResolutionParameters.of().withDefault(defaultMapping()), JkVersionProvider.empty(), COMPILE);
        for (final File file : resolveResult.localFiles()) {
            System.out.println(file.getAbsolutePath());
        }
        System.out.println("--- " + resolveResult.localFiles().size());
    }

    private static JkScopeMapping defaultMapping() {
        return JkScopeMapping.of(COMPILE).to("compile", "archive(master)")
                .and(JkScopedDependencyTest.RUNTIME).to("runtime", "archive(master)");
    }

    public static void sourceAndJavadoc() {
        final IvyResolver ivyResolver = IvyResolver.of(REPOS);
        JkModuleDependency dep = JkModuleDependency.of(
                JkPopularModules.GUAVA, "19.0").classifier("sources");
        JkDependencies deps = JkDependencies.builder().on(dep, JkJavaBuild.COMPILE).build();
        JkResolveResult result = ivyResolver.resolve(null, deps,
                JkResolutionParameters.of(), JkVersionProvider.empty(), JkScope.of("*"));
        System.out.println(result.errorReport());
    }

    public static void get() {
        final IvyResolver ivyResolver = IvyResolver.of(REPOS);
        JkModuleDependency dep = JkModuleDependency.of(
                JkPopularModules.GUAVA, "19.0").classifier("javadoc");
        File file = ivyResolver.get(dep);
        System.out.println(file);
    }

}
