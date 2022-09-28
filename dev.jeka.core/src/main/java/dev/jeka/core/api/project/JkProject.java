package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Stands for the whole project model for building purpose. It has the same purpose and scope than the Maven _POM_ but
 * it also holds methods to actually perform builds.
 *
 * A complete overview is available <a href="https://jeka-dev.github.io/jeka/reference-guide/build-library-project-build/#project-api">here</a>.
 * <p/>
 *
 * A <code>JkProject</code> consists in 3 parts : <ul>
 *    <li>{@link JkProjectConstruction} : responsible to compile, tests and make jars</li>
 *    <li>{@link JkProjectDocumentation} : responsible to creates javadoc, sources jar and others</li>
 *    <li>{@link JkProjectPublication} : responsible to publish the artifacts on binary repositories (Maven or Ivy)</li>
 * </ul>
 * Each of these parts are optional. This mean that you don't have to set the publication part if the project is not
 * supposed to be published. Or you don't have to set the <i>construction</i> part if the project publishes artifacts
 * that are created by other means.
 * <p>
 * {@link JkProject} defines <i>base</i> and <i>output</i> directories as they are shared with the 3 parts.
 */
public class JkProject implements JkIdeSupport.JkSupplier {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    private Path baseDir = Paths.get("");

    private String outputDir = "jeka/output";

    private final JkStandardFileArtifactProducer<JkProject> artifactProducer =
            JkStandardFileArtifactProducer.ofParent(this).setArtifactFilenameComputation(this::getArtifactPath);

    private JkCoordinate.ConflictStrategy duplicateConflictStrategy = JkCoordinate.ConflictStrategy.FAIL;

    private final JkProjectDocumentation documentation;

    private final JkProjectConstruction construction;

    private final JkProjectPublication publication;

    public Function<JkIdeSupport, JkIdeSupport> ideSupportModifier = x -> x;

    private JkProject() {
        documentation = new JkProjectDocumentation( this);
        construction = new JkProjectConstruction(this);
        registerArtifacts();
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

    public JkCoordinate.ConflictStrategy getDuplicateConflictStrategy() {
        return duplicateConflictStrategy;
    }

    public JkProject setDuplicateConflictStrategy(JkCoordinate.ConflictStrategy duplicateConflictStrategy) {
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

    public JkStandardFileArtifactProducer<JkProject> getArtifactProducer() {
        return artifactProducer;
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
        builder.append("Defined Artifacts : " + artifactProducer.getArtifactIds());
        JkMavenPublication mavenPublication = publication.getMaven();
        if (mavenPublication.getModuleId() != null) {
            builder
                .append("Publish Maven repositories : " + mavenPublication.getPublishRepos()  + "\n")
                .append("Published Maven Module & version : " +
                        mavenPublication.getModuleId().toCoordinate(mavenPublication.getVersion()) + "\n")
                .append("Published Maven Dependencies :");
            mavenPublication.getDependencies().getEntries().forEach(dep -> builder.append("\n  " + dep));
        }
        JkIvyPublication ivyPublication = publication.getIvy();
        if (ivyPublication.getModuleId() != null) {
            builder
                    .append("Publish Ivy repositories : " + ivyPublication.getRepos()  + "\n")
                    .append("Published Ivy Module & version : " +
                            ivyPublication.getModuleId().toCoordinate(mavenPublication.getVersion()) + "\n")
                    .append("Published Ivy Dependencies :");
            ivyPublication.getDependencies().getEntries().forEach(dep -> builder.append("\n  " + dep));
        }
        return builder.toString();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        JkQualifiedDependencySet qualifiedDependencies = JkQualifiedDependencySet.computeIdeDependencies(
                construction.getProjectDependencies(),
                JkCoordinate.ConflictStrategy.TAKE_FIRST);
        JkIdeSupport ideSupport = JkIdeSupport.of(baseDir)
            .setSourceVersion(construction.getJvmTargetVersion())
            .setProdLayout(construction.getCompilation().getLayout())
            .setTestLayout(construction.getTesting().getCompilation().getLayout())
            .setGeneratedSourceDirs(construction.getCompilation().getGeneratedSourceDirs())
            .setDependencies(qualifiedDependencies)
            .setDependencyResolver(construction.getDependencyResolver());
        return ideSupportModifier.apply(ideSupport);
    }

    public void setJavaIdeSupport(Function<JkIdeSupport, JkIdeSupport> ideSupport) {
        this.ideSupportModifier = ideSupportModifier.andThen(ideSupport);
    }

    public JkLocalProjectDependency toDependency() {
        return toDependency(artifactProducer.getMainArtifactId(), null);
    }

    public JkLocalProjectDependency toDependency(JkTransitivity transitivity) {
        return toDependency(artifactProducer.getMainArtifactId(), transitivity);
    }

    public JkLocalProjectDependency toDependency(JkArtifactId artifactId, JkTransitivity transitivity) {
       Runnable maker = () -> artifactProducer.makeArtifact(artifactId);
       Path artifactPath = artifactProducer.getArtifactPath(artifactId);
       JkDependencySet exportedDependencies = construction.getCompilation().getDependencies()
               .merge(construction.getRuntimeDependencies()).getResult();
        return JkLocalProjectDependency.of(maker, artifactPath, this.baseDir, exportedDependencies)
                .withTransitivity(transitivity);
    }

    private Path getArtifactPath(JkArtifactId artifactId) {
        JkModuleId jkModuleId = publication.getModuleId();
        String fileBaseName = jkModuleId != null ? jkModuleId.getDotNotation()
                : baseDir.toAbsolutePath().getFileName().toString();
        return baseDir.resolve(outputDir).resolve(artifactId.toFileName(fileBaseName));
    }

    /**
     * Shorthand to build all missing artifacts for publication.
     */
    public void pack() {
        artifactProducer.makeAllMissingArtifacts();
    }

    private void registerArtifacts() {
        artifactProducer.putMainArtifact(construction::createBinJar);
        artifactProducer.putArtifact(SOURCES_ARTIFACT_ID, documentation::createSourceJar);
        artifactProducer.putArtifact(JAVADOC_ARTIFACT_ID, documentation::createJavadocJar);
    }

    /**
     * Specifies if Javadoc and sources jars should be included in pack/publish. Default is true;
     */
    public JkProject includeJavadocAndSources(boolean includeJavaDoc, boolean includeSources) {
        if (includeJavaDoc) {
            artifactProducer.putArtifact(JAVADOC_ARTIFACT_ID, documentation::createJavadocJar);
        } else {
            artifactProducer.removeArtifact(JAVADOC_ARTIFACT_ID);
        }
        if (includeSources) {
            artifactProducer.putArtifact(SOURCES_ARTIFACT_ID, documentation::createSourceJar);
        } else {
            artifactProducer.removeArtifact(SOURCES_ARTIFACT_ID);
        }
        return this;
    }

}