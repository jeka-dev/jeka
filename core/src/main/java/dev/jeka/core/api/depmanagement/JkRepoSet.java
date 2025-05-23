/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.depmanagement.resolution.JkInternalDependencyResolver;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
        List<JkRepo> distinctRepos = list.stream().distinct().collect(Collectors.toList());
        return new JkRepoSet(distinctRepos);
    }

    public static JkRepoSet ofLocal() {
        return of(JkRepo.ofLocal());
    }

    /**
     * Creates a JkRepoSet for publishing on <a href="http://central.sonatype.org/">OSSRH</a>
     */
    public static JkRepoSet ofOssrhSnapshotAndRelease(String userName, String password, JkFileSigner signer) {
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

    /**
     * Returns the list of repositories configured in this repository set.
     */
    public List<JkRepo> getRepos() {
        return repos;
    }

    /**
     * Checks if the specified URL is present in the repository set.
     */
    public boolean contains(URL url) {
        return this.repos.stream().anyMatch(repo -> url.equals(repo.getUrl()));
    }

    @Override
    public String toString() {
        return repos.toString();
    }

    public String toStringMultiline(String margin) {
        StringBuilder sb = new StringBuilder();
        for (final JkRepo repo : this.repos) {
            sb.append(repo.toStringMultiline(margin)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns an immutable copy of this repository set, ensuring that the underlying repositories
     * are also marked as read-only.
     */
    public JkRepoSet toReadonly() {
        return new JkRepoSet(this.repos.stream().map(JkRepo::toReadonly).collect(Collectors.toList()));
    }

    /**
     * Retrieves directly the file embodying the specified the external dependency.
     */
    public Path get(JkCoordinate coordinate) {
        final JkInternalDependencyResolver depResolver = getInternalDependencyResolver();
        final File file = depResolver.get(coordinate);
        if (file == null) {
            return null;
        }
        return file.toPath();
    }

    /**
     * Retrieves the file associated with the specified dependency coordinate.
     * Delegates to {@link #get(JkCoordinate)} by constructing a {@link JkCoordinate}
     * from the given string representation.
     *
     * @param coordinate the string representation of the dependency coordinate to resolve
     * @return the path to the resolved file, or {@code null} if the dependency cannot be resolved
     */
    public Path get(String coordinate) {
        return get(JkCoordinate.of(coordinate));
    }

    public JkRepoSet withDefaultSigner(JkFileSigner signer) {
        List<JkRepo> reposCopy = repos.stream()
                .map(repo -> {
                    if (repo.publishConfig.getSigner() == null
                            && repo.publishConfig.isSignatureRequired()) {
                        JkRepo repoCopy = repo.copy();
                        repoCopy.publishConfig.setSigner(signer);
                        return repoCopy;
                    }
                    return repo;
                }).collect(Collectors.toList());
        return new JkRepoSet(reposCopy);
    }

    private JkInternalDependencyResolver getInternalDependencyResolver() {
        if (internalDependencyResolver == null) {
            internalDependencyResolver = JkInternalDependencyResolver.of(this);
        }
        return internalDependencyResolver;
    }



}
