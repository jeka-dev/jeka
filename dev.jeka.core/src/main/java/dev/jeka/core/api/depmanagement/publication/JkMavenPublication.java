package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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

    public final JkPomMetadata pomMetadata = JkPomMetadata.of();

    private Function<JkDependencySet, JkDependencySet> dependencies = UnaryOperator.identity();

    private Supplier<JkModuleId> moduleIdSupplier = () -> null;

    private Supplier<JkVersion> versionSupplier = () -> JkVersion.UNSPECIFIED;

    private Supplier<JkArtifactLocator> artifactLocatorSupplier;

    private JkRepoSet publishRepos = JkRepoSet.ofLocal();

    private Supplier<JkRepoSet> bomResolverRepoSupplier = () -> JkRepoSet.of();

    private UnaryOperator<Path> defaultSigner;  // Can be null. Signer used if none is defined on repos

    public final JkRunnables preActions = JkRunnables.of();

    public final JkRunnables postActions = JkRunnables.of();

    private JkMavenPublication() {
    }

    public static JkMavenPublication of() {
        return new JkMavenPublication();
    }

    /**
     * Configure the dependencies that will be exported with the published module.<br/>
     * By default, JeKa computes it from the compile and runtime dependencies.
     * This method allows to customize these dependencies by adding/removing or changing their transitivity.
     */
    public JkMavenPublication configureDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependencies = dependencies.andThen(modifier);
        return this;
    }

    public JkDependencySet getDependencies() {
        return dependencies.apply(JkDependencySet.of());
    }

    public JkMavenPublication setModuleId(String moduleId) {
        this.moduleIdSupplier = () -> JkModuleId.of(moduleId);
        return this;
    }

    public JkMavenPublication setModuleIdSupplier(Supplier<JkModuleId> moduleIdSupplier) {
        this.moduleIdSupplier = moduleIdSupplier;
        return this;
    }

    public JkMavenPublication setVersion(String version) {
        this.versionSupplier = () -> JkVersion.of(version);
        return this;
    }

    public JkMavenPublication setVersionSupplier(Supplier<JkVersion> versionSupplier) {
        this.versionSupplier = versionSupplier;
        return this;
    }

    public JkMavenPublication setBomResolutionRepos(Supplier<JkRepoSet> repoSupplier) {
        this.bomResolverRepoSupplier = repoSupplier;
        return this;
    }

    public JkModuleId getModuleId() {
        return moduleIdSupplier.get();
    }

    public JkVersion getVersion() {
        return versionSupplier.get();
    }

    public UnaryOperator<Path> getDefaultSigner() {
        return defaultSigner;
    }

    public JkMavenPublication setDefaultSigner(UnaryOperator<Path> defaultSigner) {
        this.defaultSigner = defaultSigner;
        return this;
    }

    public JkArtifactLocator getArtifactLocator() {
        return artifactLocatorSupplier.get();
    }

    public JkMavenPublication setArtifactLocatorSupplier(Supplier<JkArtifactLocator> artifactLocatorSupplier) {
        this.artifactLocatorSupplier = artifactLocatorSupplier;
        return this;
    }

    public JkMavenPublication setArtifactLocator(JkArtifactLocator artifactLocatorArg) {
        this.artifactLocatorSupplier = () -> artifactLocatorArg;
        return this;
    }

    public JkRepoSet getPublishRepos() {
        return publishRepos;
    }

    public JkMavenPublication setRepos(JkRepoSet repoSet) {
        this.publishRepos = repoSet;
        return this;
    }

    public JkMavenPublication addRepos(JkRepo ...repoArgs) {
        Arrays.stream(repoArgs).forEach(repo -> publishRepos = publishRepos.and(repo));
        return this;
    }

    /**
     * Publishes this publication to its defined repositories
     */
    public JkMavenPublication publish() {
        preActions.run();
        publish(this.publishRepos.withDefaultSigner(defaultSigner));
        postActions.run();
        return this;
    }

    /**
     * Publishes this publication on the local repository
     */
    public JkMavenPublication publishLocal() {
        preActions.run();
        publish(JkRepoSet.ofLocal());
        postActions.run();
        return this;
    }


    private JkMavenPublication publish(JkRepoSet repos) {
        JkRepoSet bomRepos = this.bomResolverRepoSupplier.get().and(repos);
        JkDependencySet dependencySet = this.getDependencies()
                .withResolvedBoms(bomRepos)
                .assertNoUnspecifiedVersion()
                .toResolvedModuleVersions();
        JkUtilsAssert.state(artifactLocatorSupplier != null, "artifact locator cannot be null.");
        JkUtilsAssert.state(moduleIdSupplier.get() != null, "moduleId cannot be null.");
        JkUtilsAssert.state(versionSupplier.get() != null, "version cannot be null.");

        List<Path> missingFiles = getArtifactLocator().getMissingFiles();
        JkUtilsAssert.argument(missingFiles.isEmpty(), "One or several files to publish do not exist : " + missingFiles);

        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos, null);
        JkCoordinate coordinate = getModuleId().toCoordinate(versionSupplier.get());
        internalPublisher.publishMaven(coordinate, getArtifactLocator(), pomMetadata, dependencySet);
        return this;
    }

    @Override
    public String toString() {
        return "JkMavenPublication{" +
                "artifactFileLocator=" + artifactLocatorSupplier +
                ", extraInfo=" + pomMetadata +
                '}';
    }

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

    /**
     * Shorthand to get the first declared publication repository.
     */
    public JkRepo findFirstNonLocalRepo() {
        return this.getPublishRepos().getRepos().stream()
                .filter(repo1 -> !repo1.isLocal())
                .findFirst()
                .orElse(null);
    }

    public String info() {
        StringBuilder builder = new StringBuilder();
        builder
                .append("\nPublish Maven repositories : ")
                .append(getPublishRepos()).append("\n")
                .append("Published Maven Module & version : ")
                .append(getModuleId().toCoordinate(getVersion()))
                .append("\n")
                .append("Published Maven Dependencies :");
        getDependencies().getEntries().forEach(dep -> builder.append("\n  ").append(dep));
        return builder.toString();
    }



}
