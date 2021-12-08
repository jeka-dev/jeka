package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.*;
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
 * These information contains : <ul>
 *   <li>The artifacts to be published (main artifact and artifacts with classifiers)</li>
 *   <li>Information about describing the project as some public repositories require</li>
 * </ul>
 */
public final class JkMavenPublication<T> {

    public final T __; // For parent chaining

    private final JkPomMetadata<JkMavenPublication<T>> pomMetadata = JkPomMetadata.ofParent(this);

    private Function<JkDependencySet, JkDependencySet> dependencies = UnaryOperator.identity();

    private Supplier<String> moduleIdSupplier;

    private Supplier<String> versionSupplier = () -> null;

    private Supplier<JkArtifactLocator> artifactLocatorSupplier;

    private JkRepoSet repos = JkRepoSet.ofLocal();

    private UnaryOperator<Path> defaultSigner;  // Can be null

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

    public JkMavenPublication<T> setDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependencies = dependencies.andThen(modifier);
        return this;
    }

    public JkDependencySet getDependencies() {
        return dependencies.apply(JkDependencySet.of());
    }

    public JkMavenPublication<T> setModuleId(String moduleId) {
        this.moduleIdSupplier = () -> moduleId;
        return this;
    }

    public JkMavenPublication<T> setModuleId(Supplier<String> moduleIdSupplier) {
        this.moduleIdSupplier = moduleIdSupplier;
        return this;
    }

    public JkMavenPublication<T> setVersion(Supplier<String> version) {
        this.versionSupplier = version;
        return this;
    }

    public JkMavenPublication<T> setVersion(JkVersion version) {
        return setVersion(version.getValue());
    }

    public JkMavenPublication<T> setVersion(String version) {
        this.versionSupplier = () -> version;
        return this;
    }

    public JkModuleId getModuleId() {
        return JkModuleId.of(moduleIdSupplier.get());
    }

    public String getVersion() {
        return versionSupplier.get();
    }

    public UnaryOperator<Path> getDefaultSigner() {
        return defaultSigner;
    }

    public JkMavenPublication<T> setDefaultSigner(UnaryOperator<Path> signer) {
        this.defaultSigner = signer;
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

    public JkRepoSet getRepos() {
        return repos;
    }

    public JkMavenPublication<T> setRepos(JkRepoSet repoSet) {
        this.repos = repoSet;
        return this;
    }

    public JkMavenPublication<T> addRepos(JkRepo ...repoArgs) {
        Arrays.stream(repoArgs).forEach(repo -> repos = repos.and(repo));
        return this;
    }

    /**
     * Publishes this publication to its defined repositories
     */
    public JkMavenPublication<T> publish() {
        publish(this.repos.withDefaultSigner(defaultSigner));
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
        JkUtilsAssert.state(artifactLocatorSupplier != null, "artifact locator cannot be null.");
        JkUtilsAssert.state(moduleIdSupplier.get() != null, "moduleId cannot be null.");
        JkUtilsAssert.state(versionSupplier.get() != null, "version cannot be null.");
        List<Path> missingFiles = getArtifactLocator().getMissingFiles();
        JkUtilsAssert.argument(missingFiles.isEmpty(), "One or several files to publish do not exist : " + missingFiles);
        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos, null);
        JkVersionedModule versionedModule = getModuleId().withVersion(versionSupplier.get());
        internalPublisher.publishMaven(versionedModule, getArtifactLocator(), pomMetadata, getDependencies());
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
                                                                          JkVersionedModule.ConflictStrategy strategy) {
        JkDependencySetMerge merge = runtimeDeps.merge(compileDeps);
        List<JkDependency> result = new LinkedList<>();
        for (JkModuleDependency moduleDependency : merge.getResult().normalised(strategy)
                .assertNoUnspecifiedVersion().getVersionedModuleDependencies()) {
            JkTransitivity transitivity = JkTransitivity.COMPILE;
            if (merge.getAbsentDependenciesFromRight().contains(moduleDependency)) {
                transitivity = JkTransitivity.RUNTIME;
            }
            result.add(moduleDependency.withTransitivity(transitivity));
        }
        return JkDependencySet.of(result);
    }



}
