package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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

    private Supplier<JkVersionedModule> versionedModule;

    private Function<JkDependencySet, JkDependencySet> dependencies = UnaryOperator.identity();

    private Supplier<JkVersionProvider> resolvedVersionProvider = () -> JkVersionProvider.of();

    private JkScopeMapping scopeMapping = JkScope.DEFAULT_SCOPE_MAPPING;

    private Supplier<? extends JkArtifactLocator> artifactLocator;

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

    public JkIvyPublication<T> setVersionedModule(Supplier<JkVersionedModule> versionedModule) {
        JkUtilsAssert.argument(versionedModule != null, "VersionedModule supplier cannot be null.");
        this.versionedModule = versionedModule;
        return this;
    }

    public JkIvyPublication<T> setVersionedModule(JkVersionedModule versionedModule) {
        return setVersionedModule(() -> versionedModule);
    }

    public JkIvyPublication<T> setDependencies(UnaryOperator<JkDependencySet> modifier) {
        JkUtilsAssert.argument(modifier != null, "Dependency modifier cannot be null.");
        this.dependencies = dependencies.andThen(modifier);
        return this;
    }

    public JkDependencySet getDependencies() {
        return dependencies.apply(JkDependencySet.of());
    }

    public JkIvyPublication<T> setResolvedVersionProvider(Supplier<JkVersionProvider> resolvedVersionProvider) {
        this.resolvedVersionProvider = resolvedVersionProvider;
        return this;
    }

    public JkIvyPublication<T> setScopeMapping(JkScopeMapping scopeMapping) {
        this.scopeMapping = scopeMapping;
        return this;
    }

    public JkIvyPublication<T> clear() {
        this.artifactLocator = null;
        this.mainArtifact = null;
        this.extraArtifacts.clear();
        return this;
    }

    /**
     * Adds all the artifacts defined in the specified artifactLocator.
     */
    public JkIvyPublication<T> addArtifacts(Supplier<JkArtifactLocator> artifactLocator) {
        this.artifactLocator = artifactLocator;
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
        result.add(toPublication(null, artifactLocator.getMainArtifactPath(), null, JkScope.COMPILE.getName()));
        for (final JkArtifactId artifactId : artifactLocator.getArtifactIds()) {
            if (artifactId.isMainArtifact()) {
                continue;
            }
            final Path file = artifactLocator.getArtifactPath(artifactId);
            result.add(toPublication(artifactId.getName(), file, null, scopeFor(artifactId.getName())));
        }
        return result;
    }

    /**
     * @see #addArtifact(String, Path, String, String...)
     */
    public JkIvyPublication<T> setMainArtifact(Path file, String... scopes) {
        return setMainArtifactWithType(file, null, scopes);
    }

    /**
     * @see #addArtifact(String, Path, String, String...)
     */
    public JkIvyPublication<T> setMainArtifactWithType(Path file, String type, String... scopes) {
        this.mainArtifact = toPublication(null, file, type, scopes);
        return this;
    }

    /**
     * Adds the specified artifact to the publication.
     */
    public JkIvyPublication<T> addArtifact(String artifactName, Path artifactFile, String type, String... scopes) {
        extraArtifacts.add(new JkPublicationArtifact(artifactName, artifactFile, type,
                JkUtilsIterable.setOf(scopes).stream().map(JkScope::of).collect(Collectors.toSet())));
        return this;
    }

    /**
     * Same as {@link #setMainArtifact(Path, String...)} (Path, String...)} but effective only if the specified file exists.
     */
    public JkIvyPublication<T> addOptionalArtifact(Path file, String... scopes) {
        if (Files.exists(file)) {
            return setMainArtifact(file, scopes);
        }
        return this;
    }

    /**
     * Same as {@link #setMainArtifact(Path, String...)} (Path, String, String...)} but effective only if the specified file
     * exists.
     */
    public JkIvyPublication<T> addOptionalArtifactWithType(Path file, String type, String... scopes) {
        if (Files.exists(file)) {
            return setMainArtifactWithType(file, type, scopes);
        }
        return this;
    }

    public List<JkPublicationArtifact> getAllArtifacts() {
        List<JkPublicationArtifact> result = new LinkedList<>();
        if (artifactLocator != null) {
            result.addAll(toArtifacts(artifactLocator.get()));
        }
        if (mainArtifact != null) {
            result.add(mainArtifact);
        }
        result.addAll(extraArtifacts);
        return result;
    }

    public void publish(JkRepoSet repos) {
        if (!repos.hasIvyRepo()) {
            return;
        }
        JkUtilsAssert.state(versionedModule != null, "Versioned module provider cannot be null.");
        JkInternalPublisher internalPublisher = JkInternalPublisher.of(repos, null);
        internalPublisher.publishIvy(versionedModule.get(), this, getDependencies(), scopeMapping,
                Instant.now(), resolvedVersionProvider.get());
    }

    private static JkPublicationArtifact toPublication(String artifactName, Path artifactFile, String type, String... scopes) {
        return new JkPublicationArtifact(artifactName, artifactFile, type,
                JkUtilsIterable.setOf(scopes).stream().map(JkScope::of).collect(Collectors.toSet()));
    }

    public static class JkPublicationArtifact {

        private JkPublicationArtifact(String name, Path path, String type, Set<JkScope> jkScopes) {
            super();
            this.file = path.toFile();
            this.extension = path.getFileName().toString().contains(".") ? JkUtilsString.substringAfterLast(
                    path.getFileName().toString(), ".") : null;
                    this.type = type;
                    this.jkScopes = jkScopes;
                    this.name = name;
        }

        public final File file;  // path not serializable

        public final String type;

        public final Set<JkScope> jkScopes;

        public final String name;

        public final String extension;

    }

    private static String scopeFor(String classifier) {
        if ("sources".equals(classifier)) {
            return JkScope.SOURCES.getName();
        }
        if ("test".equals(classifier)) {
            return JkScope.TEST.getName();
        }
        if ("test-sources".equals(classifier)) {
            return JkScope.SOURCES.getName();
        }
        if ("javadoc".equals(classifier)) {
            return JkScope.JAVADOC.getName();
        }
        return classifier;
    }


}
