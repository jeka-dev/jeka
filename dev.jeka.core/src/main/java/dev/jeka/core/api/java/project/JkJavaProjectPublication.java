package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.tooling.JkIvyPublication;
import dev.jeka.core.api.depmanagement.tooling.JkMavenPublication;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Responsible to publish artifacts on the repository. If the project does not publish anything, this part can be
 * ignored. Jeka can publish on Maven and Ivy repositories. <p>
 * From here, you can control what to publish and the transitive dependencies, depending on the repo system the project
 * is published. Note that a project can be be published on many repositories of different systems.
 */
public class JkJavaProjectPublication {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    private final JkJavaProject project;

    private JkModuleId moduleId;

    private Supplier<JkVersion> versionSupplier;

    private JkRepoSet publishRepos = JkRepoSet.of();

    private UnaryOperator<Path> signer;  // Can be null

    private final JkStandardFileArtifactProducer<JkJavaProjectPublication> artifactProducer;

    private final JkMavenPublication<JkJavaProjectPublication> mavenPublication;

    private final JkIvyPublication<JkJavaProjectPublication> ivyPublication;

    private final JkRunnables<JkJavaProjectPublication> postActions;

    /**
     * For parent chaining
     */
    public final JkJavaProject __;

    JkJavaProjectPublication(JkJavaProject project) {
        this.project = project;
        this.__ = project;
        this.versionSupplier = () -> JkVersion.UNSPECIFIED;
        artifactProducer = JkStandardFileArtifactProducer.ofParent(this)
                .setArtifactFilenameComputation(project::getArtifactPath);
        registerArtifacts();
        this.mavenPublication = JkMavenPublication.of(this)
            .setArtifactLocator(() -> artifactProducer)
            .setDependencies(deps -> project.getConstruction().getDependencyResolver().getDependencies())
            .setVersionedModule(() -> getModuleId().withVersion(versionSupplier.get()));
        this.ivyPublication = JkIvyPublication.of(this)
            .addArtifacts(() -> artifactProducer)
            .setVersionedModule(() -> getModuleId().withVersion(versionSupplier.get()))
            .setDependencies(deps -> project.getConstruction().getDependencyResolver().getDependencies())
            .setResolvedVersionProvider(this::getResolvedVersions);
        this.postActions = JkRunnables.ofParent(this);
    }

    public JkJavaProjectPublication apply(Consumer<JkJavaProjectPublication> consumer) {
        consumer.accept(this);
        return this;
    }

    /**
     * Returns the module id (group and name) for this project. This information is used for naming produced artifact files,
     * publishing. It is also consumed by tools as SonarQube.
     * If no moduleId has been explicitly set, this methods a module id inferred from the project base directory name.
     */
    public JkModuleId getModuleId() {
        return moduleId != null ? moduleId : JkModuleId.of(project.getBaseDir().getFileName().toString());
    }

    /**
     * @see #getModuleId()
     */
    public JkJavaProjectPublication setModuleId(JkModuleId moduleId) {
        JkUtilsAssert.argument(moduleId != null, "ModuleId cannot be null.");
        this.moduleId = moduleId;
        return this;
    }

    /**
     * @see #getModuleId()
     */
    public JkJavaProjectPublication setModuleId(String groupAndName) { ;
        return this.setModuleId(JkModuleId.of(groupAndName));
    }

    /**
     * Set the version supplier used to compute this project version. For example, it can be : <ul>
     *     <li>A hardcoded supplier as : <code>() -> 1.0.0</code><</li>
     *     <li>A dynamic supplier based on VCS versioning as : <code>() -> JkGitWrapper.of(baseDir).getVersionFromTags</code></li>
     *     <li>Any code retrieving version from env var, property files, ....</li>
     * </ul>
     * This version information is used for publishing project artifacts on binary repository, but also may be used by
     * tools as Sonarqube to identify projects.<p>
     * The default value is {@link JkVersion#UNSPECIFIED}
     */
    public JkJavaProjectPublication setVersionSupplier(Supplier<JkVersion> versionSupplier) {
        JkUtilsAssert.argument(versionSupplier != null, "Version supplier cannot be null.");
        this.versionSupplier = versionSupplier;
        return this;
    }

    /**
     * @see #setVersionSupplier(Supplier)
     */
    public JkJavaProjectPublication setVersion(JkVersion version) {
        JkUtilsAssert.argument(version != null, "Version cannot be null.");
        return this.setVersionSupplier(() -> version);
    }

    /**
     * @see #setVersionSupplier(Supplier)
     */
    public JkJavaProjectPublication setVersion(String version) {
        return setVersion(JkVersion.of(version));
    }

    /**
     * @see #setVersionSupplier(Supplier)
     */
    public JkVersion getVersion() {
        JkVersion result = versionSupplier.get();
        JkUtilsAssert.state(result != null, "Version returned by supplier is null.");
        return this.versionSupplier.get();
    }

    public UnaryOperator<Path> getSigner() {
        return signer;
    }

    public JkRepoSet getPublishRepos() {
        return this.publishRepos;
    }

    public JkRunnables<JkJavaProjectPublication> getPostActions() {
        return postActions;
    }

    public JkJavaProjectPublication setRepos(JkRepoSet publishRepos) {
        JkUtilsAssert.argument(publishRepos != null, "publish repos cannot be null.");
        this.publishRepos = publishRepos;
        return this;
    }

    public JkJavaProjectPublication addRepos(JkRepo ... repos) {
        for (JkRepo repo : repos) {
            this.publishRepos = this.publishRepos.and(repo);
        }
        return this;
    }

    public JkJavaProjectPublication setSigner(UnaryOperator<Path> signer) {
        this.signer = signer;
        return this;
    }

    public JkMavenPublication<JkJavaProjectPublication> getMavenPublication() {
        return mavenPublication;
    }

    public JkIvyPublication<JkJavaProjectPublication> getIvyPublication() {
        return ivyPublication;
    }

    /**
     * Publishes all defined artifacts.
     */
    public void publish() {
        JkUtilsAssert.state(getVersion() != null, "No versioned module has been set on "
                + project + ". Can't publish.");
        JkRepoSet repos = this.publishRepos;
        if (repos == null) {
            repos = JkRepoSet.ofLocal();
            JkLog.warn("No publish repo has been mentioned. Publishing on local...");
        }
        publishMaven(repos);
        publishIvy(repos);
        postActions.run();
    }

    public void publishLocal() {
        JkUtilsAssert.state(getVersion() != null, "No versioned module has been set on "
                + project + ". Can't publish.");
        publishMaven(JkRepo.ofLocal().toSet());
        postActions.run();
    }

    private void publishMaven(JkRepoSet repos) {
        mavenPublication.publish(repos, signer);
    }

    private void publishIvy(JkRepoSet repos) {
        if (!repos.hasIvyRepo()) {
            return;
        }
        JkLog.startTask("Prepare Ivy publication");
        this.ivyPublication.publish(repos);
        JkLog.endTask();
    }

    private JkVersionProvider getResolvedVersions() {
        return project.getConstruction().getDependencyResolver().resolveDependencies().getResolvedVersionProvider();
    }

    public JkStandardFileArtifactProducer<JkJavaProjectPublication> getArtifactProducer() {
        return artifactProducer;
    }

    private void registerArtifacts() {
        artifactProducer.putMainArtifact(project.getConstruction()::createBinJar);
        artifactProducer.putArtifact(SOURCES_ARTIFACT_ID, project.getDocumentation()::createSourceJar);
        artifactProducer.putArtifact(JAVADOC_ARTIFACT_ID, project.getDocumentation()::createJavadocJar);
    }
}
