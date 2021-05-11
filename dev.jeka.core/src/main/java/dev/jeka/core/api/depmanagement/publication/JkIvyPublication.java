package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactProducer;
import dev.jeka.core.api.file.JkPathFile;
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
public final class JkIvyPublication<T> {

    public final T __;

    private JkModuleId moduleId;

    private Supplier<String> versionSupplier = () -> null;

    private JkRepoSet repos = JkRepoSet.of();

    private UnaryOperator<Path> defaultSigner;  // Can be null

    private Function<JkQualifiedDependencySet, JkQualifiedDependencySet> dependencies = UnaryOperator.identity();

    private Supplier<? extends JkArtifactLocator> artifactLocatorSupplier;

    private JkPublishedArtifact mainArtifact;

    private final Set<JkPublishedArtifact> extraArtifacts = new HashSet<>();

    private JkIvyPublication(T parent) {
        this.__ = parent;
    }

    public static <T> JkIvyPublication<T> of(T parent) {
        return new JkIvyPublication<>(parent);
    }

    public static JkIvyPublication<Void> of() {
        return new JkIvyPublication<>(null);
    }

    public JkIvyPublication<T> setModuleId(String moduleId) {
        this.moduleId = JkModuleId.of(moduleId);
        return this;
    }

    public JkIvyPublication<T> setVersion(Supplier<String> version) {
        this.versionSupplier = version;
        return this;
    }

    public JkIvyPublication<T> setVersion(String version) {
        this.versionSupplier = () -> version;
        return this;
    }

    public JkModuleId getModuleId() {
        return moduleId;
    }

    public String getVersion() {
        return versionSupplier.get();
    }

    public JkRepoSet getRepos() {
        return repos;
    }

    public JkIvyPublication<T> setRepos(JkRepoSet repos) {
        this.repos = repos;
        return this;
    }

    public JkIvyPublication<T> addRepos(JkRepo ...repoArgs) {
        Arrays.stream(repoArgs).forEach(repo -> repos = repos.and(repo));
        return this;
    }

    public JkIvyPublication<T> setDefaultSigner(UnaryOperator<Path> defaultSigner) {
        this.defaultSigner = defaultSigner;
        return this;
    }

    public JkIvyPublication<T> setDependencies(UnaryOperator<JkQualifiedDependencySet> modifier) {
        JkUtilsAssert.argument(modifier != null, "Dependency modifier cannot be null.");
        this.dependencies = dependencies.andThen(modifier);
        return this;
    }

    public JkIvyPublication<T> setDependencies(JkQualifiedDependencySet configuredDependencies) {
        return setDependencies(deps-> configuredDependencies);
    }

    public JkIvyPublication<T> setDependencies(JkDependencySet compile, JkDependencySet runtime, JkDependencySet test,
                                               JkVersionedModule.ConflictStrategy conflictStrategy) {
        return setDependencies(JkQualifiedDependencySet.computeIvyPublishDependencies(compile, runtime, test,
                conflictStrategy));
    }

    public JkIvyPublication<T> setDependencies(JkDependencySet compile, JkDependencySet runtime, JkDependencySet test) {
        return setDependencies(compile, runtime, test, JkVersionedModule.ConflictStrategy.FAIL);
    }

    public JkQualifiedDependencySet getDependencies() {
        return dependencies.apply(JkQualifiedDependencySet.of());
    }

    public JkIvyPublication<T> clear() {
        this.artifactLocatorSupplier = null;
        this.mainArtifact = null;
        this.extraArtifacts.clear();
        return this;
    }

    /**
     * Adds all the artifacts defined in the specified artifactLocator.
     */
    public JkIvyPublication<T> addArtifacts(Supplier<JkArtifactLocator> artifactLocator) {
        this.artifactLocatorSupplier = artifactLocator;
        return this;
    }

    /**
     * see {@link #addArtifacts(Supplier)}
     */
    public JkIvyPublication<T> addArtifacts(JkArtifactProducer artifactProducer) {
        return addArtifacts(() -> artifactProducer);
    }

    private static List<JkPublishedArtifact> toPublishedArtifacts(JkArtifactLocator artifactLocator) {
        List<JkPublishedArtifact> result = new LinkedList<>();
        result.add(toPublishedArtifact(null, artifactLocator.getMainArtifactPath(), null, "compile"));
        for (final JkArtifactId artifactId : artifactLocator.getArtifactIds()) {
            if (artifactId.isMainArtifact()) {
                continue;
            }
            final Path file = artifactLocator.getArtifactPath(artifactId);
            result.add(toPublishedArtifact(null, file, artifactId.getName(), configurationFor(artifactId.getName())));
        }
        return result;
    }

