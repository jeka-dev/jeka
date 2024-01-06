import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupportSupplier;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectSourceGenerator;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * A pretty exhaustive usage of {@link JkProject} api.
 */
@JkInjectClasspath("../../plugins/dev.jeka.plugins.jacoco/jeka/output/dev.jeka.jacoco-plugin.jar")
@JkInjectClasspath("org.eclipse.jdt:ecj:3.25.0")  // Inject Eclipse compiler that we are using in this build
class JkProjectBuild extends KBean implements JkIdeSupportSupplier {

    IntellijKBean intelliKBean = load(IntellijKBean.class)
            .replaceLibByModule("dev.jeka.jacoco-plugin.jar", "dev.jeka.plugins.jacoco")
            .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");

    /*
     * Configures plugins to be bound to this command class. When this method is called, option
     * fields have already been injected from command line.
     */
    private JkProject project() {
        JkProject project = JkProject.of().setBaseDir(getBaseDir());

        project.packActions.set(project.packaging::createBinJar, project.packaging::createFatJar);

        // Control on how dependency resolver behavior
        project
            .dependencyResolver
                .setUseCache(true)
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
                .configureDependencies(deps -> deps
                    .and("com.google.api-client:google-api-client:1.30.7")
                        .withLocalExclusions("com.google.guava:guava")  // remove dependency to avoid conflict
                    .and("com.google.guava:guava:28.0-jre")
                    .and("org.codehaus.plexus:plexus-container-default:2.1.0")
                );

        // Control on 'test code compilation' (same as for 'prod code')
        project
            .testing
                .compilation
                    .addJavaCompilerOptions("-g")
                    .configureDependencies(deps -> deps
                            .and("org.junit.jupiter:junit-jupiter:5.10.1")
                    );

        // Control on test selection to run
        project
            .testing
                .testSelection
                    .addIncludeStandardPatterns()
                    // ...
                    .addIncludePatterns(JkTestSelection.IT_INCLUDE_PATTERN);

        // Control on test process
        project
            .testing
                .testProcessor
                    .setForkingProcess(true)
                    // ...
                    .engineBehavior
                        //...
                        .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.TREE);
        project
            .packaging
                .javadocProcessor.addOptions("--enable-preview");
        project
            .packaging
                .customizeFatJarContent(pathTreeSet ->  pathTreeSet
                        .withMatcher(JkPathMatcher.of(false, "**/*.jks")))
                .configureRuntimeDependencies(deps -> deps
                        .minus("org.codehaus.plexus:plexus-container-default")
                        .and("com.h2database:h2:2.2.224")
                )
                .manifest
                    .addMainAttribute("Build-by", "JeKa")
                    .addMainAttribute("BBuild-time", LocalDateTime.now().toString());

        // Control on publication
        project.mavenPublication
                .setModuleId("dev.jeka.examples:my-sample")
                .setVersion("1.0.0")
                .setDefaultSigner(path -> path)   // sign published artifact
                .setRepos(JkRepoSet.of("https://my.org.repository/internal"))

                    .configureDependencies(deps -> deps  // Fine tune published transitive dependencies
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

        project.createIvyPublication()
                    .setRepos(JkRepoSet.of("https://my.ivy.repo"));
                //... similar to Maven

        return project;
    }

    public void cleanPack() {
        project().clean().pack();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project().getJavaIdeSupport();
    }

    public static class MySourceGenerator extends JkProjectSourceGenerator {

        @Override
        protected String getDirName() {
            return "my-generated-prop-resources";
        }

        @Override
        protected void generate(JkProject project, Path generatedSourceDir) {
            Path resources = generatedSourceDir.resolve("org/example/my-resources.properties");
            JkUtilsPath.createDirectories(resources.getParent());
            JkUtilsPath.write(resources, (
                    "my-value=2\n" +
                    "my-build-time=" + LocalDateTime.now())
                .getBytes(StandardCharsets.UTF_8));
        }
    }

}