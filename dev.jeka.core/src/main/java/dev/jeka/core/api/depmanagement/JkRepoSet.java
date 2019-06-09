package dev.jeka.core.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import dev.jeka.core.api.utils.JkUtilsIterable;

/**
 * A set of {@link JkRepo}
 *
 * @author Jerome Angibaud
 */
public final class JkRepoSet implements Serializable {

    private static final long serialVersionUID = 1L;

    // Cached resolver
    private transient ModuleDepResolver ivyResolver;

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
    public static JkRepoSet ofOssrhSnapshotAndRelease(String userName, String password) {
        return of(JkRepo.ofMavenOssrhDownloadAndDeploySnapshot(userName, password),
                JkRepo.ofMavenOssrhDeployRelease(userName, password));
    }

    /**
     * Creates a JkRepoSet for downloading from <a href="http://central.sonatype.org/">OSSRH</a>
     */
    public static JkRepoSet ofOssrhSnapshotAndRelease() {
        return ofOssrhSnapshotAndRelease(null, null);
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

    public List<JkRepo> getRepoList() {
        return repos;
    }

    public boolean hasIvyRepo() {
        return this.repos.stream().anyMatch(JkRepo::isIvyRepo);
    }

    @Override
    public String toString() {
        return repos.toString();
    }

    /**
     * Retrieves directly the file embodying the specified the external dependency.
     */
    public Path get(JkModuleDependency moduleDependency) {
        final ModuleDepResolver depResolver = getIvyResolver();
        File file = depResolver.get(moduleDependency);
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

    private ModuleDepResolver getIvyResolver() {
        if (ivyResolver == null) {
            ivyResolver = InternalDepResolvers.ivy(this);
        }
        return ivyResolver;
    }

}
