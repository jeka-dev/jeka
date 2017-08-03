package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsIterable;

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
    public static JkResolveResult empty() {
        return of(JkDependencyNode.empty());
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
    public List<File> localFiles() {
        return this.depTree.allFiles();
    }

    /**
     * Shorthand for <code>dependencyTree.contains(JkModuleId)</code>
     */
    public boolean contains(JkModuleId moduleId) {
        return this.depTree.contains(moduleId);
    }

    /**
     * Shorthand for <code>resolvedVersion.versionOf(JkModuleId)</code>
     */
    public JkVersion versionOf(JkModuleId moduleId) {
        return this.resolvedVersionProvider().versionOf(moduleId);
    }

    /**
     * Shorthand for <code>dependencyTree.childModules(JkModuleId)</code>
     */
    public Set<JkVersionedModule> involvedModules() {
        return this.depTree.childModules();
    }

    /**
     * Shorthand for <code>dependencyTree.resolvedVersions(JkModuleId)</code>
     */
    public JkVersionProvider resolvedVersionProvider() {
        return this.depTree.flattenToVersionProvider();
    }

    /**
     * Returns the local files the specified module turns to.
     */
    public List<File> filesOf(JkModuleId moduleId) {
        final JkDependencyNode dependencyNode = this.depTree.find(moduleId);
        if (dependencyNode == null) {
            return new LinkedList<File>();
        }
        return dependencyNode.moduleInfo().files();
    }

    /**
     * Returns a concatenation of this resolve result and the specified one.
     */
    public JkResolveResult and(JkResolveResult other) {
        return new JkResolveResult(this.depTree.merge(other.depTree),
                this.errorReport.merge(other.errorReport));
    }

    /**
     * Returns the dependency tree for this dependency resolution.
     */
    public JkDependencyNode dependencyTree() {
        return this.depTree;
    }

    /**
     * Returns an error report if the resolution failed.
     **/
    public JkErrorReport errorReport() {
        return errorReport;
    }

    /**
     * Asserts that the resolution happened successfully. Throws an {@link IllegalStateException} otherwise.
     */
    public JkResolveResult assertNoError() {
        if (this.errorReport.hasErrors) {
            throw new IllegalStateException("Error in dependency resolution : "
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
            return new JkErrorReport(JkUtilsIterable.<JkModuleDepProblem>listOf(), false);
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
        public List<JkModuleDepProblem> moduleProblems() {
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
                sb.append("Errors with dependencies : " + moduleProblems);
            }
            return sb.toString();
        }
    }
}
