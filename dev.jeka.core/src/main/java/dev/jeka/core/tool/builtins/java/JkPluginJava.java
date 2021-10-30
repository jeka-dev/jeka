package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkProjectDependencies;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.project.*;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.repos.JkPluginGpg;
import dev.jeka.core.tool.builtins.repos.JkPluginRepo;
import dev.jeka.core.tool.builtins.scaffold.JkPluginScaffold;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Plugin for building Java projects. It comes with a {@link JkJavaProject} pre-configured with {@link JkOptions}.
 * and a decoration for scaffolding.
 */
@JkDoc("Build of a Java project through a JkJavaProject instance.")
@JkDocPluginDeps({JkPluginRepo.class, JkPluginScaffold.class})
public class JkPluginJava extends JkPlugin implements JkJavaIdeSupport.JkSupplier {

    /**
     * Options for the packaging tasks (jar creation). These options are injectable from command line.
     */
    public final JkJavaPackOptions pack = new JkJavaPackOptions();

    /**
     * Options for the testing tasks. These options are injectable from command line.
     */
    public final JkTestOptions test = new JkTestOptions();

    @JkDoc("Extra arguments to be passed to the compiler (e.g. -Xlint:unchecked).")
    public String compilerExtraArgs;

    @JkDoc("The template used for scaffolding the build class")
    public ScaffoldTemplate scaffoldTemplate = ScaffoldTemplate.SIMPLE_FACADE;

    @JkDoc("The output file for the xml dependency description.")
    public Path output;

    // ----------------------------------------------------------------------------------

    private final JkPluginRepo repoPlugin;

    private final JkPluginScaffold scaffoldPlugin;

    private JkJavaProject project;

    protected JkPluginJava(JkClass jkClass) {
        super(jkClass);
        this.scaffoldPlugin = jkClass.getPlugins().get(JkPluginScaffold.class);
        this.repoPlugin = jkClass.getPlugins().get(JkPluginRepo.class);
    }

    @Override
    protected void beforeSetup() {
        Path baseDir = getJkClass().getBaseDir();
        this.project = JkJavaProject.of().setBaseDir(this.getJkClass().getBaseDir());
        project.getConstruction().addTextAndLocalDependencies();

        JkJavaCompiler compiler = project.getConstruction().getCompiler();
        compiler.setJdkHomesWithProperties(JkOptions.getAllStartingWith("jdk."));
        project.getPublication().getMaven().setRepos(repoPlugin.publishRepository().toSet());
        project.getPublication().getIvy().setRepos(repoPlugin.publishRepository().toSet());
        final JkRepo downloadRepo = repoPlugin.downloadRepository();
        JkDependencyResolver resolver = project.getConstruction().getDependencyResolver();
        if (!resolver.getRepos().contains(downloadRepo.getUrl())) {
            resolver.addRepos(downloadRepo);
        }
        applyGpg(project);
    }

    public void applyGpg(JkJavaProject project) {
        JkPluginGpg pgpPlugin = this.getJkClass().getPlugins().get(JkPluginGpg.class);
        JkGpg gpg = pgpPlugin.get();
        applyGpg(gpg, pgpPlugin.keyName, project);
    }

    public static void applyGpg(JkGpg gpg, String keyName, JkJavaProject project) {
        UnaryOperator<Path> signer  = gpg.getSigner(keyName);
        project.getPublication().getMaven().setDefaultSigner(signer);
        project.getPublication().getIvy().setDefaultSigner(signer);
    }


    @JkDoc("Improves scaffolding by creating a project structure ready to build.")
    @Override  
    protected void afterSetup() {
        this.applyPostSetupOptions();
        this.setupScaffolder();
    }

