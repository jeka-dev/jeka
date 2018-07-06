package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.utils.JkUtilsIterable;

/**
 * A set of {@link JkRepo}
 *
 * @author Jerome Angibaud
 */
public final class JkRepoSet implements Serializable {

    private static final long serialVersionUID = 1L;

    // Cached resolver
    private transient InternalDepResolver ivyResolver;

    private final List<JkRepo> repoConfigs;

    private JkRepoSet(List<JkRepo> repos) {
        super();
        this.repoConfigs = Collections.unmodifiableList(repos);
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
    public static JkRepoSet of(JkRepo... configs) {
        return new JkRepoSet(Arrays.asList(configs));
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
        final List<JkRepo> list = new LinkedList<>(this.repoConfigs);
        list.add(other);
        return new JkRepoSet(list);
    }

    /**
     * Returns a merge of this repository and the specified one.
     */
    public JkRepoSet and(JkRepoSet other) {
        final List<JkRepo> list = new LinkedList<>(this.repoConfigs);
        list.addAll(other.repoConfigs);
        return new JkRepoSet(list);
    }

    public static JkRepoSet local() {
        return of(JkRepo.local());
    }

    /**
     * Creates a JkPublishRepos tailored for <a
     * href="http://central.sonatype.org/">OSSRH</a>
     */
    public static JkRepoSet ossrhSnapshotAndRelease(String userName, String password) {
        return of(JkRepo.mavenOssrhDownloadAndDeploySnapshot(userName, password),
                JkRepo.mavenOssrhDeployRelease(userName, password));
    }

    /**
     * Crates an empty {@link JkRepoSet}
     */
    public static JkRepoSet empty() {
        return new JkRepoSet(Collections.emptyList());
    }

    /**
     * Return the individual repository from this set having the specified url.
     * Returns <code>null</code> if no such repository found.
     */
    public JkRepo getRepoConfigHavingUrl(String url) {
        for (final JkRepo config : this.repoConfigs) {
            if (url.equals(config.url().toExternalForm())) {
                return config;
            }
        }
        return null;
    }

    public List<JkRepo> list() {
        return repoConfigs;
    }

    @Override
    public String toString() {
        return repoConfigs.toString();
    }

    /**
     * Retrieves directly the file embodying the specified the external dependency.
     */
    public Path get(JkModuleDependency moduleDependency) {
        final InternalDepResolver depResolver = ivyResolver();
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

    private InternalDepResolver ivyResolver() {
        if (ivyResolver == null) {
            ivyResolver = InternalDepResolvers.ivy(this);
        }
        return ivyResolver;
    }

}
