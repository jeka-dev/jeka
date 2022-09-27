package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkCoordinate.GroupAndName;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
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
public final class JkMavenPublication<T> {

    public final T __; // For parent chaining

    private final JkPomMetadata<JkMavenPublication<T>> pomMetadata = JkPomMetadata.ofParent(this);

    private Function<JkDependencySet, JkDependencySet> dependencies = UnaryOperator.identity();

    private Supplier<GroupAndName> groupAndNameSupplier = () -> null;

    private Supplier<JkVersion> versionSupplier = () -> JkVersion.UNSPECIFIED;

    private Supplier<JkArtifactLocator> artifactLocatorSupplier;

    private JkRepoSet publishRepos = JkRepoSet.ofLocal();

    private Supplier<JkRepoSet> bomResolverRepoSupplier = () -> JkRepoSet.of();

    private UnaryOperator<Path> defaultSigner;  // Can be null. Signer used if none is defined on repos

    private JkMavenPublication(T parent) {
        this.__ = parent;
    }

    public static <T> JkMavenPublication<T> of(T parent) {
        return new JkMavenPublication(parent);
    }

    public static <T> JkMavenPublication<Void> of() {
        return new JkMavenPublication(null);
    }

    public JkPomMetadata<JkMavenPublication<T>> getPomMetadata() {
        return this.pomMetadata;
    }

    public JkMavenPublication<T> configureDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependencies = dependencies.andThen(modifier);
        return this;
    }

    public JkDependencySet getDependencies() {
        return dependencies.apply(JkDependencySet.of());
    }

    public JkMavenPublication<T> setModuleId(String moduleId) {
        this.groupAndNameSupplier = () -> GroupAndName.of(moduleId);
        return this;
    }

    public JkMavenPublication<T> setModuleId(Supplier<String> moduleIdSupplier) {
        this.groupAndNameSupplier = () -> GroupAndName.of(moduleIdSupplier.get());
        return this;
    }

    public JkMavenPublication<T> setVersion(String version) {
        this.versionSupplier = () -> JkVersion.of(version);
        return this;
    }

    public JkMavenPublication<T> setVersion(Supplier<String> versionSupplier) {
        this.versionSupplier = () -> JkVersion.of(versionSupplier.get());
        return this;
    }

    public JkMavenPublication<T> setBomResolutionRepos(Supplier<JkRepoSet> repoSupplier) {
        this.bomResolverRepoSupplier = repoSupplier;
        return this;
    }

    public GroupAndName getGroupAndName() {
        return groupAndNameSupplier.get();
    }

    public JkVersion getVersion() {
        return versionSupplier.get();
    }

    public UnaryOperator<Path> getDefaultSigner() {
        return defaultSigner;
    }

    public JkMavenPublication<T> setDefaultSigner(UnaryOperator<Path> defaultSigner) {
        this.defaultSigner = defaultSigner;
        return this;
    }

    public JkArtifactLocator getArtifactLocator() {
        return artifactLocatorSupplier.get();
    }

    public JkMavenPublication<T> setArtifactLocatorSupplier(Supplier<JkArtifactLocator> artifactLocatorSupplier) {
        this.artifactLocatorSupplier = artifactLocatorSupplier;
        return this;
    }

    public JkMavenPublication<T> setArtifactLocator(JkArtifactLocator artifactLocatorArg) {
        this.artifactLocatorSupplier = () -> artifactLocatorArg;
        return this;
    }

    public JkRepoSet getPublishRepos() {
        return publishRepos;
    }

    public JkMavenPublication<T> setPublishRepos(JkRepoSet repoSet) {
        this.publishRepos = repoSet;
        return this;
    }

    public JkMavenPublication<T> addRepos(JkRepo ...repoArgs) {
        Arrays.stream(repoArgs).forEach(repo -> publishRepos = publishRepos.and(repo));
        return this;
    }

    /**
     * Publishes this publication to its defined repositories
     */
    public JkMavenPublication<T> publish() {
        publish(this.publishRepos.withDefaultSigner(defaultSigner));
        return this;
    }

    /**
     * Publishes this publication on the local repository
     */
    public JkMavenPublication<T> publishLocal() {
        publish(JkRepoSet.ofLocal());
        return this;
    }


    private JkMavenPublication publish(JkRepoSet repos) {
        JkRepoSet bomRepos = this.bomResolverRepoSupplier.get().and(repos);
        JkDependencySet dependencySet = this.getDependencies()
                .withResolvedBoms(bomRepos)
                .assertNoUnspecifiedVersion()
                .toResolvedModuleVersions();
        JkUtilsAssert.state(artifactLocatorSupplier != null, "artifact locator cannot be null.");
        JkUtilsAssert.state(groupAndNameSupplier.get() != null, "moduleId cannot be null.");
        JkUtilsAssert.state(versionSupplier.get() != null, "version cannot be null.");
        List<Path> missingFiles = getArtifactLocator().getMissingFiles();
        JkUtilsAssert.argument(missingFiles.isEmpty(), "One or several files to publish do not exist : " + missingFiles);
        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos, null);
        JkCoordinate coordinate = getGroupAndName().toCoordinate(versionSupplier.get());
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



}
