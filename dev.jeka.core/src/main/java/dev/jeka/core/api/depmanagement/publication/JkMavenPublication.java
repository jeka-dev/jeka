package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.tooling.maven.JkMvn;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Publication specific information to include in POM file in order to be published of a Maven repository.
 * This information contains : <ul>
 *   <li>The artifacts to be published (main artifact and artifacts with classifiers)</li>
 *   <li>Information about describing the project as some public repositories require</li>
 * </ul>
 */
public final class JkMavenPublication {

    /**
     * Represents the Maven metadata for a publication.
     */
    public final JkPomMetadata pomMetadata = JkPomMetadata.of();

    /**
     * Collection of runnables that are executed before publishing to M2 repos.
     */
    public final JkRunnables preActions = JkRunnables.of();

    /**
     * Collection of runnables that are executed after publishing to M2 repos.
     */
    public final JkRunnables postActions = JkRunnables.of();

    private Function<JkDependencySet, JkDependencySet> dependencies = UnaryOperator.identity();

    private Supplier<JkModuleId> moduleIdSupplier = () -> null;

    private Supplier<JkVersion> versionSupplier = () -> JkVersion.UNSPECIFIED;

    private final JkArtifactPublisher artifactPublisher;

    private JkRepoSet repos = JkRepoSet.ofLocal();

    private Supplier<JkRepoSet> bomResolverRepoSupplier = JkRepoSet::of;

    private final Map<JkModuleId, JkVersion> managedDependencies = new HashMap<>();

    private JkFileSigner defaultSigner;  // Can be null. Signer used if none is defined on repos

    private JkMavenPublication(JkArtifactLocator artifactLocator) {
        this.artifactPublisher = JkArtifactPublisher.of(artifactLocator);
    }

    /**
     * Creates a {@link JkMavenPublication} with the specified artifact locator.
     * @param artifactLocator The artifact locator for locating the artifact files to publish.
     */
    public static JkMavenPublication of(JkArtifactLocator artifactLocator) {
        return new JkMavenPublication(artifactLocator);
    }

    /**
     * Creates a {@link JkMavenPublication} that can contains only a POM, and no artifacts.
     * The typical usage is to publish BOMs.
     */
    public static JkMavenPublication ofPomOnly() {
        return of(JkArtifactLocator.VOID);
    }

