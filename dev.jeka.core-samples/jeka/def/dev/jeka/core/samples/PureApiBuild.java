package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkInit;

/**
 * This should be run with org.jerkar.samples as working dir.
 */
public class PureApiBuild extends JkCommandSet {

    public void doDefault() {
        JkJavaProject javaProject = JkJavaProject.ofMavenLayout(this.getBaseDir());
        javaProject.getMaker().setOutLayout(javaProject.getMaker().getOutLayout().withOutputDir("build/output/alt-output"));
        JkDependencySet deps = JkDependencySet.of().and(JkPopularModules.JUNIT, "4.12", JkJavaDepScopes.TEST);
        javaProject.getDependencyManagement().addDependencies(deps);
        javaProject.getMaker().reset();
        javaProject.getMaker().makeAllArtifacts();
    }

    // Bar project depends on Foo
    public void doMultiProject() {

        JkVersionProvider versionProvider = JkVersionProvider.of()
                .and("com.google.guava:guava", "21.0")
                .and("junit:junit", "4.12");

        JkJavaProject fooProject = JkJavaProject.ofMavenLayout(this.getBaseDir().resolve("foo"));
        fooProject.getDependencyManagement().addDependencies(JkDependencySet.of()
                .and("junit:junit", JkJavaDepScopes.TEST)
                .and("com.google.guava:guava")
                .and("com.sun.jersey:jersey-server:1.19.4")
                .withVersionProvider(versionProvider)
        );
        fooProject.getMaker().addJavadocArtifact();  // Generate Javadoc by default

        JkJavaProject barProject = JkJavaProject.ofMavenLayout(this.getBaseDir().resolve("bar"));
        fooProject.getDependencyManagement().addDependencies(JkDependencySet.of()
                .and("junit:junit", JkJavaDepScopes.TEST)
                .and("com.sun.jersey:jersey-server:1.19.4")
                .and(fooProject)
        );
        barProject.getMaker().defineMainArtifactAsFatJar(true); // Produced jar will embed dependencies
        barProject.getMaker().reset().makeAllArtifacts();  // Creates Bar jar along Foo jar (if not already built)
    }

    public static void main(String[] args) {
        JkInit.instanceOf(PureApiBuild.class, args).doDefault();
    }
}
