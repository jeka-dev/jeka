package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.system.JkException;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * Result of a module dependency resolution for a given scope.<br/>
 * When resolving a module dependencies for a given scope, we expect to get
 * <ul>
 *   <li>The list of publishLocalOnly file constituting the resolved dependencies (the jar
 *        files for instance)</li>
 *   <li>The {@link JkVersionProvider} that specify which static projectVersion has been
 *        taken in account when a module dependency is declared using dynamic versions
 *        (as 1.0.+)</li>
 * </ul>
 */
public final class JkResolveResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an empty {@link JkResolveResult}
     */
    public static JkResolveResult ofEmpty() {
        return of(JkDependencyNode.ofEmpty());
    }

    /**
     * Creates a dependency resolve result object form a list of module dependency files and a list of resolved versions.
     */
    public static JkResolveResult of(JkDependencyNode depTree, JkErrorReport errorReport) {
        return new JkResolveResult(depTree, errorReport);
    }


    private static JkResolveResult of(JkDependencyNode dependencyTree) {
        return new JkResolveResult(dependencyTree, JkErrorReport.allFine());
    }

    private final JkDependencyNode depTree;

    private final JkErrorReport errorReport;

    private JkResolveResult(JkDependencyNode depTree, JkErrorReport errorReport) {
        super();
        this.depTree = depTree;
        this.errorReport = errorReport;
    }

    /**
     * Shorthand for <code>dependencyTree.allFiles()</code>
     */
    public List<Path> getLocalFiles() {
        return this.depTree.getAllResolvedFiles();
    }

    /**
     * Shorthand for <code>dependencyTree.contains(JkModuleId)</code>
     */
    public boolean contains(JkModuleId moduleId) {
        return this.depTree.contains(moduleId);
    }

    /**
     * Shorthand for <code>resolvedVersion.getVersionOf(JkModuleId)</code>
     */
    public JkVersion getVersionOf(JkModuleId moduleId) {
        return this.getResolvedVersionProvider().versionOf(moduleId);
    }

    /**
     * Shorthand for <code>dependencyTree.childModules(JkModuleId)</code>
     */
    public Set<JkVersionedModule> getInvolvedModules() {
        return this.depTree.getChildModules();
    }

    /**
     * Shorthand for <code>dependencyTree.getResolvedVersions(JkModuleId)</code>
     */
    public JkVersionProvider getResolvedVersionProvider() {
        return this.depTree.getResolvedVersion();
    }

    /**
     * Returns files the specified module is resolved to.
     */
    public List<Path> getResolvedFilesFor(JkModuleId moduleId) {
        final JkDependencyNode dependencyNode = this.depTree.getFirst(moduleId);
        if (dependencyNode == null) {
            return new LinkedList<>();
        }
        return dependencyNode.getModuleInfo().getFiles();
    }

    /**
     * Returns a concatenation of this resolve result and the specified one.
     */
    public JkResolveResult and(JkResolveResult other) {
        return new JkResolveResult(this.depTree.getMerge(other.depTree),
                this.errorReport.merge(other.errorReport));
    }

    /**
     * Returns the dependency tree for this dependency resolution.
     */
    public JkDependencyNode getDependencyTree() {
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
            throw new JkException("Error in dependency resolution : "
                    + this.errorReport + "On following tree : \n" + depTree.toStringComplete());
        }
        return this;
    }

    @Override
    public String toString() {
        return this.depTree.toString();
    }

    /**
     *
     */
    public static class JkErrorReport implements Serializable {

        private static final long serialVersionUID = 1L;

        private final List<JkModuleDepProblem> moduleProblems;

        private final boolean hasErrors;

        static JkErrorReport allFine() {
            return new JkErrorReport(JkUtilsIterable.listOf(), false);
        }

        static JkErrorReport failure(List<JkModuleDepProblem> missingArtifacts) {
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

        @SuppressWarnings("unchecked")
        private JkErrorReport merge(JkErrorReport other) {
            return new JkErrorReport(JkUtilsIterable.concatLists(this.moduleProblems, other.moduleProblems),
                    this.hasErrors || other.hasErrors);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            if (!moduleProblems.isEmpty()) {
                sb.append("Errors with dependencies : ").append(moduleProblems);
            }
            return sb.toString();
        }
    }
}
