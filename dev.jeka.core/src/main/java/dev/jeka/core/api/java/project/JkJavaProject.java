package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkArtifactId;
import dev.jeka.core.api.depmanagement.JkLocalLibDependency;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * A Java project consists in 3 parts : <ul>
 *    <li>{@link JkJavaProjectJarProduction} : responsible to compile, tests and make jars</li>
 *    <li>{@link JkJavaProjectDocumentation} : responsible to creates javadoc, sources jar and others</li>
 *    <li>{@link JkJavaProjectPublication} : responsible to publish the artifacts on binary repositories (Maven or Ivy)</li>
 * </ul>
 * Each of these parts are optional. This mean that you don't have to set the publication part if the project is not
 * supposed to be published. Or you don't have to set the <i>production</i> part if the project publishes artifacts
 * that are created by other means than the <i>production<i/> part.
 * <p>
 * {@link JkJavaProject} defines <i>base</i> and <i>output</i> directories as they are shared with the 3 parts.
 */
public class JkJavaProject implements JkJavaIdeSupport.JkSupplier {

    private Path baseDir = Paths.get(".");

    private String outputDir = "jeka/output";

    private final JkJavaProjectDocumentation documentation;

    private final JkJavaProjectJarProduction jarProduction;

    private final JkJavaProjectPublication publication;

    private JkJavaProject() {
        documentation = new JkJavaProjectDocumentation( this);
        jarProduction = new JkJavaProjectJarProduction(this);
        publication = new JkJavaProjectPublication(this);
    }

    public static JkJavaProject of() {
        return new JkJavaProject();
    }

    public JkJavaProject apply(Consumer<JkJavaProject> projectConsumer) {
        projectConsumer.accept(this);
        return this;
    }

    // ---------------------------- Getters / setters --------------------------------------------

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

    public JkJavaProjectJarProduction getJarProduction() {
        return jarProduction;
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
            .append("Production sources : " + jarProduction.getCompilation().getLayout().getInfo()).append("\n")
            .append("Test sources : " + jarProduction.getTesting().getCompilation().getLayout().getInfo()).append("\n")
            .append("Java Source Version : " + jarProduction.getCompilation().getComputedCompileSpec().getSourceVersion() + "\n")
            .append("Source Encoding : " + jarProduction.getCompilation().getComputedCompileSpec().getEncoding() + "\n")
            .append("Source file count : " + jarProduction.getCompilation().getLayout().resolveSources().count(Integer.MAX_VALUE, false) + "\n")
            .append("Download Repositories : " + jarProduction.getDependencyManagement().getResolver().getRepos() + "\n")
            .append("Publish repositories : " + publication.getPublishRepos()  + "\n")
            .append("Declared Dependencies : " + jarProduction.getDependencyManagement().getDependencies().toList().size() + " elements.\n")
            .append("Defined Artifacts : " + publication.getArtifactProducer().getArtifactIds())
            .toString();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return JkJavaIdeSupport.of(baseDir)
            .setSourceVersion(jarProduction.getCompilation().getJavaVersion())
            .setProdLayout(jarProduction.getCompilation().getLayout())
            .setTestLayout(jarProduction.getTesting().getCompilation().getLayout())
            .setDependencies(jarProduction.getDependencyManagement().getDependencies())
            .setDependencyResolver(jarProduction.getDependencyManagement().getResolver());
    }

    public JkLocalLibDependency toDependency() {
        return toDependency(publication.getArtifactProducer().getMainArtifactId());
    }

    public JkLocalLibDependency toDependency(JkArtifactId artifactId) {
        return JkLocalLibDependency.of(
            () -> publication.getArtifactProducer().makeArtifact(artifactId),
                publication.getArtifactProducer().getArtifactPath(artifactId),
            this.baseDir,
            this.publication.getMavenPublication().getDependencies());
    }

    Path getArtifactPath(JkArtifactId artifactId) {
        return baseDir.resolve(outputDir).resolve(artifactId.toFileName(publication.getModuleId().getDotedName()));
    }

}