    private void applyPostSetupOptions() {
        final JkStandardFileArtifactProducer artifactProducer = project.getPublication().getArtifactProducer();
        JkArtifactId sources = JkJavaProjectPublication.SOURCES_ARTIFACT_ID;
        if (pack.sources != null && !pack.sources) {
            artifactProducer.removeArtifact(sources);
        } else if (pack.sources != null && pack.sources && !artifactProducer.getArtifactIds().contains(sources)) {
            Consumer<Path> sourceJar = project.getDocumentation()::createSourceJar;
            artifactProducer.putArtifact(sources, sourceJar);
        }
        JkArtifactId javadoc = JkJavaProjectPublication.JAVADOC_ARTIFACT_ID;
        if (pack.javadoc != null && !pack.javadoc) {
            artifactProducer.removeArtifact(javadoc);
        } else if (pack.javadoc != null && pack.javadoc && !artifactProducer.getArtifactIds().contains(javadoc)) {
            Consumer<Path> javadocJar = project.getDocumentation()::createJavadocJar;
            artifactProducer.putArtifact(javadoc, javadocJar);
        }
        JkTestProcessor testProcessor = project.getConstruction().getTesting().getTestProcessor();
        if (test.fork != null && test.fork && testProcessor.getForkingProcess() == null) {
            final JkJavaProcess javaProcess = JkJavaProcess.ofJava(JkTestProcessor.class.getName())
                    .addJavaOptions(this.test.jvmOptions);
            testProcessor.setForkingProcess(javaProcess);
        } else if (test.fork != null && !test.fork && testProcessor.getForkingProcess() != null) {
            testProcessor.setForkingProcess(false);
        }
        if (test.skip != null) {
            project.getConstruction().getTesting().setSkipped(test.skip);
        }
        if (this.compilerExtraArgs != null) {
            project.getConstruction().getCompilation().addJavaCompilerOptions(JkUtilsString.translateCommandline(this.compilerExtraArgs));
        }
    }

    private void setupScaffolder() {
        scaffoldPlugin.getScaffolder().setJekaClassCodeProvider( () -> {
            final String snippet;
            if (scaffoldTemplate == ScaffoldTemplate.NORMAL) {
                snippet = "buildclass.snippet";
            } else if (scaffoldTemplate == ScaffoldTemplate.PLUGIN) {
                snippet = "buildclassplugin.snippet";
            } else {
                snippet = "buildclassfacade.snippet";
            }
            String template = JkUtilsIO.read(JkPluginJava.class.getResource(snippet));
            String baseDirName = getJkClass().getBaseDir().getFileName().toString();
            return template.replace("${group}", baseDirName).replace("${name}", baseDirName);
        });
        scaffoldPlugin.getScaffolder().setClassFilename("Build.java");
        scaffoldPlugin.getScaffolder().getExtraActions().append( () -> {
            JkLog.info("Create source directories.");
            JkCompileLayout prodLayout = project.getConstruction().getCompilation().getLayout();
            prodLayout.resolveSources().toList().stream().forEach(tree -> tree.createIfNotExist());
            prodLayout.resolveResources().toList().stream().forEach(tree -> tree.createIfNotExist());
            JkCompileLayout testLayout = project.getConstruction().getTesting().getCompilation().getLayout();
            testLayout.resolveSources().toList().stream().forEach(tree -> tree.createIfNotExist());
            testLayout.resolveResources().toList().stream().forEach(tree -> tree.createIfNotExist());
            if (this.scaffoldTemplate == ScaffoldTemplate.PLUGIN) {
                Path breakinkChangeFile = this.getProject().getBaseDir().resolve("breaking_versions.txt");
                String text = "## Next line means plugin 2.4.0.RC11 is not compatible with Jeka 0.9.0.RELEASE and above\n" +
                        "## 2.4.0.RC11 : 0.9.0.RELEASE   (remove this comment and leading '##' to be effective)";
                JkPathFile.of(breakinkChangeFile).createIfNotExist().write(text.getBytes(StandardCharsets.UTF_8));
                Path sourceDir =
                        project.getConstruction().getCompilation().getLayout().getSources().toList().get(0).getRoot();
                String pluginCode = JkUtilsIO.read(JkPluginJava.class.getResource("pluginclass.snippet"));
                JkPathFile.of(sourceDir.resolve("your/basepackage/JkPluginXxxxxxx.java"))
                        .createIfNotExist()
                        .write(pluginCode.getBytes(StandardCharsets.UTF_8));
            }
        });

    }

