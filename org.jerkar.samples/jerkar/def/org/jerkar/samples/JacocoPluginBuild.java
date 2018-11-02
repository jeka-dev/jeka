package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.jacoco.JkPluginJacoco;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

/**
 * This build deletes artifacts, compiles, tests and launches SonarQube analyse.
 */
public class JacocoPluginBuild extends JkJavaProjectBuild {

    @Override
    protected void setup() {
        java().project()
                .setDependencies(JkDependencySet.of()
                .and(GUAVA, "18.0")
                .and(JUNIT, "4.11", JkJavaDepScopes.TEST));
        getPlugins().get(JkPluginJacoco.class);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(JacocoPluginBuild.class, args).java().project().getMaker().test();
    }

}
