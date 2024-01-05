package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.JkJdks;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.project.*;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.scaffold.ScaffoldKBean;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Plugin for building JVM language based projects. It comes with a {@link JkProject} pre-configured with {@link JkProperties}.
 * and a decoration for scaffolding.
 */
@JkDoc("Provides a configured JkProject instance for building JVM based projects.")
public class ProjectKBean extends KBean implements JkIdeSupportSupplier {

    /**
     * Options for the packaging tasks (jar creation). These options are injectable from command line.
     */
    @JkDoc("") // injectable field, but it has no impact after init() call
    private final JkPackOptions pack = new JkPackOptions();

    /**
     * Options for run tasks
     */
    public final JkRunOptions run = new JkRunOptions();

    /**
     * Options for configuring testing tasks.
     */
    @JkDoc("")
    private final JkTestOptions tests = new JkTestOptions();

    /**
     * Options for configuring scaffold.
     */
    public final JkScaffoldOptions scaffold = new JkScaffoldOptions();

    /**
     * Options for configuring directory layout.
     */
    @JkDoc("")
    private final JkLayoutOptions layout = new JkLayoutOptions();

    /**
     * Options for configuring compilation.
     */
    @JkDoc("")
    private final JkCompilationOptions compilation = new JkCompilationOptions();

    /**
     * Options for Maven publication
     */
    @JkDoc("")
    private final JkPublishOptions mavenPublication = new JkPublishOptions();

    @JkDoc("The output file for the xml dependency description.")
    public Path outputFile;

    public final JkProject project = JkProject.of();

    @Override
    protected void init() {
        configureProject();
        configureScaffold();
    }

    // ------------------------------- command line methods -----------------------------

    @JkDoc("Delete the content of jeka/output directory and might execute extra clean actions")
    public void clean() {
        project.clean();
    }

    @JkDoc("Generate sources")
    public void generateSources() {
        project.compilation.generateSources();
    }

    @JkDoc("Performs compilation and resource processing")
    public void compile() {
        project.compilation.run();
    }

    @JkDoc("Compiles and run tests defined within the project (typically Junit tests)")
    public void test() {    //NOSONAR
        project.testing.run();
    }

    @JkDoc("Generates from scratch artifacts defined through 'pack' options if not yet generated. " +
            "Use #cleanPack to force re-generation.")
    public void pack() {   //NOSONAR
        project.pack();
    }

    @JkDoc("Convenient method to perform a 'clean' followed by a 'pack'.")
    public void cleanPack() {
        clean();
        pack();
    }

    @JkDoc("Displays resolved dependency tree on console.")
    public final void showDependencies() {
        project.displayDependencyTree();
    }

    @JkDoc("Displays resolved dependency tree in xml")
    public void showDependenciesXml() {
        Document document = project.getDependenciesAsXml();
        String xml = JkDomDocument.of(document).toXml();
        if (outputFile == null) {
           JkLog.info(xml);
        } else {
            JkPathFile.of(outputFile).createIfNotExist().write(xml);
        }
    }

    @JkDoc("Displays information about the Java project to build.")
    public void info() {
        JkLog.info(this.project.getInfo());
        JkLog.info("\nExecute 'java#showDependencies' to display details on dependencies.");
    }

    @JkDoc("Run the generated jar.")
    public void runJar() {
        this.run.runJar();
    }

    @JkDoc("Publishes produced artifacts to configured repository.")
    public void publish() {
        JkLog.info("Publish " + project + " ...");
        project.mavenPublication.publish();
    }

    @JkDoc("Publishes produced artifacts to local repository.")
    public void publishLocal() {
        project.mavenPublication.publishLocal();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project.getJavaIdeSupport();
    }

    // ------- static classes for configuration

    /**
     * Standard options for packaging java projects.
     */
    public static class JkPackOptions {

        @JkDoc("Set the type of jar to produce for the main artifact.")
        public JkProjectPackaging.JarType jarType;

    }