    /**
     * @see #addArtifact(String, Path, String, String...)
     */
    public JkIvyPublication<T> setMainArtifact(Path file, String... configurationNames) {
        return setMainArtifactWithType(file, null, configurationNames);
    }

    /**
     * @see #addArtifact(String, Path, String, String...)
     */
    public JkIvyPublication<T> setMainArtifactWithType(Path file, String type, String... configurationNames) {
        this.mainArtifact = toPublishedArtifact(null, file, type, configurationNames);
        return this;
    }

    /**
     * Adds the specified artifact to the publication.
     */
    public JkIvyPublication<T> addArtifact(String artifactName, Path artifactFile, String type, String... configurationNames) {
        extraArtifacts.add(new JkPublishedArtifact(artifactName, artifactFile, type,
                JkPathFile.of(artifactFile).getExtension(),
                Arrays.stream(configurationNames).collect(Collectors.toSet())));
        return this;
    }

    /**
     * Same as {@link #setMainArtifact(Path, String...)} (Path, String...)} but effective only if the specified file exists.
     */
    public JkIvyPublication<T> addOptionalArtifact(Path file, String... configurationNames) {
        if (Files.exists(file)) {
            return setMainArtifact(file, configurationNames);
        }
        return this;
    }

    /**
     * Same as {@link #setMainArtifact(Path, String...)} (Path, String, String...)} but effective only if the specified file
     * exists.
     */
    public JkIvyPublication<T> addOptionalArtifactWithType(Path file, String type, String... configurationNames) {
        if (Files.exists(file)) {
            return setMainArtifactWithType(file, type, configurationNames);
        }
        return this;
    }

    public List<JkPublishedArtifact> getAllArtifacts() {
        List<JkPublishedArtifact> result = new LinkedList<>();
        if (artifactLocatorSupplier != null) {
            result.addAll(toPublishedArtifacts(artifactLocatorSupplier.get()));
        }
        if (mainArtifact != null) {
            result.add(mainArtifact);
        }
        result.addAll(extraArtifacts);
        return result;
    }

    public void publish() {
        publish(repos);
    }

    public void publishLocal() {
        publish(JkRepo.ofLocalIvy().toSet());
    }

    private void publish(JkRepoSet repos) {
        JkUtilsAssert.state(moduleId != null, "moduleIId cannot be null.");
        JkUtilsAssert.state(versionSupplier.get() != null, "version cannot be null.");
        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos.withDefaultSigner(defaultSigner), null);
        internalPublisher.publishIvy(moduleId.withVersion(versionSupplier.get()), getAllArtifacts(), getDependencies());
    }

    private static JkPublishedArtifact toPublishedArtifact(String artifactName, Path artifactFile, String type,
                                                           String... configurationNames) {
        return new JkPublishedArtifact(artifactName, artifactFile, type, null,
                JkUtilsIterable.setOf(configurationNames).stream().collect(Collectors.toSet()));
    }

    public static class JkPublishedArtifact {

        private JkPublishedArtifact(String name, Path path, String type, String extension, Set<String> configurationNames) {
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

        public final String extension;  // if not null, override the implicit file extensiion

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

    public static JkQualifiedDependencySet getPublishDependencies(JkDependencySet compileDependencies,
                                                                  JkDependencySet runtimeDependencies,
                                                                  JkVersionedModule.ConflictStrategy strategy) {
        JkDependencySetMerge merge = compileDependencies.merge(runtimeDependencies);
        List<JkQualifiedDependency> result = new LinkedList<>();
        for (JkModuleDependency moduleDependency : merge.getResult().normalised(strategy)
                .assertNoUnspecifiedVersion().getVersionedModuleDependencies()) {
            String configuration = "compile->compile(*),master(*)";
            if (merge.getAbsentDependenciesFromRight().contains(moduleDependency)) {
               // compile only dependency
            } else if (merge.getAbsentDependenciesFromLeft().contains(moduleDependency)) {
                configuration = "runtime->runtime(*),master(*)";
            } else {
                configuration = configuration + ";runtime -> runtime(*),master(*)";
            }
            result.add(JkQualifiedDependency.of(configuration, moduleDependency));
        }
        return JkQualifiedDependencySet.of(result);

    }


}
