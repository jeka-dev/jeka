package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.project.java.JkJarProject;
import org.jerkar.api.project.java.JkJavaCompileVersion;

import java.io.File;

/**
 * This should be run with org.jerkar.samples as workinf dir.
 */
public class PureApiBuild {

    public static void main(String[] args) {
        JkJarProject javaProject = new JkJarProject(new File("."));

        // We want to output stuff in another place than build/output
        javaProject.setOutLayout(javaProject.getOutLayout().withOutputBaseDir(new File("build/output/alt-output")));
        javaProject.setDependencies(JkDependencies.builder()
                .on(JkPopularModules.JUNIT, "4.12").scope(JkJavaDepScopes.TEST)
                .build());
        javaProject.setCompileVersion(JkJavaCompileVersion.V6);

        JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepos.mavenCentral());
        JkJavaCompiler compiler = JkJavaCompiler.base();
        JkUnit junit = JkUnit.of();
        javaProject.getOutLayout().deleteDirs();
        javaProject.doMainJar();
        javaProject.generateJavadoc();

    }
}