    /**
     * Options about tests
     */
    public static final class JkTestOptions {

        /** Turn it on to skip tests. */
        @JkDoc("If true, tests are not run.")
        public Boolean skip;

        /** Turn it on to run tests in a withForking process. */
        @JkDoc("If true, tests will be executed in a forked process.")
        public Boolean fork;

        /** Argument passed to the JVM if tests are withForking. Example : -Xms2G -Xmx2G */
        @JkDoc("Argument passed to the JVM if tests are executed in a forked process (example -Xms2G -Xmx2G).")
        public String jvmOptions;

    }

    public static class JkLayoutOptions {

        @JkDoc("Style of directory source structure (src/main/java or just src)")
        public JkCompileLayout.Style style = JkCompileLayout.Style.MAVEN;

        @JkDoc("If true, Resource files are located in same folder than Java code.")
        public boolean mixSourcesAndResources = false;

    }

    public static class JkCompilationOptions {

        @JkDoc("The target JVM version for compiled files.")
        @JkInjectProperty("jeka.java.version")
        public String javaVersion;

        @JkDoc("Extra arguments to be passed to the compiler (example -Xlint:unchecked).")
        public String compilerExtraArgs;

    }

    public class JkRunOptions {

        @JkDoc("JVM options to use when running generated jar")
        public String jvmOptions;

        @JkDoc("Program arguments to use when running generated jar")
        public String programArgs;

        @JkDoc("If true, the resolved runtime classpath will be used when running the generated jar. " +
                "If the generated jar is a Uber jar or contains all the needed dependencies, leave it to 'false'")
        public boolean useRuntimeDepsForClasspath;

        void runJar() {
            project.runJar(useRuntimeDepsForClasspath, jvmOptions, programArgs).exec();
        }
    }

    public static class JkPublishOptions {

        /** When true, javadoc is created and packed in a jar file.*/
        @JkDoc("If true, javadoc jar is added in the list of artifact to produce/publish.")
        public boolean javadoc = true;

        /** When true, sources are packed in a jar file.*/
        @JkDoc("If true, sources jar is added in the list of artifact to produce/publish.")
        public Boolean sources = true;

    }

    public static class JkScaffoldOptions {

        @JkDoc("Generate jeka/project-libs sub-folders for hosting local libraries")
        private final boolean generateLocalLibsFolders = false;

        @JkDoc("The template used for scaffolding the build class")
        private final JkProjectScaffold.BuildClassTemplate template = JkProjectScaffold.BuildClassTemplate.SIMPLE_FACADE;

        public JkProjectScaffold.BuildClassTemplate getTemplate() {
            return template;
        }

        public final DependenciesTxt dependenciesTxt = new DependenciesTxt();

        public static class DependenciesTxt {

            @JkDoc("Comma separated dependencies to include in project-dependencies.txt COMPILE section")
            public String compile;

            @JkDoc("Comma separated dependencies to include in project-dependencies.txt RUNTIME section")
            public String runtime;

            @JkDoc("Comma separated dependencies to include in project-dependencies.txt TEST section")
            public String test;

            static List<String> toList(String description) {
                if (description == null) {
                    return Collections.emptyList();
                }
                return Arrays.asList(description.split(","));
            }

        }

    }

    // ------- private methods

    private JkJdks jdks() {
        return JkJdks.ofJdkHomeProps(getRuntime().getProperties().getAllStartingWith("jeka.jdk.", false));
    }

    private void applyRepoConfigOn(JkProject project) {
        JkRepoProperties repoProperties = JkRepoProperties.of(this.getRuntime().getProperties());
        JkRepoSet mavenPublishRepos = repoProperties.getPublishRepository();
        if (mavenPublishRepos.getRepos().isEmpty()) {
            mavenPublishRepos = mavenPublishRepos.and(JkRepo.ofLocal());
        }
        project.mavenPublication.setRepos(mavenPublishRepos);

        // set dependency resolver
        final JkRepoSet downloadRepos = repoProperties.getDownloadRepos();
        if (!downloadRepos.getRepos().isEmpty()) {
            project.dependencyResolver.setRepos(downloadRepos);
        }
    }

