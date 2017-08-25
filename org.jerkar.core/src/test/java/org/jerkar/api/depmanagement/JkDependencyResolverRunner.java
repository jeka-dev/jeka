package org.jerkar.api.depmanagement;

//import static org.jerkar.api.depmanagement.JkScopedDependencyTest.COMPILE;

import java.io.File;

import org.jerkar.api.system.JkLog;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

@SuppressWarnings("javadoc")
public class JkDependencyResolverRunner {

    static final JkRepos REPOS = IvyResolverRunner.REPOS;

    public static void main(String[] args) {
        JkLog.verbose(true);
        spring();
    }

    public static void hibernate() {
        final JkDependencies deps = JkDependencies.builder()
                .on("org.hibernate:hibernate-core:4.3.7.Final").excludeLocally("dom4j", "dom4j")
                .excludeGlobally("antlr", "antlr").excludeGlobally("org.jboss.logging", "*")
                .build();// .withDefaultScope(COMPILE);
        // final JkResolutionParameters params =
        // JkResolutionParameters.defaultScopeMapping((COMPILE.mapTo("compile(*)")));
        final JkDependencyResolver resolver = JkDependencyResolver.of(REPOS);// .withParams(params);
        final JkResolveResult resolveResult = resolver.resolve(deps);
        for (final File file : resolveResult.localFiles()) {
            System.out.println(file.getAbsolutePath());
        }
        System.out.println(deps.resolvedWith(resolveResult.involvedModules()));
    }

    public static void spring() {
        final JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").scope(JkJavaBuild.TEST)
                .build();// .
        final JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral());
        final JkResolveResult resolveResult = resolver.resolve(deps, JkJavaBuild.TEST);
        JkDependencyNode tree = resolveResult.dependencyTree();
        System.out.println(tree.toStrings());
    }



}
