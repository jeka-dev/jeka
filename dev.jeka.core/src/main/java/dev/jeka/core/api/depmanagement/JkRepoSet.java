package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.resolution.JkInternalDependencyResolver;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A set of {@link JkRepo}
 *
 * @author Jerome Angibaud
 */
public final class JkRepoSet {

    // Cached resolver
    private transient JkInternalDependencyResolver internalDependencyResolver;

    private final List<JkRepo> repos;

    private JkRepoSet(List<JkRepo> repos) {
        super();
        this.repos = Collections.unmodifiableList(repos);
    }

    /**
     * Creates a repository set from the specified configurations.
     */
    public static JkRepoSet of(Iterable<JkRepo> configs) {
        return new JkRepoSet(JkUtilsIterable.listOf(configs));
    }

    /**
     * Creates a repository set from the specified configurations.
     */
    public static JkRepoSet of(JkRepo repo, JkRepo... others) {
        return new JkRepoSet(JkUtilsIterable.listOf1orMore(repo, others));
    }

    /**
     * Crates a {@link JkRepoSet} from the specified {@link JkRepo}s
     */
    public static JkRepoSet of(String ... urls) {
        final List<JkRepo> list = new LinkedList<>();
        for (final String url : urls) {
            list.add(JkRepo.of(url));
        }
        return new JkRepoSet(list);
    }

    /**
     * Returns a repo identical to this one but with the extra specified repository.
     */
    public JkRepoSet and(JkRepo other) {
        final List<JkRepo> list = new LinkedList<>(this.repos);
        list.add(other);
        return new JkRepoSet(list);
    }

    /**
     * Returns a merge of this repository and the specified one.
     */
    public JkRepoSet and(JkRepoSet other) {
        final List<JkRepo> list = new LinkedList<>(this.repos);
        list.addAll(other.repos);
        return new JkRepoSet(list);
    }

    public static JkRepoSet ofLocal() {
        return of(JkRepo.ofLocal());
    }

    /**
     * Creates a JkRepoSet for publishing on <a href="http://central.sonatype.org/">OSSRH</a>
     */
    public static JkRepoSet ofOssrhSnapshotAndRelease(String userName, String password, UnaryOperator<Path> signer) {
        return of(JkRepo.ofMavenOssrhDownloadAndDeploySnapshot(userName, password),
                JkRepo.ofMavenOssrhDeployRelease(userName, password, signer));
    }

    /**
     * Return the individual repository from this set having the specified url.
     * Returns <code>null</code> if no such repository found.
     */
    public JkRepo getRepoConfigHavingUrl(String url) {
        for (final JkRepo config : this.repos) {
            if (url.equals(config.getUrl().toExternalForm())) {
                return config;
            }
        }
        return null;
    }

    public List<JkRepo> getRepos() {
        return repos;
    }

    public boolean contains(URL url) {
        return this.repos.stream().anyMatch(repo -> url.equals(repo.getUrl()));
    }

    @Override
    public String toString() {
        return repos.toString();
    }

    /**
     * Retrieves directly the file embodying the specified the external dependency.
     */
    public Path get(JkModuleDependency moduleDependency) {
        final JkInternalDependencyResolver depResolver = getInternalDependencyResolver();
        final File file = depResolver.get(moduleDependency);
        if (file == null) {
            return null;
        }
        return depResolver.get(moduleDependency).toPath();
    }

    /**
     * Short hand for {@link #get(JkModuleDependency)}
     */
    public Path get(JkModuleId moduleId, String version) {
        return get(JkModuleDependency.of(moduleId, version));
    }

    /**
     * Short hand for {@link #get(JkModuleDependency)}
     */
    public Path get(String moduleGroupVersion) {
        return get(JkModuleDependency.of(moduleGroupVersion));
    }

    private JkInternalDependencyResolver getInternalDependencyResolver() {
        if (internalDependencyResolver == null) {
            internalDependencyResolver = JkInternalDependencyResolver.of(this);
        }
        return internalDependencyResolver;
    }

}
