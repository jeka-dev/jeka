import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet.Hint;
import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.*;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectClasspath;

/**
 * This build illustrate partially what is doable to configure through the project flat facade.
 *
 * The dependencies declared here are only for demonstration purpose and not necessary to build.
 * the project.
 */
@JkInjectClasspath("../../plugins/dev.jeka.plugins.jacoco/jeka/output/dev.jeka.jacoco-plugin.jar")
class FlatFacadeBuild extends JkBean implements JkIdeSupportSupplier {

    @JkDoc("Tell if the Integration Tests should be run.")
    public boolean runIT = true;

    private JkProject project() {
        JkProject project =JkProject.of();
        project.flatFacade()

                // Control on JDK version for compilation
                .setJvmTargetVersion(JkJavaVersion.V8)

                // Control on project Layout
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE) // don't use maven layout
                .mixResourcesAndSources()  // java sources and resources are located in same folder

                // Control on produced artifacts
                .includeJavadocAndSources(false, true)
                .setMainArtifactJarType(JkProjectPackaging.JarType.FAT)

                // Simple declaration of project dependencies
                .addCompileDeps(
                        "com.google.code.gson:gson:2.10.1",
                        "log4j:log4j:1.2.17"
                )
                .addCompileOnlyDeps(
                        "javax.servlet:javax.servlet-api:4.0.1"
                )

                // Fine control on project dependencies
                .configureCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:22.0").withLocalExclusions(
                                    "com.google.j2objc:j2objc-annotations",
                                    "com.google.code.findbugs")

                        .and("com.github.djeang:vincer-dom:1.4.0")
                        .and("org.projectlombok:lombok:1.18.30")

                        .and("com.fasterxml.jackson:jackson-bom::pom:2.16.0")
                        .and("com.fasterxml.jackson.core:jackson-core", JkTransitivity.NONE)
                        .and("com.fasterxml.jackson.core:jackson-databind", JkTransitivity.RUNTIME)
                )
                .configureRuntimeDependencies(deps -> deps
                        .and(Hint.before(JkCoordinateDependency.of("com.github.djeang:vincer-dom")),
                                "commons-codec:commons-codec:1.16.0")
                        .minus("org.projectlombok:lombok")
                        .withMoving(Hint.first(), "com.fasterxml.jackson.core:jackson-databind")

                )
                .configureTestDependencies(deps -> deps
                        .and(JkPopularLibs.JUNIT_5.toCoordinate("5.8.1"))
                )

                // Control on Test behavior
                .addTestIncludeFilterSuffixedBy("IT", runIT)

                // Control on published artifact and versions
                .setPublishedModuleId("org.jerkar:examples-java-flat-facade")
                .setPublishedVersionFromGitTag();

        // Here we are modifying the dependencies mentioned in the published POM
        project.publication.maven
                .configureDependencies(deps -> deps
                        .minus("com.fasterxml.jackson.core")
                        .withTransitivity("com.github.djeang:vincer-dom", JkTransitivity.RUNTIME)
                );

        return project;
    }

    public void cleanPack() {
        project().clean().pack();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project().getJavaIdeSupport();
    }
}

