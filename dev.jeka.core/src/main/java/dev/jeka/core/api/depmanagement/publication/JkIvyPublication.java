package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactProducer;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.File;
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

    private Function<JkQualifiedDependencies, JkQualifiedDependencies> dependencies = UnaryOperator.identity();

    private JkIvyConfigurationMappingSet configurationMapping = JkIvyConfigurationMappingSet.RESOLVE_MAPPING;

    private Supplier<? extends JkArtifactLocator> artifactLocatorSupplier;

    private JkPublicationArtifact mainArtifact;

    private final Set<JkPublicationArtifact> extraArtifacts = new HashSet<>();

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

    public JkIvyPublication<T> setDependencies(UnaryOperator<JkQualifiedDependencies> modifier) {
        JkUtilsAssert.argument(modifier != null, "Dependency modifier cannot be null.");
        this.dependencies = dependencies.andThen(modifier);
        return this;
    }

    public JkIvyPublication<T> setDependencies(JkQualifiedDependencies configuredDependencies) {
        return setDependencies(deps-> configuredDependencies);
    }

    public JkIvyPublication<T> setDependencies(JkDependencySet compile, JkDependencySet runtime, JkDependencySet test,
                                               JkVersionedModule.ConflictStrategy conflictStrategy) {
        return setDependencies(JkQualifiedDependencies.computeIvyPublishDependencies(compile, runtime, test,
                conflictStrategy));
    }

    public JkIvyPublication<T> setDependencies(JkDependencySet compile, JkDependencySet runtime, JkDependencySet test) {
        return setDependencies(compile, runtime, test, JkVersionedModule.ConflictStrategy.FAIL);
    }

    public JkQualifiedDependencies getDependencies() {
        return dependencies.apply(JkQualifiedDependencies.of());
    }

    public JkIvyPublication<T> setConfigurationMapping(JkIvyConfigurationMappingSet configurationMapping) {
        this.configurationMapping = configurationMapping;
        return this;
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

    private static List<JkIvyPublication.JkPublicationArtifact> toArtifacts(JkArtifactLocator artifactLocator) {
        List<JkIvyPublication.JkPublicationArtifact> result = new LinkedList<>();
        result.add(toPublication(null, artifactLocator.getMainArtifactPath(), null, "compile"));
        for (final JkArtifactId artifactId : artifactLocator.getArtifactIds()) {
            if (artifactId.isMainArtifact()) {
                continue;
            }
            final Path file = artifactLocator.getArtifactPath(artifactId);
            result.add(toPublication(artifactId.getName(), file, null, configurationFor(artifactId.getName())));
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
        this.mainArtifact = toPublication(null, file, type, configurationNames);
        return this;
    }

    /**
     * Adds the specified artifact to the publication.
     */
    public JkIvyPublication<T> addArtifact(String artifactName, Path artifactFile, String type, String... configurationNames) {
        extraArtifacts.add(new JkPublicationArtifact(artifactName, artifactFile, type,
                JkUtilsIterable.setOf(configurationNames).stream().collect(Collectors.toSet())));
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

    public List<JkPublicationArtifact> getAllArtifacts() {
        List<JkPublicationArtifact> result = new LinkedList<>();
        if (artifactLocatorSupplier != null) {
            result.addAll(toArtifacts(artifactLocatorSupplier.get()));
        }
        if (mainArtifact != null) {
            result.add(mainArtifact);
        }
        result.addAll(extraArtifacts);
        return result;
    }

    public void publish() {
        JkUtilsAssert.state(moduleId != null, "moduleIId cannot be null.");
        JkUtilsAssert.state(versionSupplier.get() != null, "version cannot be null.");
        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos.withDefaultSigner(defaultSigner), null);
        internalPublisher.publishIvy(moduleId.withVersion(versionSupplier.get()), this, getDependencies());
    }

    private static JkPublicationArtifact toPublication(String artifactName, Path artifactFile, String type,
                                                       String... configurationNames) {
        return new JkPublicationArtifact(artifactName, artifactFile, type,
                JkUtilsIterable.setOf(configurationNames).stream().collect(Collectors.toSet()));
    }

    public static class JkPublicationArtifact {

        private JkPublicationArtifact(String name, Path path, String type, Set<String> configurationNames) {
            super();
            this.file = path.toFile();
            this.extension = path.getFileName().toString().contains(".") ? JkUtilsString.substringAfterLast(
                    path.getFileName().toString(), ".") : null;
                    this.type = type;
                    this.configurationNames = configurationNames;
                    this.name = name;
        }

        public final File file;  // path not serializable

        public final String type;

        public final Set<String> configurationNames;

        public final String name;

        public final String extension;

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

    public static JkQualifiedDependencies getPublishDependencies(JkDependencySet compileDependencies,
                                                          JkDependencySet runtimeDependencies,
                                                          JkVersionedModule.ConflictStrategy strategy) {
        JkDependencySetMerge dependencySetMerge = compileDependencies.merge(runtimeDependencies);
        return JkQualifiedDependencies.of(); // TODO

    }


}
