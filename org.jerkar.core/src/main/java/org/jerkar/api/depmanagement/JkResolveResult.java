package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Result of a module dependency resolution for a given scope.<br/>
 * When resolving a module dependencies for a given scope, we expect to get
 * <ul>
 *   <li>The list of local file constituting the resolved dependencies (the jar
 *        files for instance)</li>
 *   <li>The {@link JkVersionProvider} that specify which static version has been
 *        taken in account when a module dependency is declared using dynamic versions
 *        (as 1.0.+)</li>
 * </ul>
 */
public final class JkResolveResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an empty {@link JkResolveResult}
     */
    @SuppressWarnings("unchecked")
    public static JkResolveResult empty(JkVersionedModule versionedModule) {
        return of(Collections.EMPTY_LIST, new JkDependencyNode(null, new LinkedList<JkDependencyNode>()));
    }

    /**
     * Creates a dependency resolve result object form a list of module dependency files and a list of resolved versions.
     */
    public static JkResolveResult of(List<JkModuleDepFile> artifacts,
            JkVersionProvider jkVersionProvider, JkDependencyNode depTree) {
        return new JkResolveResult(artifacts, jkVersionProvider, depTree);
    }

    /**
     * Creates a dependency resolve result object form a list of module dependency files.
     */
    private static JkResolveResult of(List<JkModuleDepFile> artifacts, JkDependencyNode dependencyTree) {
        return new JkResolveResult(artifacts, JkVersionProvider.empty(), dependencyTree);
    }

    private final List<JkModuleDepFile> jkModuleDepFiles;

    private final JkVersionProvider jkVersionProvider;

    private final JkDependencyNode depTree;

    private JkResolveResult(List<JkModuleDepFile> artifacts, JkVersionProvider jkVersionProvider, JkDependencyNode depTree) {
        super();
        this.jkModuleDepFiles = artifacts;
        this.jkVersionProvider = jkVersionProvider;
        this.depTree = depTree;
    }

    /**
     * Returns the list of local files standing for this dependencies resolution.
     */
    public List<File> localFiles() {
        final List<File> result = new LinkedList<File>();
        for (final JkModuleDepFile artifact : this.jkModuleDepFiles) {
            result.add(artifact.localFile());
        }
        return result;
    }

    public boolean contains(JkModuleId moduleId) {
        for (JkModuleDepFile moduleDepFile : jkModuleDepFiles) {
            if (moduleDepFile.versionedModule().moduleId().equals(moduleId)) {
                return true;
            }
        }
        return false;
    }



    /**
     * Returns the versioned modules which with this result has been created.
     */
    public Set<JkVersionedModule> involvedModules() {
        final Set<JkVersionedModule> result = new HashSet<JkVersionedModule>();
        for (final JkModuleDepFile artifact : this.jkModuleDepFiles) {
            result.add(artifact.versionedModule());
        }
        return result;
    }

    /**
     * Returns the version provider which with this result has been created.
     */
    public JkVersionProvider resolvedVersionProvider() {
        return jkVersionProvider;
    }

    /**
     * Returns the local files the specified module turns to.
     */
    public List<File> filesOf(JkModuleId jkModuleId) {
        final List<File> result = new LinkedList<File>();
        for (final JkModuleDepFile artifact : this.jkModuleDepFiles) {
            if (jkModuleId.equals(artifact.versionedModule().moduleId())) {
                result.add(artifact.localFile());
            }
        }
        return result;
    }

    /**
     * Returns a concatenation of this resolve result and the specified one.
     */
    public JkResolveResult and(JkResolveResult other) {
        final List<JkModuleDepFile> artifacts = new LinkedList<JkModuleDepFile>(
                this.jkModuleDepFiles);
        artifacts.addAll(other.jkModuleDepFiles);
        final JkVersionProvider jkVersionProvider = this.jkVersionProvider
                .and(other.jkVersionProvider);
        return new JkResolveResult(artifacts, jkVersionProvider, this.depTree.merge(other.depTree));
    }

    /**
     * Returns the dependency tree for this dependency resolution.
     */
    public JkDependencyNode dependencyTree() {
        return this.depTree;
    }

    @Override
    public String toString() {
        return this.jkModuleDepFiles.toString();
    }
}
