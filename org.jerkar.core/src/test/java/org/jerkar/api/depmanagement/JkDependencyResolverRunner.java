package org.jerkar.api.depmanagement;

//import static org.jerkar.api.depmanagement.JkScopedDependencyTest.COMPILE;

import java.io.File;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;

@SuppressWarnings("javadoc")
public class JkDependencyResolverRunner {

    static final JkRepos REPOS = IvyResolverRunner.REPOS;

    public static void main(String[] args) {
        JkLog.verbose(true);
        hibernate();
    }

    public static void hibernate() {
        final JkDependencies deps = JkDependencies.builder()
                .on("org.hibernate:hibernate-core:4.3.7.Final").excludeLocally("dom4j", "dom4j")
                .excludeGlobally("antlr", "antlr").excludeGlobally("org.jboss.logging", "*")
                .build();// .withDefaultScope(COMPILE);
        // final JkResolutionParameters params =
        // JkResolutionParameters.defaultScopeMapping((COMPILE.mapTo("compile(*)")));
        final JkDependencyResolver resolver = JkDependencyResolver.managed(REPOS, deps);// .withParams(params);
        final JkResolveResult resolveResult = resolver.resolve();
        for (final File file : resolveResult.localFiles()) {
            System.out.println(file.getAbsolutePath());
        }
        System.out.println(deps.resolvedWith(resolveResult.involvedModules()));
    }



}
