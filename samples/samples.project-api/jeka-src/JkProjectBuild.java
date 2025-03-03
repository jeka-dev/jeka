import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.project.*;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;


import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * A pretty exhaustive usage of {@link JkProject} api.
 */
@JkDep("../../plugins/plugins.jacoco/jeka-output/dev.jeka.jacoco-plugin.jar")
@JkDep("org.eclipse.jdt:ecj:3.25.0")  // Inject Eclipse compiler that we are using in this build
class JkProjectBuild extends KBean implements JkIdeSupportSupplier {

    JkProject project = project();

    JkMavenPublication mavenPublication = mavenPublication(project);

    JkIvyPublication ivyPublication = ivyPublication(project);

    @JkPostInit
    private void postInt(IntellijKBean intellijKBean) {
        intellijKBean.replaceLibByModule("dev.jeka.jacoco-plugin.jar", "plugins.jacoco")
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core");
    }

    @JkDoc("Clean output and create the bin jar for this project")
    public void cleanPack() {
        project().clean().pack();
    }

    @JkDoc("Pulish on Maven repo")
    public void publish() {
        mavenPublication.publish();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project().getJavaIdeSupport();
    }


    /*
     * Configures plugins to be bound to this command class. When this method is called, option
     * fields have already been injected from command line.
     */
    private JkProject project() {
        JkProject project = JkProject.of().setBaseDir(getBaseDir());

        // we want to create regular and fat jar, when 'pack' is invoked.
        project.packActions.append(project.packaging::createFatJar);

        // Control on how dependency resolver behavior
        project
            .dependencyResolver
                .setUseInMemoryCache(true)
                .getDefaultParams()
                    .setRefreshed(true)
                    .setFailOnDependencyResolutionError(false)
                    .setConflictResolver(JkResolutionParameters.JkConflictResolver.STRICT);

        // Control on overall compiler
        project
            .compilerToolChain
                .setCompileTool(new EclipseCompiler(), "-warn:nullDereference,unusedPrivate");

        // Control on 'production code' compilation
        project
            .compilation
                .addJavaCompilerOptions("-g")
                .addSourceGenerator(new MySourceGenerator()) /// Custom basic source generator
                .dependencies
                    .addWithExclusions("com.google.api-client:google-api-client:1.30.7",
                        "com.google.guava:guava")  // remove dependency to avoid conflict
                    .add("com.google.guava:guava:28.0-jre")
                    .add("org.codehaus.plexus:plexus-container-default:2.1.0");


        // Control on 'test code compilation' (same as for 'prod code')
        project
            .testing
                .compilation
                    .addJavaCompilerOptions("-g")
                    .dependencies
                            .add("org.junit.jupiter:junit-jupiter:5.10.1");

        // Control on test selection to run
        project
            .testing
                .testSelection
                    .addIncludePatterns(JkTestSelection.IT_INCLUDE_PATTERN);

        // Control on test process
        project
            .testing
                .testProcessor
                    .setForkingProcess(true)
                    // ...
                    .engineBehavior
                        //...
                        .setProgressDisplayer(JkTestProcessor.JkProgressStyle.FULL);
        project
            .packaging
                .javadocProcessor.addOptions("--enable-preview");
        project
            .packaging
                .customizeFatJarContent(pathTreeSet ->  pathTreeSet
                        .withMatcher(JkPathMatcher.of(false, "**/*.jks")))
                .runtimeDependencies
                        .remove("org.codehaus.plexus:plexus-container-default")
                        .add("com.h2database:h2:2.2.224");
        return project;
    }

    private JkMavenPublication mavenPublication(JkProject project) {

        JkMavenPublication mavenPublication = JkMavenPublication.of(project.asBuildable());
        mavenPublication
                .setModuleId("dev.jeka.examples:my-sample")
                .setVersion("1.0.0")
                .setDefaultSigner((path1, path2) -> {})   // sign published artifact
                .setRepos(JkRepoSet.of("https://my.org.repository/internal"))

                .customizeDependencies(deps -> deps  // Fine tune published transitive dependencies
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                )
                .pomMetadata   // Metadata required to publish on Maven Central
                .addLicense("A licence", "https://org.my.license")
                .addDeveloper("John", "Doe", "g-mol", "johndoe@gmol.com")
                .setProjectName("My project name")
                .setProjectDescription("My project description")
                .setProjectUrl("https://my.project.url")
                .setScmConnection("git://my.git.repo/for.project")
                .setScmDeveloperConnection("https://my.scn.dev.connectyion");
        return mavenPublication;
    }

    public JkIvyPublication ivyPublication(JkProject project) {
        return JkProjectPublications.ivyPublication(project)
                .setRepos(JkRepoSet.of("https://my.ivy.repo"));
                //... similar to Maven
    }

    public static class MySourceGenerator implements JkProjectSourceGenerator {

        @Override
        public String getDirName() {
            return "my-generated-prop-resources";
        }

        @Override
        public void generate(JkProject project, Path generatedSourceDir) {
            Path resources = generatedSourceDir.resolve("org/example/my-resources.properties");
            JkUtilsPath.createDirectories(resources.getParent());
            JkUtilsPath.write(resources, (
                    "my-value=2\n" +
                    "my-build-time=" + LocalDateTime.now())
                .getBytes(StandardCharsets.UTF_8));
        }
    }

}