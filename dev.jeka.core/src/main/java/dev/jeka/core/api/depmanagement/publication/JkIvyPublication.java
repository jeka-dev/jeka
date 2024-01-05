package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Information required to publish a module in an Ivy repository.
 *
 * @author Jerome Angibaud.
 */
public final class JkIvyPublication {

    private Supplier<JkModuleId> moduleIdSupplier = () -> null;

    private Supplier<JkVersion> versionSupplier = () -> JkVersion.UNSPECIFIED;

    private JkRepoSet repos = JkRepoSet.of();

    private UnaryOperator<Path> defaultSigner;  // Can be null. Signer used if none is defined on repos

    private Function<JkQualifiedDependencySet, JkQualifiedDependencySet> dependencies = UnaryOperator.identity();

    private JkIvyPublishedArtifact mainArtifact;

    private final Set<JkIvyPublishedArtifact> extraArtifacts = new HashSet<>();

    private Supplier<JkRepoSet> bomResolverRepoSupplier = () -> JkRepoSet.of();

    public final JkRunnables preActions = JkRunnables.of();

    public final JkRunnables postActions = JkRunnables.of();

    private JkIvyPublication() {
    }

    /**
     * Creates a {@link JkIvyPublication}.
     */
    public static JkIvyPublication of() {
        return new JkIvyPublication();
    }

    /**
     * Configure the dependencies that will be exported with the published module.<br/>
     * By default, JeKa computes it from the compile and runtime dependencies.
     * This method allows to customize these dependencies by adding/removing or changing their transitivity.
     */
    public JkIvyPublication configureDependencies(UnaryOperator<JkQualifiedDependencySet> modifier) {
        JkUtilsAssert.argument(modifier != null, "Dependency modifier cannot be null.");
        this.dependencies = dependencies.andThen(modifier);
        return this;
    }

    /**
     * Sets transitive dependencies for this publication.
     */
    public JkIvyPublication setDependencies(JkQualifiedDependencySet configuredDependencies) {
        return configureDependencies(deps-> configuredDependencies);
    }

    /**
     * Sets transitive dependencies for this publication.
     */
    public JkIvyPublication setDependencies(JkDependencySet allCompileDeps,
                                            JkDependencySet allRuntimeDeps,
                                            JkDependencySet allTestDeps,
                                            JkCoordinate.ConflictStrategy conflictStrategy) {
        return setDependencies(JkQualifiedDependencySet.computeIvyPublishDependencies(
                allCompileDeps, allRuntimeDeps, allTestDeps, conflictStrategy));
    }

    /**
     * Sets transitive dependencies for this publication.
     */
    public JkIvyPublication setDependencies(JkDependencySet allCompileDeps,
                                            JkDependencySet allRuntimeDeps,
                                            JkDependencySet allTestDeps) {
        return setDependencies(allCompileDeps, allRuntimeDeps, allTestDeps, JkCoordinate.ConflictStrategy.FAIL);
    }

    /**
     * Returns the transitive dependencies for this publication
     */
    public JkQualifiedDependencySet getDependencies() {
        return dependencies.apply(JkQualifiedDependencySet.of());
    }

    /**
     * Sets the supplier providing the moduleId (group + artifactName) for this publication.
     */
    public JkIvyPublication setModuleIdSupplier(Supplier<JkModuleId> moduleIdSupplier) {
        this.moduleIdSupplier = moduleIdSupplier;
        return this;
    }

    /**
     * Sets the moduleId (group + artifactName) for this publication.
     * @see #setModuleIdSupplier(Supplier)
     */
    public JkIvyPublication setModuleId(String moduleId) {
        return setModuleIdSupplier( () -> JkModuleId.of(moduleId) );
    }

    /**
     * Sets the supplier providing the version of the artifacts to publish.
     */
    public JkIvyPublication setVersionSupplier(Supplier<JkVersion> versionSupplier) {
        this.versionSupplier = versionSupplier;
        return this;
    }

