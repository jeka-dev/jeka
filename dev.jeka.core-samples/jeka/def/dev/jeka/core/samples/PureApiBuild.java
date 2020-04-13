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

    public void cleanBuild() {
        clean();
        JkJavaProject javaProject = JkJavaProject.of().setBaseDir(this.getBaseDir());
        javaProject.setOutputDir("jeka/output/alt-output");
        JkDependencySet deps = JkDependencySet.of().and(JkPopularModules.JUNIT, "4.12", JkJavaDepScopes.TEST);
        javaProject.getDependencyManagement().addDependencies(deps);
        javaProject.getArtifactProducer().makeAllArtifacts();
    }

    // Bar project depends on Foo
    public void doMultiProject() {

        JkVersionProvider versionProvider = JkVersionProvider.of()
                .and("com.google.guava:guava", "21.0")
                .and("junit:junit", "4.12");

        JkJavaProject fooProject = JkJavaProject.of()
            .setBaseDir(this.getBaseDir().resolve("foo"))
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                    .and("junit:junit", JkJavaDepScopes.TEST)
                    .and("com.google.guava:guava")
                    .and("com.sun.jersey:jersey-server:1.19.4")
                    .withVersionProvider(versionProvider)).__;

        JkJavaProject barProject = JkJavaProject.of()
            .setBaseDir(this.getBaseDir().resolve("bar"))
            .getDependencyManagement().addDependencies(JkDependencySet.of()
                .and("junit:junit", JkJavaDepScopes.TEST)
                .and("com.sun.jersey:jersey-server:1.19.4")
                .and(fooProject)).__;

        barProject.getArtifactProducer()
            .putMainArtifact(barProject.getPackaging()::createFatJar) // Produced jar will embed dependencies
            .makeAllArtifacts();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(PureApiBuild.class, args).cleanBuild();
    }
}
