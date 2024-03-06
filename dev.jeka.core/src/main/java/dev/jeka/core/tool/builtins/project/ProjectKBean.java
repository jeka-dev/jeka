package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.project.*;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.scaffold.JkScaffoldOptions;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@JkDoc("Manages the build and execution of a JVM project hosted in the base directory")
public final class ProjectKBean extends KBean implements JkIdeSupportSupplier {

    // The underlying project managed by this KBean
    @JkDoc(hide = true)
    public final JkProject project = JkProject.of();

    @JkDoc("Version of the project. Can be used by a CI/CD tool to inject version.")
    public String version;

    /**
     * Options for the packaging tasks (jar creation). These options are injectable from command line.
     */
    @JkDoc
    private final JkPackOptions pack = new JkPackOptions();

    /**
     * Options for run tasks
     */
    public final JkRunOptions run = new JkRunOptions();

    @JkDoc
    private final JkDependenciesOptions dependencies = new JkDependenciesOptions();

    /**
     * Options for configuring testing tasks.
     */
    @JkDoc
    private final JkTestOptions tests = new JkTestOptions();

    /**
     * Options for configuring scaffold.
     */
    public final JkProjectScaffoldOptions scaffold = new JkProjectScaffoldOptions();

    /**
     * Options for configuring directory layout.
     */
    @JkDoc
    public final JkLayoutOptions layout = new JkLayoutOptions();

    /**
     * Options for configuring compilation.
     */
    @JkDoc
    @JkInjectProperty // some nested fields are prop-injectable
    private final JkCompilationOptions compilation = new JkCompilationOptions();

    @JkDoc("The output file for the xml dependency description.")
    public Path outputFile;

    private JkProjectScaffold projectScaffold;

    @Override
    protected void init() {
        configureProject();
    }

    // ------------------------------- command line methods -----------------------------

    @JkDoc("Delete the content of jeka-output directory and might execute extra clean actions")
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

    @JkDoc("Displays resolved dependency trees on console.")
    public void depTree() {
        project.displayDependencyTree();
    }

    @JkDoc("Displays resolved dependency trees as xml, on console.")
    public void depTreeAsXml() {
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
        JkLog.info("\nExecute 'project: showDepTree' to display details on dependencies.");
    }

    @JkDoc("Run the generated jar.")
    public void runJar() {
        this.run.runJar();
    }

    @JkDoc("Run the compiled classes.")
    public void runMain() {
        this.run.runMain();
    }
    
    @JkDoc("Scaffold a JeKa project skeleton in working directory.")
    public void scaffold() {
        projectScaffold.run();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project.getJavaIdeSupport();
    }

    /**
     * Returns the project scaffold used by this KBean to perform #scaffold#
     */
    public JkProjectScaffold getProjectScaffold() {
        return projectScaffold;
    }

    // ------- static classes for configuration

    /**
     * Standard options for packaging java projects.
     */
    public static class JkPackOptions {

        @JkDoc("Type of jar to produce for the main artifact.")
        public JkProjectPackaging.JarType jarType = JkProjectPackaging.JarType.REGULAR;

        @JkDoc("Main class name to include in Manifest. Use 'auto' to automatic discovering.")
        public String mainClass;

    }

    /**
     * Options about tests
     */
    public static final class JkTestOptions {

        /** Turn it on to skip tests. */
        @JkDoc("If true, tests are not run.")
        @JkInjectProperty("jeka.skip.tests")
        public Boolean skip;

        /** Turn it on to run tests in a withForking process. */
        @JkDoc("If true, tests will be executed in a forked process.")
        public Boolean fork;

        /** Argument passed to the JVM if tests are withForking. Example : -Xms2G -Xmx2G */
        @JkDoc("Argument passed to the JVM if tests are executed in a forked process (example -Xms2G -Xmx2G).")
        public String jvmOptions;

        @JkDoc("The style to use to show test execution progress.")
        public JkTestProcessor.JkProgressOutputStyle progressStyle;

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
        public String compilerOptions;

    }

    public class JkRunOptions {

        @JkDoc("JVM options to use when running generated jar")
        public String jvmOptions = "";

        @JkDoc("Program arguments to use when running generated jar")
        public String programArgs = "";

        @JkDoc("If true, the resolved runbase classpath will be used when running the generated jar. " +
                "If the generated jar is a Uber jar or contains all the needed dependencies, leave it to 'false'")
        public boolean useRuntimeDepsForClasspath;

