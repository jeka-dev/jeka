package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.project.JkJavaDepScopes;
import org.jerkar.api.java.project.JkJavaProject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This should be run with org.jerkar.samples as workinf dir.
 */
public class PureApiBuild {

    public static void main(String[] args) {
        JkJavaProject javaProject = JkJavaProject.of();

        // We want to output stuff in another place than build/output
        javaProject.setOutLayout(javaProject.getOutLayout().withOutputBaseDir(new File("build/output/alt-output")));
        javaProject.setDependencies(JkDependencies.builder()
                .on(JkPopularModules.JUNIT, "4.12").scope(JkJavaDepScopes.TEST)
                .build());
        javaProject.setSourceAndTargetVersion(JkJavaVersion.V7);

        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepos.mavenCentral());
        Map<String, String> options = new HashMap<String, String>();
        JkJavaCompiler compiler = JkJavaCompiler.base();
        JkUnit junit = JkUnit.of();

        javaProject.getOutLayout().deleteDirs();
        javaProject.buildMainJar(dependencyResolver, compiler, junit, options);
        javaProject.generateJavadoc(dependencyResolver, null, options);

    }
}
