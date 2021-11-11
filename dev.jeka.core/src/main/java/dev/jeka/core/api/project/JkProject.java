package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A Java project consists in 3 parts : <ul>
 *    <li>{@link JkProjectConstruction} : responsible to compile, tests and make jars</li>
 *    <li>{@link JkProjectDocumentation} : responsible to creates javadoc, sources jar and others</li>
 *    <li>{@link JkProjectPublication} : responsible to publish the artifacts on binary repositories (Maven or Ivy)</li>
 * </ul>
 * Each of these parts are optional. This mean that you don't have to set the publication part if the project is not
 * supposed to be published. Or you don't have to set the <i>production</i> part if the project publishes artifacts
 * that are created by other means than the <i>production<i/> part.
 * <p>
 * {@link JkProject} defines <i>base</i> and <i>output</i> directories as they are shared with the 3 parts.
 */
public class JkProject implements JkIdeSupport.JkSupplier {

    private Path baseDir = Paths.get(".");

    private String outputDir = "jeka/output";

    private String artifactBaseName;  // artifact files will be named as artifactBaseName-classifier.ext

    private JkVersionedModule.ConflictStrategy duplicateConflictStrategy = JkVersionedModule.ConflictStrategy.FAIL;

    private final JkProjectDocumentation documentation;

    private final JkProjectConstruction construction;

    private final JkProjectPublication publication;

    public Function<JkIdeSupport, JkIdeSupport> ideSupportModifier = x -> x;

    private JkProject() {
        documentation = new JkProjectDocumentation( this);
        construction = new JkProjectConstruction(this);
        publication = new JkProjectPublication(this);
    }

    public static JkProject of() {
        return new JkProject();
    }

    public JkProject apply(Consumer<JkProject> projectConsumer) {
        projectConsumer.accept(this);
        return this;
    }

    public JkProjectSimpleFacade simpleFacade() {
        return new JkProjectSimpleFacade(this);
    }

    // ---------------------------- Getters / setters --------------------------------------------

    public Path getBaseDir() {
        return this.baseDir;
    }

    public JkProject setBaseDir(Path baseDir) {
        this.baseDir = JkUtilsPath.relativizeFromWorkingDir(baseDir);
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
    public JkProject setOutputDir(String relativePath) {
        this.outputDir = relativePath;
        return this;
    }

    public JkVersionedModule.ConflictStrategy getDuplicateConflictStrategy() {
        return duplicateConflictStrategy;
    }

    public JkProject setDuplicateConflictStrategy(JkVersionedModule.ConflictStrategy duplicateConflictStrategy) {
        this.duplicateConflictStrategy = duplicateConflictStrategy;
        return this;
    }

    public JkProjectConstruction getConstruction() {
        return construction;
    }

    public JkProjectPublication getPublication() {
        return publication;
    }

    public JkProjectDocumentation getDocumentation() {
        return documentation;
    }

    public String getArtifactBaseName() {
        return artifactBaseName != null ? artifactBaseName : baseDir.toAbsolutePath().getFileName().toString();
    }

    public JkProject setArtifactBaseName(String artifactBaseName) {
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
            .append("Java Source Version : " + construction.getJvmTargetVersion() + "\n")
            .append("Source Encoding : " + construction.getSourceEncoding() + "\n")
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
    public JkIdeSupport getJavaIdeSupport() {
        JkQualifiedDependencySet qualifiedDependencies = JkQualifiedDependencySet.computeIdeDependencies(
                construction.getProjectDependencies(),
                JkVersionedModule.ConflictStrategy.TAKE_FIRST);
        JkIdeSupport ideSupport = JkIdeSupport.of(baseDir)
            .setSourceVersion(construction.getJvmTargetVersion())
            .setProdLayout(construction.getCompilation().getLayout())
            .setTestLayout(construction.getTesting().getCompilation().getLayout())
            .setDependencies(qualifiedDependencies)
            .setDependencyResolver(construction.getDependencyResolver());
        return ideSupportModifier.apply(ideSupport);
    }

    public void setJavaIdeSupport(Function<JkIdeSupport, JkIdeSupport> ideSupport) {
        this.ideSupportModifier = ideSupportModifier.andThen(ideSupport);
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