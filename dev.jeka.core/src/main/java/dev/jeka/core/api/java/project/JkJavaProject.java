package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkFileSystemLocalizable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

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
public class JkJavaProject implements JkJavaIdeSupport.JkSupplier, JkFileSystemLocalizable, JkArtifactProducer.JkSupplier {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    private Path baseDir = Paths.get(".");

    private String outputDir = "jeka/output";

    private final JkDependencyManagement<JkJavaProject> dependencyManagement;

    private final JkArtifactBasicProducer<JkJavaProject> artifactProducer;

    private final JkJavaProjectCompilation compilation;

    private final JkJavaProjectTesting testing;

    private final JkJavaProjectDocumentation documentation;

    private final JkJavaProjectPackaging packaging;

    private final JkJavaProjectPublication publication;


    private JkJavaProject() {
        dependencyManagement = JkDependencyManagement.ofParent(this);
        compilation = JkJavaProjectCompilation.ofProd(this);
        testing = new JkJavaProjectTesting(this);
        documentation = new JkJavaProjectDocumentation( this);
        packaging = new JkJavaProjectPackaging(this);
        publication = new JkJavaProjectPublication(this);
        artifactProducer = JkArtifactBasicProducer.ofParent(this)
                .setArtifactFilenameComputation(this::getOutputDir, this::artifactFileNamePart);
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

    public JkJavaProjectCompilation<JkJavaProject> getCompilation() {
        return compilation;
    }

    public JkJavaProjectTesting getTesting() {
        return testing;
    }

    public JkJavaProjectPackaging getPackaging() {
        return packaging;
    }

    public JkJavaProjectPublication getPublication() {
        return publication;
    }

    public JkJavaProjectDocumentation getDocumentation() {
        return documentation;
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + getBaseDir().getFileName();
    }

    public String getInfo() {
        return new StringBuilder("Project Location : " + this.getBaseDir() + "\n")
                .append("Published Module & version : " + publication.getModuleId() + ":" + publication.getVersion() + "\n")
                .append("Production sources : " + compilation.getLayout().getInfo()).append("\n")
                .append("Test sources : " + testing.getCompilation().getLayout().getInfo()).append("\n")
                .append("Java Source Version : " + compilation.getComputedCompileSpec().getSourceVersion() + "\n")
                .append("Source Encoding : " + compilation.getComputedCompileSpec().getEncoding() + "\n")
                .append("Source file count : " + compilation.getLayout().resolveSources().count(Integer.MAX_VALUE, false) + "\n")
                .append("Download Repositories : " + dependencyManagement.getResolver().getRepos() + "\n")
                .append("Publish repositories : " + publication.getPublishRepos()  + "\n")
                .append("Declared Dependencies : " + dependencyManagement.getDependencies().toList().size() + " elements.\n")
                .append("Defined Artifacts : " + getArtifactProducer().getArtifactIds())
                .toString();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return JkJavaIdeSupport.of(baseDir)
                .setSourceVersion(compilation.getJavaVersion())
                .setProdLayout(compilation.getLayout())
                .setTestLayout(testing.getCompilation().getLayout())
                .setDependencies(this.dependencyManagement.getDependencies())
                .setDependencyResolver(this.dependencyManagement.getResolver());
    }

    private String artifactFileNamePart() {
        return publication.getModuleId().getDotedName();
    }

    private void registerArtifacts() {
        artifactProducer.putMainArtifact(packaging::createBinJar,
                () -> dependencyManagement.fetchDependencies(JkJavaDepScopes.RUNTIME).getFiles());
        artifactProducer.putArtifact(SOURCES_ARTIFACT_ID, packaging::createSourceJar);
        artifactProducer.putArtifact(JAVADOC_ARTIFACT_ID, packaging::createJavadocJar);
    }

}