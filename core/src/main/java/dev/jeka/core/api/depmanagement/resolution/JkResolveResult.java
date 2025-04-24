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

package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Result of a module dependency resolution for a given scope.<br/>
 * When resolving a module dependencies for a given scope, we expect to get
 * <ul>
 *   <li>The list of files constituting the resolved dependencies (the jar files for instance)</li>
 *   <li>The {@link JkVersionProvider} that specify which static version has been
 *        taken in account when a module dependency is declared using dynamic versions
 *        (as 1.0.+)</li>
 * </ul>
 */
public final class JkResolveResult {

    private final JkResolvedDependencyNode depTree;

    private final JkErrorReport errorReport;

    private JkResolveResult(JkResolvedDependencyNode depTree, JkErrorReport errorReport) {
        super();
        this.depTree = depTree;
        this.errorReport = errorReport;
    }

    /**
     * Creates an empty {@link JkResolveResult}
     */
    static JkResolveResult ofRoot(JkCoordinate coordinate) {
        final JkResolvedDependencyNode.JkModuleNodeInfo nodeInfo = coordinate == null ?
                JkResolvedDependencyNode.JkModuleNodeInfo.ofAnonymousRoot() :
                    JkResolvedDependencyNode.JkModuleNodeInfo.ofRoot(coordinate);
                return of(JkResolvedDependencyNode.ofEmpty(nodeInfo));
    }

    /**
     * Creates a dependency resolve result object form a list of module dependency files and a list of resolved versions.
     */
    public static JkResolveResult of(JkResolvedDependencyNode depTree, JkErrorReport errorReport) {
        return new JkResolveResult(depTree, errorReport);
    }

    private static JkResolveResult of(JkResolvedDependencyNode dependencyTree) {
        return new JkResolveResult(dependencyTree, JkErrorReport.allFine());
    }

    /**
     * Shorthand for {@link JkResolvedDependencyNode#getResolvedFiles()} on the tree root.
     */
    public JkPathSequence getFiles() {
        return JkPathSequence.of(this.depTree.getResolvedFiles()).withoutDuplicates().resolvedTo(Paths.get(""));
    }

    /**
     * Shorthand for <code>dependencyTree.contains(JkModuleId)</code>
     */
    public boolean contains(JkModuleId jkModuleId) {
        return this.depTree.contains(jkModuleId);
    }

    /**
     * Shorthand for <code>resolvedVersion.getVersionOf(JkModuleId)</code>
     */
    public JkVersion getVersionOf(JkModuleId jkModuleId) {
        return this.getResolvedVersionProvider().getVersionOf(jkModuleId);
    }

    /**
     * Shorthand for <code>dependencyTree.childModules(JkModuleId)</code>
     */
    public Set<JkCoordinate> getInvolvedCoordinates() {
        return this.depTree.getDescendantModuleCoordinates();
    }

    /**
     * Shorthand for <code>dependencyTree.getResolvedVersions(JkModuleId)</code>
     */
    public JkVersionProvider getResolvedVersionProvider() {
        return this.depTree.getResolvedVersions();
    }

    /**
     * Returns files the specified module is resolved to.
     */
    public JkPathSequence getFilesFor(JkModuleId jkModuleId) {
        final JkResolvedDependencyNode dependencyNode = this.depTree.getFirst(jkModuleId);
        if (dependencyNode == null) {
            return JkPathSequence.of();
        }
        return JkPathSequence.of(dependencyNode.getModuleInfo().getFiles()).withoutDuplicates();
    }

    /**
     * Returns a concatenation of this resolve result and the specified one.
     */
    public JkResolveResult and(JkResolveResult other) {
        return new JkResolveResult(this.depTree.withMerging(other.depTree),
                this.errorReport.merge(other.errorReport));
    }

    JkResolveResult withBaseDir(Path baseDir) {
        return new JkResolveResult(this.depTree, this.errorReport);
    }

    /**
     * Returns the dependency tree for this dependency resolution.
     */
    public JkResolvedDependencyNode getDependencyTree() {
        return this.depTree;
    }

    /**
     * Returns an error report if the resolution failed.
     **/
    public JkErrorReport getErrorReport() {
        return errorReport;
    }

    /**
     * Asserts that the resolution happened successfully. Throws an {@link IllegalStateException} otherwise.
     */
    public JkResolveResult assertNoError() {
        if (this.errorReport.hasErrors) {
            throw new IllegalStateException(this.errorReport + "\nOn following tree : \n" + depTree.toStringTree());
        }
        return this;
    }

    /**
     *
     */
    public static class JkErrorReport implements Serializable {

        private static final long serialVersionUID = 1L;

        private final List<JkModuleDepProblem> moduleProblems;

        private final boolean hasErrors;

        public static JkErrorReport allFine() {
            return new JkErrorReport(JkUtilsIterable.listOf(), false);
        }

        public static JkErrorReport failure(List<JkModuleDepProblem> missingArtifacts) {
            return new JkErrorReport(missingArtifacts, true);
        }

        private JkErrorReport(List<JkModuleDepProblem> dependencies, boolean hasErrors) {
            this.moduleProblems = dependencies;
            this.hasErrors = hasErrors || !this.moduleProblems.isEmpty();
        }

        /**
         * Returns the list of problems.
         */
        public List<JkModuleDepProblem> getModuleProblems() {
            return moduleProblems;
        }

        public boolean hasErrors() {
            return this.hasErrors;
        }

        @SuppressWarnings("unchecked")
        private JkErrorReport merge(JkErrorReport other) {
            return new JkErrorReport(JkUtilsIterable.concatLists(this.moduleProblems, other.moduleProblems),
                    this.hasErrors || other.hasErrors);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            if (!moduleProblems.isEmpty()) {
                sb.append("Error with dependencies:\n");
                moduleProblems.forEach(pb -> sb.append("  ").append(pb).append("\n"));
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }
    }

}