    // ------------------------------ Accessors -----------------------------------------

    public JkJavaProject getProject() {
        return project;
    }

    public void setProject(JkJavaProject javaProject) {
        this.project = javaProject;
    }

    public JkPluginRepo getRepoPlugin() {
        return repoPlugin;
    }

    public  JkPluginScaffold getScaffoldPlugin() {
        return scaffoldPlugin;
    }

    // ------------------------------- command line methods -----------------------------

    @JkDoc("Perform declared pre compilation task as generating sources.")
    public void preCompile() {
        project.getConstruction().getCompilation().getPreCompileActions().run();
    }

    @JkDoc("Performs compilation and resource processing.")
    public void compile() {
        project.getConstruction().getCompilation().run();
    }

    @JkDoc("Compiles and run tests defined within the project (typically Junit tests).")
    public void test() {
        project.getConstruction().getTesting().run();
    }

    @JkDoc("Generates from scratch artifacts defined through 'pack' options (Perform compilation and testing if needed).  " +
            "\nDoes not re-generate artifacts already generated : " +
            "execute 'clean java#pack' to re-generate artifacts.")
    public void pack() {
        project.getPublication().getArtifactProducer().makeAllMissingArtifacts();
    }

    /**
     * Displays the resolved dependency tree on the console.
     */
    @JkDoc("Displays resolved dependency tree on console.")
    public final void showDependencies() {
        JkJavaProjectConstruction construction = project.getConstruction();
        showDependencies("compile", construction.getCompilation().getDependencies());
        showDependencies("runtime", construction.getRuntimeDependencies());
        showDependencies("test", construction.getTesting().getCompilation().getDependencies());
    }

    private void showDependencies(String purpose, JkDependencySet deps) {
        JkLog.info("\nDependencies for " + purpose + " : ");
        final JkResolveResult resolveResult = this.getProject().getConstruction().getDependencyResolver().resolve(deps);
        final JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        JkLog.info("------------------------------");
        JkLog.info(String.join("\n", tree.toStrings()));
        JkLog.info("");
    }

    @JkDoc("Displays resolved dependency tree in xml")
    public void showDependenciesXml() {
        Transformer transformer = null;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        Writer out;
        if (output == null) {
            out = new PrintWriter(JkLog.getOutPrintStream());
        } else {
            try {
                JkPathFile.of(output).createIfNotExist();
                out = new FileWriter(output.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        Document document = project.getConstruction().getDependenciesAsXml();
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

    @JkDoc("Publishes produced artifacts to configured repository.")
    public void publish() {
        project.getPublication().publish();
    }

    @JkDoc("Publishes produced artifacts to local repository.")
    public void publishLocal() {
        project.getPublication().getMaven().publishLocal();
    }


    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return project.getJavaIdeSupport();
    }


    /**
     * Standard options for packaging java projects.
     */
    public static class JkJavaPackOptions {

        /** When true, javadoc is created and packed in a jar file.*/
        @JkDoc("If true, javadoc jar is added in the list of artifact to produce/publish.")
        public Boolean javadoc;

        /** When true, sources are packed in a jar file.*/
        @JkDoc("If true, sources jar is added in the list of artifact to produce/publish.")
        public Boolean sources;

    }

    /**
     * Options about tests
     */
    public static final class JkTestOptions {

        /** Turn it on to skip tests. */
        @JkDoc("If true, tests are not run.")
        public Boolean skip;

        /** Turn it on to run tests in a withForking process. */
        @JkDoc("If true, tests will be executed in a withForking process.")
        public Boolean fork;

        /** Argument passed to the JVM if tests are withForking. Example : -Xms2G -Xmx2G */
        @JkDoc("Argument passed to the JVM if tests are withForking. E.g. -Xms2G -Xmx2G.")
        public String jvmOptions;

    }

    public enum ScaffoldTemplate {

        NORMAL, SIMPLE_FACADE, PLUGIN

    }
}