    private void configureProject() {
        project.setBaseDir(getBaseDir());
        if (!JkLog.isAcceptAnimation()) {
            project.testing.testProcessor.engineBehavior.setProgressDisplayer(
                    JkTestProcessor.JkProgressOutputStyle.SILENT);
        }
        if (!JkUtilsString.isBlank(compilation.javaVersion)) {
            JkJavaVersion version = JkJavaVersion.of(compilation.javaVersion);
            project.setJvmTargetVersion(version);
        }
        applyRepoConfigOn(project);
        project.flatFacade().setLayoutStyle(layout.style);
        if (layout.mixSourcesAndResources) {
            project.flatFacade().mixResourcesAndSources();
        }
        JkJavaCompilerToolChain compilerToolChain = project.compilerToolChain;
        if (!compilerToolChain.isToolOrProcessSpecified()) {
            compilerToolChain.setJdkHints(jdks(), true);
        }
        project.flatFacade().publishJavadocAndSources(mavenPublication.javadoc, mavenPublication.sources);
        if (pack.jarType != null) {
            project.flatFacade().setMainArtifactJarType(pack.jarType);
        }
        JkTestProcessor testProcessor = project.testing.testProcessor;
        testProcessor.setJvmHints(jdks(), project.getJvmTargetVersion());
        if (tests.fork != null && tests.fork && testProcessor.getForkingProcess() == null) {
            final JkJavaProcess javaProcess = JkJavaProcess.ofJava(JkTestProcessor.class.getName())
                    .addJavaOptions(this.tests.jvmOptions);
            if (project.getJvmTargetVersion() != null &&
                    !JkJavaVersion.ofCurrent().equals(project.getJvmTargetVersion())) {
                Path javaHome = jdks().getHome(project.getJvmTargetVersion());
                if (javaHome != null) {
                    JkLog.trace("Tests are configured to run using JDK %s", javaHome);
                    javaProcess.setCommand(javaHome.resolve("bin/java").toString());
                }
            }
            testProcessor.setForkingProcess(javaProcess);
        } else if (tests.fork != null && !tests.fork) {
            testProcessor.setForkingProcess(false);
        }
        if (tests.skip != null) {
            project.testing.setSkipped(tests.skip);
        }
        if (compilation.compilerExtraArgs != null) {
            project.compilation.addJavaCompilerOptions(
                    JkUtilsString.translateCommandline(compilation.compilerExtraArgs));
        }
    }

    private void configureScaffold() {
        getRuntime().find(ScaffoldKBean.class).ifPresent(scaffoldKBean -> {

            // Scaffold project structure including build class
            JkScaffoldOptions scaffoldOptions = this.scaffold;
            JkProjectScaffold projectScaffold = JkProjectScaffold.of(project, scaffoldKBean.scaffold);
            projectScaffold.configureScaffold(scaffoldOptions.template);

            // Create 'project-dependencies.txt' file if needed
            JkScaffoldOptions.DependenciesTxt dependenciesTxt = scaffoldOptions.dependenciesTxt;
            List<String> compileDeps = JkScaffoldOptions.DependenciesTxt.toList(dependenciesTxt.compile);
            List<String> runtimeDeps = JkScaffoldOptions.DependenciesTxt.toList(dependenciesTxt.runtime);
            List<String> testDeps = JkScaffoldOptions.DependenciesTxt.toList(dependenciesTxt.test);
            projectScaffold.createProjectDependenciesTxt(compileDeps, runtimeDeps, testDeps);

            // Create local lib folder structure if needed
            if (scaffoldOptions.generateLocalLibsFolders) {
                projectScaffold.generateLocalLibsFolders();
            }
        });
    }

}