    /**
     * Configure the dependencies that will be exported with the published module.<br/>
     * By default, JeKa computes it from the compile and runtime dependencies.
     * This method allows to customize these dependencies by adding/removing or changing their transitivity.
     */
    public JkMavenPublication customizeDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependencies = dependencies.andThen(modifier);
        return this;
    }

    /**
     * Adds the specified moduleId and version in the <i>dependencyManagement</i> of the POM file to be published.
     * This is the way to create BOM file.
     */
    public JkMavenPublication addManagedDependenciesInPom(String moduleId, String version) {
        this.managedDependencies.put(JkModuleId.of(moduleId), JkVersion.of(version));
        return this;
    }

    /**
     * Returns the dependencies to be included in POM as transitive dependencies.
     */
    public JkDependencySet getDependencies() {
        return dependencies.apply(JkDependencySet.of());
    }

    /**
     * Sets the supplier providing the moduleId (group + artifactName) for this publication.
     */
    public JkMavenPublication setModuleIdSupplier(Supplier<JkModuleId> moduleIdSupplier) {
        JkUtilsAssert.argument(moduleIdSupplier != null, " moduleId supplier can't be null.");
        this.moduleIdSupplier = moduleIdSupplier;
        return this;
    }

    /**
     * Sets the moduleId (group + artifactName) for this publication.
     * @see #setModuleIdSupplier(Supplier)
     */
    public JkMavenPublication setModuleId(String moduleId) {
        this.moduleIdSupplier = () -> JkModuleId.of(moduleId);
        return this;
    }

    /**
     * Sets the supplier providing the version of the artifacts to publish.
     */
    public JkMavenPublication setVersionSupplier(Supplier<JkVersion> versionSupplier) {
        JkUtilsAssert.argument(versionSupplier != null, " version supplier can't be null.");
        this.versionSupplier = versionSupplier;
        return this;
    }

    /**
     * Sets the version of the artifacts to publish.
     * @see #setVersionSupplier(Supplier)
     */
    public JkMavenPublication setVersion(String version) {
        JkUtilsAssert.argument(!JkUtilsString.isBlank(version), "Version can't be blank. Was '%s'.", version);
        this.versionSupplier = () -> JkVersion.of(version);
        return this;
    }

    /**
     * Sets the supplier providing the download repositories used to resolve BOMs.
     */
    public JkMavenPublication setBomResolutionRepos(Supplier<JkRepoSet> repoSupplier) {
        this.bomResolverRepoSupplier = repoSupplier;
        return this;
    }

    /**
     * Returns the moduleId (group + artifact name) for this publication.
     */
    public JkModuleId getModuleId() {
        return moduleIdSupplier.get();
    }

    /**
     * Returns the version of the artifacts for this publication.
     */
    public JkVersion getVersion() {
        return versionSupplier.get();
    }

    /**
     * Returns the default file signer for this publication.<p>
     * Normally, each publish repository can define its own signer.
     * Conveniently we can specify a file signer for repositories which don't have.
     */
    public JkFileSigner getDefaultSigner() {
        return defaultSigner;
    }

    /**
     * Sets the default file signer to use for this publication.
     * @see #getDefaultSigner()
     */
    public JkMavenPublication setDefaultSigner(JkFileSigner defaultSigner) {
        this.defaultSigner = defaultSigner;
        return this;
    }

    /**
     * Returns the repositories where this publication will be published.
     */
    public JkRepoSet getRepos() {
        return repos;
    }

    /**
     * Sets the repositories where this publication will be published.
     */
    public JkMavenPublication setRepos(JkRepoSet repoSet) {
        this.repos = repoSet;
        return this;
    }

    /**
     * Adds the specified repositories to the publication repositories.
     * @see #setRepos(JkRepoSet)
     */
    public JkMavenPublication addRepos(JkRepo ...repoArgs) {
        Arrays.stream(repoArgs).forEach(repo -> repos = repos.and(repo));
        return this;
    }

    /**
     * Adds the specified artifact to the publication. If the artifact file is not present,
     * this one will be created using the specified artifact file maker.
     * @param artifactId The artifactId to add to publication.
     * @param artifactFileMaker  A {@link Consumer} creating the artifact file at the provided location.
     */
    public JkMavenPublication putArtifact(JkArtifactId artifactId, Consumer<Path> artifactFileMaker) {
        this.artifactPublisher.putArtifact(artifactId, artifactFileMaker);
        return this;
    }

    /**
     * Adds the specified artifact to the publication assuming the artifact file will exist when {@link #publish()}
     * will be invoked . If the artifact file is not present, an exception will be raised.
     */
    public JkMavenPublication putArtifact(JkArtifactId artifactId) {
        return this.putArtifact(artifactId, null);
    }

    /**
     * Removes the specified artifact from this publication.
     */
    public JkMavenPublication removeArtifact(JkArtifactId artifactId) {
        this.artifactPublisher.removeArtifact(artifactId);
        return this;
    }

    /**
     * Publishes this publication to its defined repositories
     */
    public JkMavenPublication publish() {
        preActions.run();
        publish(this.repos.withDefaultSigner(defaultSigner));
        postActions.run();
        return this;
    }

    /**
     * Publishes this publication on the JeKa local repository
     */
    public JkMavenPublication publishLocal() {
        publish(JkRepoSet.ofLocal());
        return this;
    }

    /**
     * Publishes this publication on the M2 local repository
     */
    public JkMavenPublication publishLocalM2() {
        publish(JkRepo.of(JkMvn.getM2LocalRepo()).toSet());
        return this;
    }

    /**
     * Returns a string representation of the information for this {@link JkMavenPublication}.
     */
    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append("ModuleId    : " + this.moduleIdSupplier.get() + "\n");
        sb.append("Version     : " + this.versionSupplier.get() + "\n");
        sb.append("Repos       : " + "\n");
        this.repos.getRepos().forEach(repo -> sb.append("  " + repo + "\n"));
        sb.append("Artifacts   : " +  "\n");
        Arrays.stream(this.artifactPublisher.info().split("\n")).forEach(
                line -> sb.append("  " + line + "\n"));
        sb.append("Dependencies : " + "\n");
        getDependencies().withResolvedBoms(this.repos).toResolvedModuleVersions().getEntries().forEach(
                dep -> sb.append("  " + dep + "\n"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return "JkMavenPublication{" +
                "artifactFileLocator=" + artifactPublisher +
                ", extraInfo=" + pomMetadata +
                '}';
    }

    /**
     * Shorthand to get the first declared publication repository.
     */
    public JkRepo findFirstNonLocalRepo() {
        return this.getRepos().getRepos().stream()
                .filter(repo1 -> !repo1.isLocal())
                .findFirst()
                .orElse(null);
    }

    /**
     * Computes the published transitive dependencies from the specified <i>compile</i> and <i>runtime</i>
     * dependencies of the project that we are publishing artifacts for.
     */
    public static JkDependencySet computeMavenPublishDependencies(JkDependencySet compileDeps,
                                                                  JkDependencySet runtimeDeps,
                                                                  JkCoordinate.ConflictStrategy strategy) {
        JkDependencySetMerge merge = runtimeDeps.merge(compileDeps);
        JkDependencySet mergeResult = merge.getResult().normalised(strategy);
        List<JkDependency> result = new LinkedList<>();
        for (JkCoordinateDependency coordinateDependency : mergeResult.getCoordinateDependencies()) {
            JkTransitivity transitivity = JkTransitivity.COMPILE;
            if (merge.getAbsentDependenciesFromRight().contains(coordinateDependency)) {
                transitivity = JkTransitivity.RUNTIME;
            }
            result.add(coordinateDependency.withTransitivity(transitivity));
        }
        return JkDependencySet.of(result).withVersionProvider(mergeResult.getVersionProvider());
    }

    private JkMavenPublication publish(JkRepoSet repos) {

        JkUtilsAssert.state(moduleIdSupplier.get() != null, "moduleId cannot be null.");
        JkUtilsAssert.state(versionSupplier.get() != null, "version cannot be null.");

        JkRepoSet bomRepos = this.bomResolverRepoSupplier.get().and(repos);
        JkDependencySet dependencySet = this.getDependencies()
                .withResolvedBoms(bomRepos)
                .assertNoUnspecifiedVersion()
                .toResolvedModuleVersions();

        artifactPublisher.makeMissingArtifacts();

        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos, null);
        JkCoordinate coordinate = getModuleId().toCoordinate(versionSupplier.get());
        internalPublisher.publishMaven(
                coordinate,
                artifactPublisher,
                pomMetadata,
                dependencySet,
                this.managedDependencies);
        return this;
    }

}
