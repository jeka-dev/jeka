package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.JkJdks;
import dev.jeka.core.api.project.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.scaffold.JkScaffold;
import dev.jeka.core.tool.builtins.scaffold.ScaffoldKBean;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plugin for building JVM language based projects. It comes with a {@link JkProject} pre-configured with {@link JkProperties}.
 * and a decoration for scaffolding.
 */
@JkDoc("Provides a configured JkProject instance for building JVM based projects.")
public class ProjectKBean extends KBean implements JkIdeSupportSupplier {

    /**
     * Options for the packaging tasks (jar creation). These options are injectable from command line.
     */
    public final JkPackOptions pack = new JkPackOptions();

    /**
     * Options for run tasks
     */
    public final JkRunOptions run = new JkRunOptions();

    /**
     * Options for the testing tasks. These options are injectable from command line.
     */
    public final JkTestOptions test = new JkTestOptions();

    public final JkScaffoldOptions scaffold = new JkScaffoldOptions();

    public final JkLayoutOptions layout = new JkLayoutOptions();

    public final JkCompilationOptions compilation = new JkCompilationOptions();

    @JkDoc("The output file for the xml dependency description.")
    public Path outputFile;

    public final JkProject project = JkProject.of();

    @Override
    protected void init() {
        configureProject();

        // configure ScaffoldKBean
        getRuntime().getOptionalKBean(ScaffoldKBean.class).ifPresent(scaffoldKBean ->
                configureScaffold(scaffoldKBean.scaffold));
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
        project.artifactProducer.makeAllMissingArtifacts();
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
        final Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();  //NOSONAR
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        Writer out;
        if (outputFile == null) {
            out = new PrintWriter(JkLog.getOutPrintStream());
        } else {
            try {
                JkPathFile.of(outputFile).createIfNotExist();
                out = new FileWriter(outputFile.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        Document document = project.getDependenciesAsXml();
        try {
            transformer.transform(new DOMSource(document), new StreamResult(out));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
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
        project.publication.publish();
    }

    @JkDoc("Publishes produced artifacts to local repository.")
    public void publishLocal() {
        project.publication.publishLocal();
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

        /** When true, javadoc is created and packed in a jar file.*/
        @JkDoc("If true, javadoc jar is added in the list of artifact to produce/publish.")
        public Boolean javadoc;

        /** When true, sources are packed in a jar file.*/
        @JkDoc("If true, sources jar is added in the list of artifact to produce/publish.")
        public Boolean sources;

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
            Path jarPath = ProjectKBean.this.project.artifactProducer.getMainArtifactPath();
            if (!Files.exists(jarPath)) {
                ProjectKBean.this.project.artifactProducer.makeMainArtifact();
            }
            JkJavaProcess javaProcess = JkJavaProcess.ofJavaJar(jarPath, null)
                    .setLogCommand(JkLog.isVerbose())
                    .setDestroyAtJvmShutdown(true)
                    .addJavaOptions(JkUtilsString.translateCommandline(jvmOptions))
                    .addParams(JkUtilsString.translateCommandline(programArgs));
            if (useRuntimeDepsForClasspath) {
                javaProcess
                        .setClasspath(ProjectKBean.this.project.packaging.resolveRuntimeDependencies().getFiles());
            }
            javaProcess.exec();
        }
    }

    public static class JkScaffoldOptions {

        @JkDoc("Generate jeka/project-libs sub-folders for hosting local libraries")
        public boolean generateLocalLibsFolders = false;

        @JkDoc("The template used for scaffolding the build class")
        public JkProjectBuildClassTemplate template = JkProjectBuildClassTemplate.SIMPLE_FACADE;

        public final DependenciesTxt dependenciesTxt = new DependenciesTxt();

        private void scaffoldProjectStructure(JkProject configuredProject) {
            JkLog.info("Create source directories.");
            JkCompileLayout prodLayout = configuredProject.compilation.layout;
            prodLayout.resolveSources().toList().forEach(JkPathTree::createIfNotExist);
            prodLayout.resolveResources().toList().forEach(JkPathTree::createIfNotExist);
            JkCompileLayout testLayout = configuredProject.testing.compilation.layout;
            testLayout.resolveSources().toList().forEach(JkPathTree::createIfNotExist);
            testLayout.resolveResources().toList().forEach(JkPathTree::createIfNotExist);

            // Create specific files and folders
            String dependenciesTxtContent = dependenciesTxtContent();
            JkPathFile.of(configuredProject.getBaseDir().resolve(JkConstants.JEKA_DIR)
                            .resolve(JkConstants.PROJECT_DEPENDENCIES_TXT_FILE))
                    .createIfNotExist()
                    .write(dependenciesTxtContent.getBytes(StandardCharsets.UTF_8));
            Path libs = configuredProject.getBaseDir().resolve(JkConstants.JEKA_DIR)
                    .resolve(JkConstants.PROJECT_LIBS_DIR);
            if (generateLocalLibsFolders) {
                JkPathFile.of(libs.resolve("readme.txt"))
                        .fetchContentFrom(ProjectKBean.class.getResource("libs-readme.txt"));
                JkUtilsPath.createDirectories(libs.resolve("regular"));
                JkUtilsPath.createDirectories(libs.resolve("compile-only"));
                JkUtilsPath.createDirectories(libs.resolve("runtime-only"));
                JkUtilsPath.createDirectories(libs.resolve("test"));
                JkUtilsPath.createDirectories(libs.resolve("sources"));
            }

            // This is special scaffolding for project pretending to be plugins for Jeka
            if (this.template == JkProjectBuildClassTemplate.PLUGIN) {
                Path breakingChangeFile = configuredProject.getBaseDir().resolve("breaking_versions.txt");
                String text = "## Next line means plugin 2.4.0.RC11 is not compatible with Jeka 0.9.0.RELEASE and above\n" +
                        "## 2.4.0.RC11 : 0.9.0.RELEASE   (remove this comment and leading '##' to be effective)";
                JkPathFile.of(breakingChangeFile).write(text);
                Path sourceDir =
                        configuredProject.compilation.layout.getSources().toList().get(0).getRoot();
                String pluginCode = JkUtilsIO.read(ProjectKBean.class.getResource("pluginclass.snippet"));
                JkPathFile.of(sourceDir.resolve("your/basepackage/XxxxxJkBean.java"))
                        .createIfNotExist()
                        .write(pluginCode.getBytes(StandardCharsets.UTF_8));
            }
        }

        private String dependenciesTxtContent() {
            List<String> lines = JkUtilsIO.readAsLines(ProjectKBean.class.getResourceAsStream("dependencies.txt"));
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line).append("\n");
                if (line.startsWith("== COMPILE") && !JkUtilsString.isBlank(this.dependenciesTxt.compile)) {
                    Arrays.stream(this.dependenciesTxt.compile.split(",")).forEach(extraDep ->
                            sb.append(extraDep.trim()).append("\n")
                    );
                }
                if (line.startsWith("== RUNTIME") && !JkUtilsString.isBlank(this.dependenciesTxt.runtime)) {
                    Arrays.stream(this.dependenciesTxt.runtime.split(",")).forEach(extraDep ->
                            sb.append(extraDep.trim()).append("\n")
                    );
                }
                if (line.startsWith("== TEST") && !JkUtilsString.isBlank(this.dependenciesTxt.test)) {
                    Arrays.stream(this.dependenciesTxt.test.split(",")).forEach(extraDep ->
                            sb.append(extraDep.trim()).append("\n")
                    );
                }
            }
            return sb.toString();
        }

        public static class DependenciesTxt {

            @JkDoc("Comma separated dependencies to include in project-dependencies.txt COMPILE section")
            public String compile;

            @JkDoc("Comma separated dependencies to include in project-dependencies.txt RUNTIME section")
            public String runtime;

            @JkDoc("Comma separated dependencies to include in project-dependencies.txt TEST section")
            public String test;

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
        project.publication.maven.setPublishRepos(mavenPublishRepos);
        JkRepoSet ivyPulishRepos = repoProperties.getPublishRepository();
        if (ivyPulishRepos.getRepos().isEmpty()) {
            ivyPulishRepos = ivyPulishRepos.and(JkRepo.ofLocal());
        }
        project.publication.ivy.setRepos(ivyPulishRepos);

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
        if (layout.style != null) {
            project.flatFacade().setLayoutStyle(layout.style);
        }
        if (layout.mixSourcesAndResources) {
            project.flatFacade().mixResourcesAndSources();
        }
        JkJavaCompiler compiler = project.compiler;
        if (!compiler.isToolOrProcessSpecified()) {
            compiler.setGuessHints(jdks(), project.getJvmTargetVersion(), true);
        }
        final JkStandardFileArtifactProducer artifactProducer = project.artifactProducer;
        JkArtifactId sources = JkProject.SOURCES_ARTIFACT_ID;
        if (pack.sources != null && !pack.sources) {
            artifactProducer.removeArtifact(sources);
        } else if (pack.sources != null && pack.sources && !artifactProducer.getArtifactIds().contains(sources)) {
            Consumer<Path> sourceJar = project.packaging::createSourceJar;
            artifactProducer.putArtifact(sources, sourceJar);
        }
        JkArtifactId javadoc = JkProject.JAVADOC_ARTIFACT_ID;
        if (pack.javadoc != null && !pack.javadoc) {
            artifactProducer.removeArtifact(javadoc);
        } else if (pack.javadoc != null && pack.javadoc && !artifactProducer.getArtifactIds().contains(javadoc)) {
            Consumer<Path> javadocJar = project.packaging::createJavadocJar;
            artifactProducer.putArtifact(javadoc, javadocJar);
        }
        if (pack.jarType != null) {
            project.flatFacade().setMainArtifactJarType(pack.jarType);
        }
        JkTestProcessor testProcessor = project.testing.testProcessor;
        testProcessor.setJvmHints(jdks(), project.getJvmTargetVersion());
        if (test.fork != null && test.fork && testProcessor.getForkingProcess() == null) {
            final JkJavaProcess javaProcess = JkJavaProcess.ofJava(JkTestProcessor.class.getName())
                    .addJavaOptions(this.test.jvmOptions);
            if (project.getJvmTargetVersion() != null &&
                    !JkJavaVersion.ofCurrent().equals(project.getJvmTargetVersion())) {
                Path javaHome = jdks().getHome(project.getJvmTargetVersion());
                if (javaHome != null) {
                    JkLog.trace("Tests are configured to run using JDK %s", javaHome);
                    javaProcess.setCommand(javaHome.resolve("bin/java").toString());
                }
            }
            testProcessor.setForkingProcess(javaProcess);
        } else if (test.fork != null && !test.fork) {
            testProcessor.setForkingProcess(false);
        }
        if (test.skip != null) {
            project.testing.setSkipped(test.skip);
        }
        if (compilation.compilerExtraArgs != null) {
            project.compilation.addJavaCompilerOptions(
                    JkUtilsString.translateCommandline(compilation.compilerExtraArgs));
        }
    }

    private void configureScaffold(JkScaffold scaffold) {
        scaffold.setJekaClassCodeProvider( () -> {
            final String snippet;
            if (this.scaffold.template == JkProjectBuildClassTemplate.CODE_LESS) {
                return null;
            }
            if (this.scaffold.template == JkProjectBuildClassTemplate.NORMAL) {
                snippet = "buildclass.snippet";
            } else if (this.scaffold.template == JkProjectBuildClassTemplate.PLUGIN) {
                snippet = "buildclassplugin.snippet";
            } else if (this.scaffold.template == JkProjectBuildClassTemplate.PURE_API) {
                snippet = "buildclasspureapi.snippet";
            } else {
                snippet = "buildclassfacade.snippet";
            }
            String template = JkUtilsIO.read(ProjectKBean.class.getResource(snippet));
            String baseDirName = getBaseDir().getFileName().toString();
            return template.replace("${group}", baseDirName).replace("${name}", baseDirName);
        });
        scaffold.setClassFilename("Build.java");
        scaffold.extraActions.append( () -> this.scaffold.scaffoldProjectStructure(project));
    }

}
