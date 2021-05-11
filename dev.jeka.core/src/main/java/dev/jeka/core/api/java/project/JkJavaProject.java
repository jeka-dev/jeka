package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * A Java project consists in 3 parts : <ul>
 *    <li>{@link JkJavaProjectConstruction} : responsible to compile, tests and make jars</li>
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

    private String artifactBaseName;  // artifact files will be named as artifactBaseName-classifier.ext

    private JkVersionedModule.ConflictStrategy duplicateConflictStrategy = JkVersionedModule.ConflictStrategy.FAIL;

    private final JkJavaProjectDocumentation documentation;

    private final JkJavaProjectConstruction construction;

    private final JkJavaProjectPublication publication;

    private JkJavaProject() {
        documentation = new JkJavaProjectDocumentation( this);
        construction = new JkJavaProjectConstruction(this);
        publication = new JkJavaProjectPublication(this);
    }

    public static JkJavaProject of() {
        return new JkJavaProject();
    }

    public JkJavaProject apply(Consumer<JkJavaProject> projectConsumer) {
        projectConsumer.accept(this);
        return this;
    }

    public JkJavaProjectSimpleFacade simpleFacade() {
        return new JkJavaProjectSimpleFacade(this);
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

    public JkVersionedModule.ConflictStrategy getDuplicateConflictStrategy() {
        return duplicateConflictStrategy;
    }

    public JkJavaProject setDuplicateConflictStrategy(JkVersionedModule.ConflictStrategy duplicateConflictStrategy) {
        this.duplicateConflictStrategy = duplicateConflictStrategy;
        return this;
    }

    public JkJavaProjectConstruction getConstruction() {
        return construction;
    }

    public JkJavaProjectPublication getPublication() {
        return publication;
    }

    public JkJavaProjectDocumentation getDocumentation() {
        return documentation;
    }

    public String getArtifactBaseName() {
        return artifactBaseName != null ? artifactBaseName : baseDir.getFileName().toString();
    }

    public JkJavaProject setArtifactBaseName(String artifactBaseName) {
        this.artifactBaseName = artifactBaseName;
        return this;
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + getBaseDir().getFileName();
    }

    public String getInfo() {
        JkDependencySet compileDependencies = construction.getCompilation().getDependencies();
        JkDependencySet runtimeDependencies = construction.getRuntimeDependencies();
        JkDependencySet testDependencies = construction.getTesting().getCompilation().getDependencies();
        StringBuilder builder = new StringBuilder("Project Location : " + this.getBaseDir() + "\n")
            .append("Production sources : " + construction.getCompilation().getLayout().getInfo()).append("\n")
            .append("Test sources : " + construction.getTesting().getCompilation().getLayout().getInfo()).append("\n")
            .append("Java Source Version : " + construction.getCompilation().getJavaVersion() + "\n")
            .append("Source Encoding : " + construction.getCompilation().getSourceEncoding() + "\n")
            .append("Source file count : " + construction.getCompilation().getLayout().resolveSources()
                    .count(Integer.MAX_VALUE, false) + "\n")
            .append("Download Repositories : " + construction.getDependencyResolver().getRepos() + "\n")
            .append("Declared Compile Dependencies : " + compileDependencies.getEntries().size() + " elements.\n");
        compileDependencies.getVersionedDependencies().forEach(dep -> builder.append("  " + dep + "\n"));
        builder.append("Declared Runtime Dependencies : " + runtimeDependencies
                .getEntries().size() + " elements.\n");
        runtimeDependencies.getVersionedDependencies().forEach(dep -> builder.append("  " + dep + "\n"));
        builder.append("Declared Test Dependencies : " + testDependencies.getEntries().size() + " elements.\n");
        testDependencies.getVersionedDependencies().forEach(dep -> builder.append("  " + dep + "\n"));
        builder.append("Defined Artifacts : " + publication.getArtifactProducer().getArtifactIds());
        JkMavenPublication mavenPublication = publication.getMaven();
        if (mavenPublication.getModuleId() != null) {
            builder
                .append("Publish Maven repositories : " + mavenPublication.getRepos()  + "\n")
                .append("Published Maven Module & version : " +
                        mavenPublication.getModuleId().withVersion(mavenPublication.getVersion()) + "\n")
                .append("Published Maven Dependencies :");
            mavenPublication.getDependencies().getEntries().forEach(dep -> builder.append("\n  " + dep));
        }
        JkIvyPublication ivyPublication = publication.getIvy();
        if (ivyPublication.getModuleId() != null) {
            builder
                    .append("Publish Ivy repositories : " + ivyPublication.getRepos()  + "\n")
                    .append("Published Ivy Module & version : " +
                            ivyPublication.getModuleId().withVersion(mavenPublication.getVersion()) + "\n")
                    .append("Published Ivy Dependencies :");
            ivyPublication.getDependencies().getEntries().forEach(dep -> builder.append("\n  " + dep));
        }
        return builder.toString();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        JkQualifiedDependencySet qualifiedDependencies = JkQualifiedDependencySet.computeIdeDependencies(
                construction.getCompilation().getDependencies(),
                construction.getRuntimeDependencies(),
                construction.getTesting().getCompilation().getDependencies(),
                JkVersionedModule.ConflictStrategy.TAKE_FIRST);
        return JkJavaIdeSupport.of(baseDir)
            .setSourceVersion(construction.getCompilation().getJavaVersion())
            .setProdLayout(construction.getCompilation().getLayout())
            .setTestLayout(construction.getTesting().getCompilation().getLayout())
            .setDependencies(qualifiedDependencies)
            .setDependencyResolver(construction.getDependencyResolver());
    }

    public JkLocalProjectDependency toDependency() {
        return toDependency(publication.getArtifactProducer().getMainArtifactId(), null);
    }

    public JkLocalProjectDependency toDependency(JkTransitivity transitivity) {
        return toDependency(publication.getArtifactProducer().getMainArtifactId(), transitivity);
    }

    public JkLocalProjectDependency toDependency(JkArtifactId artifactId, JkTransitivity transitivity) {
       Runnable maker = () -> publication.getArtifactProducer().makeArtifact(artifactId);
       Path artifactPath = publication.getArtifactProducer().getArtifactPath(artifactId);
       JkDependencySet exportedDependencies = construction.getCompilation().getDependencies()
               .merge(construction.getRuntimeDependencies()).getResult();
        return JkLocalProjectDependency.of(maker, artifactPath, this.baseDir, exportedDependencies)
                .withTransitivity(transitivity);
    }

    Path getArtifactPath(JkArtifactId artifactId) {
        JkModuleId moduleId = publication.getModuleId();
        String fileBaseName = moduleId != null ? moduleId.getDotedName() : this.getArtifactBaseName();
        return baseDir.resolve(outputDir).resolve(artifactId.toFileName(fileBaseName));
    }

}