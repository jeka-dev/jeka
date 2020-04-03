package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkFileSystemLocalizable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Container for a Java project with classic characteristic :
 * <ul>
 *     <li>Contains Java source files to be compiled</li>
 *     <li>All Java sources file (prod + test) are wrote against the same Java version and encoding</li>
 *     <li>JkJavaProject may contain unit tests</li>
 *     <li>It can depends on any accepted dependencies (Maven module, other project, files on fs, ...)</li>
 *     <li>It produces a bin jar, a source jar and a javadoc jar</li>
 *     <li>It can produce any other artifact files (fat-jar, test jar, doc, ...)</li>
 *     <li>It can be identified as a Maven module (means it can provide a group, artifact id, version) in order to be published/consumed</li>
 *     <li>It can be published on any Maven/Ivy repository, including Maven central</li>
 *     <li>Part of the sources/resources may be generated</li>
 *     <li>By default, passing test suite is required to produce bin artifacts.</li>
 * </ul>
 *
 * It provides cache mechanism in order compile or unit test phases are executed once when generating
 * several artifact files so be aware of clean it if you want to replay some tasks with different settings.
 *
 */
public class JkJavaProject implements JkJavaIdeSupportSupplier, JkFileSystemLocalizable, Supplier<JkArtifactProducer> {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    private Path baseDir = Paths.get(".");

    private String outputDir = "jeka/output";

    private final JkJavaProject.JkSteps steps;

    private final JkDependencyManagement<JkJavaProject> dependencyManagement;

    private final JkArtifactBasicProducer<JkJavaProject> artifactProducer;

    private JkJavaProject() {
        dependencyManagement = JkDependencyManagement.ofParent(this);
        steps = new JkSteps(this);
        artifactProducer = JkArtifactBasicProducer.ofParent(this)
                .setArtifactFileFunction(this::getOutputDir, this::artifactFileNamePart);
        registerArtifacts();
    }

    public static JkJavaProject of() {
        return new JkJavaProject();
    }

    public JkJavaProject apply(Consumer<JkJavaProject> projectConsumer) {
        projectConsumer.accept(this);
        return this;
    }


    // ---------------------------- Getters / setters --------------------------------------------

    @Override
    public Path getBaseDir() {
        return this.baseDir;
    }

    public JkJavaProject setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    /**
     * Returns path of the directory under which are produced build files
     */
    public Path getOutputDir() {
        return baseDir.resolve(outputDir);
    }

    /**
     * Sets the output path dir relative to base dir.
     */
    public JkJavaProject setOutputDir(String relativePath) {
        this.outputDir = relativePath;
        return this;
    }

    public JkDependencyManagement<JkJavaProject> getDependencyManagement() {
        return dependencyManagement;
    }

    public JkArtifactBasicProducer<JkJavaProject> getArtifactProducer() {
        return artifactProducer;
    }

    public JkSteps getSteps() {
        return steps;
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + getBaseDir().getFileName();
    }

    @Override
    public JkArtifactProducer get() {
        return artifactProducer;
    }

    public String getInfo() {
        JkJavaProjectCompilationStep compilation = getSteps().compilation;
        return new StringBuilder("Project Location : " + this.getBaseDir() + "\n")
                .append("Published Module & version : " + steps.getPublishing().getVersionedModule() + "\n")
                .append("Production sources : " + steps.compilation.getLayout().getInfo()).append("\n")
                .append("Test sources : " + steps.testing.getTestCompilation().getLayout().getInfo()).append("\n")
                .append("Java Source Version : " + steps.getCompilation().getComputedCompileSpec().getSourceVersion() + "\n")
                .append("Source Encoding : " + steps.getCompilation().getComputedCompileSpec().getEncoding() + "\n")
                .append("Source file count : " + compilation.getLayout().getSources().count(Integer.MAX_VALUE, false) + "\n")
                .append("Download Repositories : " + dependencyManagement.getResolver().getRepos() + "\n")
                .append("Publish repositories : " + steps.getPublishing().getPublishRepos()  + "\n")
                .append("Declared Dependencies : " + dependencyManagement.getDependencies().toList().size() + " elements.\n")
                .append("Defined Artifacts : " + get().getArtifactIds())
                .toString();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return JkJavaIdeSupport.of(baseDir)
                .setSourceVersion(steps.compilation.getJavaVersion())
                .setProdLayout(steps.compilation.getLayout())
                .setTestLayout(steps.testing.getTestCompilation().getLayout())
                .setDependencies(this.dependencyManagement.getDependencies())
                .setDependencyResolver(this.dependencyManagement.getResolver());
    }

    private String artifactFileNamePart() {
        JkVersionedModule versionedModule = steps.getPublishing().getVersionedModule();
        if (versionedModule != null) {
            return versionedModule.toString();
        }
        return baseDir.getFileName().toString();
    }

    private void registerArtifacts() {
        artifactProducer.putMainArtifact(steps.getPackaging()::createBinJar,
                () -> dependencyManagement.fetchDependencies(JkJavaDepScopes.RUNTIME).getFiles());
        artifactProducer.putArtifact(SOURCES_ARTIFACT_ID, steps.getPackaging()::createSourceJar);
        artifactProducer.putArtifact(JAVADOC_ARTIFACT_ID, steps.getPackaging()::createJavadocJar);
    }

    public static class JkSteps {

        public final JkJavaProject __;

        private final JkJavaProjectCompilationStep compilation;

        private final JkJavaProjectTestingStep testing;

        private final JkJavaProjectPackagingStep packaging;

        private final JkJavaProjecPublishingStep publishing;

        private final JkJavaProjectDocumentationStep documentation;

        private JkSteps(JkJavaProject __) {
            this.__ = __;
            compilation = JkJavaProjectCompilationStep.ofProd(__, this);
            testing = new JkJavaProjectTestingStep(__, this);
            packaging = new JkJavaProjectPackagingStep(__, this);
            publishing = new JkJavaProjecPublishingStep(__, this);
            documentation = new JkJavaProjectDocumentationStep(__, this);
        }

        public JkJavaProjectCompilationStep<JkSteps> getCompilation() {
            return compilation;
        }

        public JkJavaProjectTestingStep getTesting() {
            return testing;
        }

        public JkJavaProjectPackagingStep getPackaging() {
            return packaging;
        }

        public JkJavaProjecPublishingStep getPublishing() {
            return publishing;
        }

        public JkJavaProjectDocumentationStep getDocumentation() {
            return documentation;
        }
    }


}