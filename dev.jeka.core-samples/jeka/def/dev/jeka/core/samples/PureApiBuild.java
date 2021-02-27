package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkInit;

/**
 * This should be run with org.jerkar.samples as working dir.
 */
public class PureApiBuild extends JkClass {

    public void cleanBuild() {
        clean();
        JkJavaProject javaProject = JkJavaProject.of().setBaseDir(this.getBaseDir());
        javaProject.setOutputDir("jeka/output/alt-output");
        JkDependencySet deps = JkDependencySet.of().and(JkPopularModules.JUNIT.version("4.12"));
        javaProject.getConstruction().getTesting().getCompilation().setDependencies(dep -> deps);
        javaProject.getPublication().getArtifactProducer().makeAllArtifacts();
    }

    // Bar project depends on Foo
    public void doMultiProject() {

        JkVersionProvider versionProvider = JkVersionProvider.of()
                .and("com.google.guava:guava", "21.0")
                .and("junit:junit", "4.12");

        JkJavaProject fooProject = JkJavaProject.of().simpleFacade()
            .setBaseDir(this.getBaseDir().resolve("foo"))
            .setCompileDependencies(deps -> deps
                    .and("com.google.guava:guava")
                    .and("com.sun.jersey:jersey-server:1.19.4")
                    .withVersionProvider(versionProvider))
            .setTestDependencies(deps -> deps
                        .and("junit:junit"))
            .getProject();

        JkJavaProject barProject = JkJavaProject.of().simpleFacade()
            .setBaseDir(this.getBaseDir().resolve("bar"))
            .setCompileDependencies(deps -> deps
                    .and("com.sun.jersey:jersey-server:1.19.4")
                    .and(fooProject.toDependency()))
            .setTestDependencies(deps -> deps
                    .and("junit:junit")
                    .withVersionProvider(versionProvider))
            .getProject();

        barProject.getPublication().getArtifactProducer()
            .putMainArtifact(barProject.getConstruction()::createFatJar) // Produced jar will embed dependencies
            .makeAllArtifacts();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(PureApiBuild.class, args).cleanBuild();
    }
}