    /**
     * Sets the version of the artifacts to publish.
     * @see #setVersionSupplier(Supplier)
     */
    public JkIvyPublication setVersion(String version) {
        return setVersionSupplier(() -> JkVersion.of(version));
    }

    /**
     * Sets the supplier providing the download repositories used to resolve BOMs.
     */
    public JkIvyPublication setBomResolutionRepos(Supplier<JkRepoSet> repoSupplier) {
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
    public UnaryOperator<Path> getDefaultSigner() {
        return defaultSigner;
    }

    /**
     * Sets the default file signer to use for this publication.
     * @see #getDefaultSigner()
     */
    public JkIvyPublication setDefaultSigner(UnaryOperator<Path> defaultSigner) {
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
    public JkIvyPublication setRepos(JkRepoSet repos) {
        this.repos = repos;
        return this;
    }

    /**
     * Adds the specified repositories to the publication repositories.
     * @see #setRepos(JkRepoSet)
     */
    public JkIvyPublication addRepos(JkRepo ...repoArgs) {
        Arrays.stream(repoArgs).forEach(repo -> repos = repos.and(repo));
        return this;
    }

    /**
     * @see #putArtifactWithType(String, Path, String, String...)
     */
    public JkIvyPublication putMainArtifact(Path file, String... configurationNames) {
        return putMainArtifactWithType(file, null, configurationNames);
    }

    /**
     * @see #putArtifactWithType(String, Path, String, String...)
     */
    public JkIvyPublication putMainArtifactWithType(Path file, String type, String... configurationNames) {
        this.mainArtifact = toPublishedArtifact(null, file, type, configurationNames);
        return this;
    }

    /**
     * Adds the specified artifact to the publication.
     */
    public JkIvyPublication putArtifactWithType(String artifactName, Path artifactFile, String type, String... configurationNames) {
        extraArtifacts.add(new JkIvyPublishedArtifact(artifactName, artifactFile, type,
                JkPathFile.of(artifactFile).getExtension(),
                Arrays.stream(configurationNames).collect(Collectors.toSet())));
        return this;
    }

    /**
     * Adds the specified artifact to the publication.
     */
    public JkIvyPublication putArtifact(String artifactName, Path artifactFile, String... configurationNames) {
        return putArtifactWithType(artifactName, artifactFile, null, configurationNames);
    }

    /**
     * Same as {@link #putMainArtifact(Path, String...)} (Path, String...) but effective only if the specified file exists.
     */
    public JkIvyPublication putOptionalArtifact(Path file, String... configurationNames) {
        if (Files.exists(file)) {
            return putMainArtifact(file, configurationNames);
        }
        return this;
    }

    /**
     * Same as {@link #putMainArtifact(Path, String...)} (Path, String, String...) but effective only if the specified file
     * exists.
     */
    public JkIvyPublication putOptionalArtifactWithType(Path file, String type, String... configurationNames) {
        if (Files.exists(file)) {
            return putMainArtifactWithType(file, type, configurationNames);
        }
        return this;
    }


    public List<JkIvyPublishedArtifact> getAllArtifacts() {
        List<JkIvyPublishedArtifact> result = new LinkedList<>();
        if (mainArtifact != null) {
            result.add(mainArtifact);
        }
        result.addAll(extraArtifacts);
        return result;
    }

    public JkIvyPublication publish() {
        preActions.run();
        publish(repos);
        postActions.run();
        return this;
    }

    public JkIvyPublication publishLocal() {
        preActions.run();
        publish(JkRepo.ofLocalIvy().toSet());
        postActions.run();
        return this;
    }

    /**
     * Shorthand to get the first declared publication repository.
     */
    public JkRepo findFirstNonLocalRepo() {
        return getRepos().getRepos().stream()
                    .filter(repo1 -> !repo1.isLocal())
                    .findFirst().orElse(null);
    }

    public String info() {
        StringBuilder builder = new StringBuilder();
        builder
                .append("\nPublish Ivy repositories : " + getRepos() + "\n")
                .append("Published Ivy Module & version : " +
                        getModuleId().toCoordinate(getVersion()) + "\n")
                .append("Published Ivy Dependencies :");
        getDependencies().getEntries().forEach(dep -> builder.append("\n  " + dep));
        return builder.toString();
    }

    public static JkQualifiedDependencySet getPublishDependencies(JkDependencySet compileDependencies,
                                                                  JkDependencySet runtimeDependencies,
                                                                  JkCoordinate.ConflictStrategy strategy) {
        JkDependencySetMerge merge = compileDependencies.merge(runtimeDependencies);
        List<JkQualifiedDependency> result = new LinkedList<>();
        for (JkCoordinateDependency coordinateDependency : merge.getResult().normalised(strategy)
                .assertNoUnspecifiedVersion().getVersionResolvedCoordinateDependencies()) {
            String configuration = "compile->compile(*),master(*)";
            if (merge.getAbsentDependenciesFromRight().contains(coordinateDependency)) {
                // compile only dependency
            } else if (merge.getAbsentDependenciesFromLeft().contains(coordinateDependency)) {
                configuration = "runtime->runtime(*),master(*)";
            } else {
                configuration = configuration + ";runtime -> runtime(*),master(*)";
            }
            result.add(JkQualifiedDependency.of(configuration, coordinateDependency));
        }
        return JkQualifiedDependencySet.of(result);
    }

    private static String configurationFor(String classifier) {
        if ("sources".equals(classifier)) {
            return "sources";
        }
        if ("test".equals(classifier)) {
            return "test";
        }
        if ("test-sources".equals(classifier)) {
            return "test-sources";
        }
        if ("javadoc".equals(classifier)) {
            return "javadoc";
        }
        return classifier;
    }

    private void publish(JkRepoSet repos) {
        JkUtilsAssert.state(moduleIdSupplier.get() != null, "moduleId cannot be null.");
        JkUtilsAssert.state(versionSupplier.get() != null, "version cannot be null.");
        JkRepoSet bomRepos = this.bomResolverRepoSupplier.get().and(repos);
        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos.withDefaultSigner(defaultSigner),
                null);
        internalPublisher.publishIvy(getModuleId().toCoordinate(versionSupplier.get()), getAllArtifacts(),
                getDependencies().withResolvedBoms(bomRepos));
    }

    private static JkIvyPublishedArtifact toPublishedArtifact(String artifactName, Path artifactFile, String type,
                                                              String... configurationNames) {
        return new JkIvyPublishedArtifact(artifactName, artifactFile, type, null,
                JkUtilsIterable.setOf(configurationNames).stream().collect(Collectors.toSet()));
    }

    private static List<JkIvyPublishedArtifact> toIvyPublishedArtifacts(JkArtifactPublisher artifactProducer) {
        List<JkIvyPublishedArtifact> result = new LinkedList<>();
        result.add(toPublishedArtifact(null, artifactProducer.artifactLocator.getMainArtifactPath(), null, "compile"));
        for (final JkArtifactId artifactId : artifactProducer.getArtifactIds()) {
            if (artifactId.isMainArtifact()) {
                continue;
            }
            final Path file = artifactProducer.artifactLocator.getArtifactPath(artifactId);
            result.add(toPublishedArtifact(null, file, artifactId.getClassifier(), configurationFor(artifactId.getClassifier())));
        }
        return result;
    }

    public static class JkIvyPublishedArtifact {

        private JkIvyPublishedArtifact(String name, Path path, String type, String extension, Set<String> configurationNames) {
            super();
            this.file = path;
            this.extension = extension;
            this.type = type;
            this.configurationNames = configurationNames;
            this.name = name;
        }

        public final Path file;  // path not serializable

        public final String type;

        public final Set<String> configurationNames;

        public final String name;

        public final String extension;  // if not null, override the implicit file extension

    }

}
