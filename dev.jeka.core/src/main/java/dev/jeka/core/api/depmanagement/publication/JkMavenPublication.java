package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;
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

    private Supplier<JkVersionedModule> versionedModule;

    private Supplier<JkArtifactLocator> artifactLocator;

    private JkRepoSet repoSet;

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

    public JkVersionedModule getVersionedModule() {
        return versionedModule.get();
    }

    public JkMavenPublication<T> setVersionedModule(Supplier<JkVersionedModule> versionedModule) {
        this.versionedModule = versionedModule;
        return this;
    }

    public JkMavenPublication<T> setVersionedModule(JkVersionedModule versionedModuleArg) {
        this.versionedModule = () -> versionedModuleArg;
        return this;
    }

    public JkArtifactLocator getArtifactLocator() {
        return artifactLocator.get();
    }

    public JkMavenPublication<T> setArtifactLocator(Supplier<JkArtifactLocator> artifactLocator) {
        this.artifactLocator = artifactLocator;
        return this;
    }

    public JkMavenPublication<T> setArtifactLocator(JkArtifactLocator artifactLocatorArg) {
        this.artifactLocator = () -> artifactLocatorArg;
        return this;
    }

    /**
     * Publishes the specified publication on the Maven repositories of this publisher.
     *
     * @param signer can be null.
     */
    public JkMavenPublication publish(JkRepoSet repos, UnaryOperator<Path> signer) {
        JkUtilsAssert.state(artifactLocator != null, "artifact locator cannot be null.");
        JkUtilsAssert.state(versionedModule != null, "versioned module cannot be null.");
        List<Path> missingFiles = getArtifactLocator().getMissingFiles();
        JkUtilsAssert.argument(missingFiles.isEmpty(), "One or several files to publish do not exist : " + missingFiles);
        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos, null);
        internalPublisher.publishMaven(getVersionedModule(), this, getDependencies(), signer);
        return this;
    }

    @Override
    public String toString() {
        return "JkMavenPublication{" +
                "artifactFileLocator=" + artifactLocator +
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
            JkTransitivity transitivity = JkTransitivity.RUNTIME;
            if (merge.getAbsentDependenciesFromLeft().contains(moduleDependency)) {
                transitivity = JkTransitivity.COMPILE;
            }
            result.add(moduleDependency.withTransitivity(transitivity));
        }
        return JkDependencySet.of(result);
    }



}