        void runJar() {
            project.prepareRunJar(useRuntimeDepsForClasspath)
                    .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                    .addParams(JkUtilsString.parseCommandline(programArgs))
                    .exec();
        }

        void runMain() {
            project.prepareRunMain()
                    .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                    .addParams(JkUtilsString.parseCommandline(programArgs))
                    .exec();
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

    public class JkProjectScaffoldOptions extends JkScaffoldOptions {

        @JkDoc("Generate libs sub-folders for hosting local libraries")
        private boolean generateLibsFolders = false;

        @JkDoc("The template used for scaffolding the build class")
        private JkProjectScaffold.Template template = JkProjectScaffold.Template.BUILD_CLASS;

        public JkProjectScaffold.Template getTemplate() {
            return template;
        }

        @Override
        public void applyTo(JkScaffold scaffold) {
            super.applyTo(scaffold);
            // Scaffold project structure including build class
            JkProjectScaffold projectScaffold = (JkProjectScaffold) scaffold;
            projectScaffold.setTemplate(template);
            projectScaffold.setUseSimpleStyle(ProjectKBean.this.layout.style == JkCompileLayout.Style.SIMPLE);

            // Create 'project-dependencies.txt' file if needed
            List<String> compileDeps = dependencies.toList(dependencies.compile);
            List<String> runtimeDeps = dependencies.toList(dependencies.runtime);
            List<String> testDeps = dependencies.toList(dependencies.test);
            projectScaffold.compileDeps.addAll(compileDeps);
            projectScaffold.runtimeDeps.addAll(runtimeDeps);
            projectScaffold.testDeps.addAll(testDeps);

            // Create local lib folder structure if needed
            if (generateLibsFolders) {
                projectScaffold.setGenerateLibsFolders(true);
            }
        }
    }

    @Override
    public String toString() {
        return project.toString();
    }

    // ------- private methods

    private JkJavaCompilerToolChain.JkJdks jdks() {
        return JkJavaCompilerToolChain.JkJdks.ofJdkHomeProps(getRunbase().getProperties().getAllStartingWith("jeka.jdk.", false));
    }

    private void applyRepoConfigOn(JkProject project) {

        // set dependency resolver
        JkRepoProperties repoProperties = JkRepoProperties.of(this.getRunbase().getProperties());
        final JkRepoSet downloadRepos = repoProperties.getDownloadRepos();
        if (!downloadRepos.getRepos().isEmpty()) {
            project.dependencyResolver.setRepos(downloadRepos);
        }
    }

    private void configureProject() {
        project.setBaseDir(getBaseDir());
        if (!JkLog.isAnimationAccepted()) {
            project.testing.testProcessor.engineBehavior.setProgressDisplayer(
                    JkTestProcessor.JkProgressOutputStyle.SILENT);
        }
        if (!JkUtilsString.isBlank(version)) {
            project.setVersion(version);
        }
        project.dependencyResolver.setFileSystemCacheDir(getBaseDir().resolve(JkConstants.JEKA_WORK_PATH)
                .resolve("project-dep-resolution-cache"));
        project.dependencyResolver.setUseFileSystemCache(true);
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
        if (pack.jarType != null) {
            project.flatFacade().setMainArtifactJarType(pack.jarType);
        }
        if (pack.mainClass != null) {
            project.packaging.setMainClass(pack.mainClass);
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
                    JkLog.verbose("Tests are configured to run using JDK %s", javaHome);
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
        // The style should not be forced by default as it is determined by the presence of a console,
        // and the log level
        if (tests.progressStyle != null) {
            project.testing.testProcessor.engineBehavior.setProgressDisplayer(tests.progressStyle);
        }
        if (compilation.compilerOptions != null) {
            String[] options = JkUtilsString.parseCommandline(compilation.compilerOptions);
            project.compilation.addJavaCompilerOptions(options);
            project.testing.compilation.addJavaCompilerOptions(options);
        }

        // Configure scaffold
        this.projectScaffold = JkProjectScaffold.of(project);
        this.scaffold.applyTo(projectScaffold); // apply basic configuration from KBean fields
    }

    private static class JkDependenciesOptions {

        @JkDoc("Comma separated compile dependencies to include at scaffold time")
        public String compile;

        @JkDoc("Comma separated runtime dependencies to include at scaffold time")
        public String runtime;

        @JkDoc("Comma separated test dependencies to include at scaffold time")
        public String test;

        List<String> toList(String description) {
            if (description == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(description.split(","));
        }
    }

}
