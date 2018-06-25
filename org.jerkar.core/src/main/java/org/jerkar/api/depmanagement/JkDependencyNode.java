package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A representation of a node in a dependency tree.
 *
 * @author Jerome Angibaud
 */
public class JkDependencyNode implements Serializable {

    private static final String INDENT = "    ";

    private static final long serialVersionUID = 1L;

    private final NodeInfo nodeInfo;

    private final List<JkDependencyNode> children;

    private final JkVersionProvider resolvedVersions;

    private JkDependencyNode(NodeInfo nodeInfo, List<JkDependencyNode> children) {
        this.nodeInfo = nodeInfo;
        this.children = children;
        this.resolvedVersions = compute(nodeInfo, children);
    }

    /**
     * Returns an empty instance of tree.
     */
    public static JkDependencyNode empty() {
        return new JkDependencyNode(null, new LinkedList<>());
    }

    /**
     * Constructs a node for the specified versioned module having the specified
     * direct flatten.
     */
    public static JkDependencyNode ofModuleDep(ModuleNodeInfo moduleNodeInfo, List<JkDependencyNode> children) {
        return new JkDependencyNode(moduleNodeInfo, Collections.unmodifiableList(children));
    }

    public static JkDependencyNode ofFileDep(JkFileDependency dependency, Set<JkScope> scopes) {
        final NodeInfo moduleInfo = FileNodeInfo.of(scopes, dependency);
        return new JkDependencyNode(moduleInfo, Collections.unmodifiableList(new LinkedList<>()));
    }

    JkDependencyNode mergeNonModules(JkDependencySet dependencies, Set<JkScope> scopes) {
        final List<JkDependencyNode> result = new LinkedList<>();
        final Set<JkFileDependency> addedFileDeps = new HashSet<>();
        for (final JkDependencyNode node : this.children) {
            if (node.isModuleNode()) {
                addFileDepsToTree(dependencies, scopes, result, addedFileDeps, node.moduleId());
                result.add(node);
            }
        }
        addFileDepsToTree(dependencies, scopes, result, addedFileDeps, null);
        return new JkDependencyNode(this.nodeInfo,result);
    }

    /**
     * Returns all files resulting of this dependency node (this node itself plus all descendants).
     */
    public List<Path> allFiles() {
        final List<Path> list = new LinkedList<>();
        JkUtilsIterable.addAllWithoutDuplicate(list, this.nodeInfo.files());
        for (final JkDependencyNode child : children) {
            JkUtilsIterable.addAllWithoutDuplicate(list, child.allFiles());
        }
        return list;
    }

    /**
     * Returns true if this node stands for a module dependency. It returns <code>false</code> if
     * it stands for a file dependency.
     */
    public boolean isModuleNode() {
        return this.nodeInfo instanceof ModuleNodeInfo;
    }

    /**
     * Convenient method to return relative information about this node, assuming this node stands for a module dependency.
     */
    public ModuleNodeInfo moduleInfo() {
        if (this.nodeInfo instanceof ModuleNodeInfo) {
            return (ModuleNodeInfo) this.nodeInfo;
        }
        throw new IllegalStateException("The current node is type of " + this.nodeInfo.getClass().getName()
                + " (for " + this.nodeInfo + "), so is not a module dependency as expected. Caller must check if type is correct before calling this method.");
    }

    /**
     * Returns information relative to this dependency node.
     */
    public NodeInfo nodeInfo() {
        return this.nodeInfo;
    }

    /**
     * Returns the children nodes for this node in the tree structure.
     */
    public List<JkDependencyNode> children() {
        return children;
    }

    /**
     * Returns <code>true</code> if this node or one of its descendant stand for the specified module.
     * Evicted nodes are not taken in account.
     */
    public boolean contains(JkModuleId moduleId) {
        if (this.isModuleNode() && moduleId.equals(this.moduleInfo().moduleId()) && !this.moduleInfo().isEvicted()) {
            return true;
        }
        for (final JkDependencyNode child : this.children) {
            final boolean contains = child.contains(moduleId);
            if (contains) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the resolved projectVersion for this node and all its flatten.
     */
    public JkVersionProvider flattenToVersionProvider() {
        return this.resolvedVersions;
    }

    /**
     * Returns the versioned modules which with this result has been created.
     */
    public Set<JkVersionedModule> childModules() {
        return resolvedModules(true);
    }

    private Set<JkVersionedModule> resolvedModules(boolean root) {
        final Set<JkVersionedModule> result = new HashSet<>();
        if (!root && this.isModuleNode() && !this.moduleInfo().isEvicted()) {
            result.add(this.moduleInfo().moduleId.version(this.moduleInfo().resolvedVersion.name()));
        }
        for (final JkDependencyNode child : this.children) {
            result.addAll(child.resolvedModules(false));
        }
        return result;
    }

    /**
     * Returns the children nodes for this node having the specified moduleId.
     */
    public List<JkDependencyNode> children(JkModuleId moduleId) {
        final List<JkDependencyNode> result = new LinkedList<>();
        for (final JkDependencyNode child : children()) {
            if (child.moduleInfo().moduleId().equals(moduleId)) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * Returns the child node having the specified moduleId.
     */
    public JkDependencyNode child(JkModuleId moduleId) {
        for (final JkDependencyNode node : children) {
            if (node.moduleId().equals(moduleId)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns a merge of this dependency node with the specified one. The
     * children of the merged node is a union of the two node children.
     */
    public JkDependencyNode merge(JkDependencyNode other) {
        final List<JkDependencyNode> resultChildren = new LinkedList<>(this.children);
        for (final JkDependencyNode otherNodeChild : other.children) {
            if (!otherNodeChild.isModuleNode() || !directChildrenContains(otherNodeChild.moduleInfo().moduleId())) {
                resultChildren.add(otherNodeChild);
            }
        }
        return new JkDependencyNode(this.nodeInfo, resultChildren);
    }

    private static void addFileDepsToTree(JkDependencySet dependencies, Set<JkScope> scopes, List<JkDependencyNode> result,
                                          Set<JkFileDependency> addedFileDeps, JkModuleId moduleId) {
        for (final JkScopedDependency scopedDependency : depsUntilLast(dependencies, moduleId)) {
            if (scopes.isEmpty() || scopedDependency.isInvolvedInAnyOf(scopes)) {
                final JkFileDependency fileDep = (JkFileDependency) scopedDependency.dependency();
                if (!addedFileDeps.contains(fileDep)) {
                    final JkDependencyNode fileNode = JkDependencyNode.ofFileDep(fileDep, scopedDependency.scopes());
                    addedFileDeps.add(fileDep);
                    result.add(fileNode);
                }
            }
        }
    }

    /**
     * Returns all nodes descendant of this one, deep first.
     */
    public List<JkDependencyNode> flatten() {
        final List<JkDependencyNode> result = new LinkedList<>();
        for (final JkDependencyNode child : this.children()) {
            result.add(child);
            result.addAll(child.flatten());
        }
        return result;
    }

    /**
     * Returns first node descendant of this one standing for the specified moduleId, deep first.
     */
    public JkDependencyNode find(JkModuleId moduleId) {
        if (this.isModuleNode() && moduleId.equals(this.moduleId())) {
            return this;
        }
        for (final JkDependencyNode child : this.flatten()) {
            if (child.isModuleNode() && moduleId.equals(child.moduleId())) {
                return child;
            }
        }
        return null;
    }

    private boolean directChildrenContains(JkModuleId moduleId) {
        for (final JkDependencyNode dependencyNode : this.children) {
            if (dependencyNode.isModuleNode() && dependencyNode.moduleId().equals(moduleId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of lines standing for the representation of this
     * dependency tree.
     */
    public List<String> toStrings() {
        if (this.isModuleNode()) {
            return this.toStrings(false, -1, new HashSet<>());
        }
        return JkUtilsIterable.listOf(this.moduleInfo().toString());
    }

    private List<String> toStrings(boolean showRoot, int indentLevel, Set<JkModuleId> expandeds) {
        final List<String> result = new LinkedList<>();
        if (showRoot) {
            final String label = nodeInfo.toString();
            result.add(JkUtilsString.repeat(INDENT, indentLevel) + label);
        }
        if (this.nodeInfo == null || (this.isModuleNode() && !expandeds.contains(this.moduleId()))) {
            if (this.nodeInfo != null) {
                expandeds.add(this.moduleId());
            }
            for (final JkDependencyNode child : children) {
                result.addAll(child.toStrings(true, indentLevel+1, expandeds));
            }
        }
        return result;
    }

    /**
     * Returns a complete representation string of the tree.
     */
    public String toStringComplete() {
        final StringBuilder builder = new StringBuilder();
        for (final String line: toStrings()) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    private JkModuleId moduleId() {
        return moduleInfo().moduleId();
    }

    @Override
    public String toString() {
        return this.nodeInfo().toString();
    }

    public interface NodeInfo extends Serializable {

        List<Path> files();

        Set<JkScope> declaredScopes();

    }

    public static final class ModuleNodeInfo implements Serializable, NodeInfo {

        private static final long serialVersionUID = 1L;

        static ModuleNodeInfo anonymousRoot() {
            return new ModuleNodeInfo(JkModuleId.of("anonymousGroup:anonymousName"), JkVersionRange.of("-"),
                    new HashSet<>(), new HashSet<>(), JkVersion.name("-"), new LinkedList<>());
        }

        static ModuleNodeInfo root(JkVersionedModule versionedModule) {
            return new ModuleNodeInfo(versionedModule.moduleId(), JkVersionRange.of("-"),
                    new HashSet<>(), new HashSet<>(), versionedModule.version(), new LinkedList<>());
        }

        private final JkModuleId moduleId;
        private final JkVersionRange declaredVersion;
        private final Set<JkScope> declaredScopes;  // the left conf mapping side in the caller dependency description
        private final Set<JkScope> rootScopes; // scopes fetching this node to baseTree
        private final JkVersion resolvedVersion;
        private final List<File> artifacts; // Path is not serializable
        private final boolean treeRoot;

        ModuleNodeInfo(JkModuleId moduleId, JkVersionRange declaredVersion, Set<JkScope> declaredScopes,
                Set<JkScope> rootScopes, JkVersion resolvedVersion, List<Path> artifacts) {
            this(moduleId, declaredVersion, declaredScopes, rootScopes, resolvedVersion, artifacts, false);
        }

        ModuleNodeInfo(JkModuleId moduleId, JkVersionRange declaredVersion, Set<JkScope> declaredScopes,
                Set<JkScope> rootScopes, JkVersion resolvedVersion, List<Path> artifacts, boolean treeRoot) {
            this.moduleId = moduleId;
            this.declaredVersion = declaredVersion;
            this.declaredScopes = declaredScopes;
            this.rootScopes = rootScopes;
            this.resolvedVersion = resolvedVersion;
            this.artifacts = Collections.unmodifiableList(new LinkedList<>(JkUtilsPath.toFiles(artifacts)));
            this.treeRoot = treeRoot;
        }

        public JkModuleId moduleId() {
            return moduleId;
        }

        /**
         * Shorthand for {@link #moduleId} + {@link #resolvedVersion()}
         */
        public JkVersionedModule resolvedVersionedModule() {
            return moduleId.version(resolvedVersion.name());
        }

        public JkVersionRange declaredVersion() {
            return declaredVersion;
        }

        @Override
        public Set<JkScope> declaredScopes() {
            return declaredScopes;
        }

        public Set<JkScope> resolvedScopes() {
            return rootScopes;
        }

        public JkVersion resolvedVersion() {
            return resolvedVersion;
        }

        @Override
        public String toString() {
            final String resolvedVersionName = isEvicted() ? "(evicted)" : resolvedVersion.name();
            final String declaredVersionLabel = declaredVersion().definition().equals(resolvedVersionName) ? "" : " as " + declaredVersion();
            return moduleId + ":" + resolvedVersion
                    + " (present in " + rootScopes + ")"
                    + " (declared" + declaredVersionLabel + " for scope " + declaredScopes + ")";
        }

        public boolean isEvicted() {
            return resolvedVersion == null;
        }

        @Override
        public List<Path> files() {
            return JkUtilsPath.toPaths(artifacts);
        }

        public List<Path> paths() {
            return artifacts.stream().map(file -> file.toPath()).collect(Collectors.toList());
        }
    }

    private static List<JkScopedDependency> depsUntilLast(JkDependencySet deps, JkModuleId to) {
        final List<JkScopedDependency> result = new LinkedList<>();
        final List<JkScopedDependency> partialResult = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : deps) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                final JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency.dependency();
                if (moduleDependency.moduleId().equals(to)) {
                    result.addAll(partialResult);
                    partialResult.clear();
                }
            } else if (scopedDependency.dependency() instanceof JkFileDependency) {
                partialResult.add(scopedDependency);
            }
        }
        if (to == null) {
            result.addAll(partialResult);
        }
        return result;
    }

    private static JkVersionProvider compute(NodeInfo nodeInfo, List<JkDependencyNode> children) {
        JkVersionProvider result = JkVersionProvider.empty();
        if (nodeInfo instanceof ModuleNodeInfo) {
            final ModuleNodeInfo moduleNodeInfo = (ModuleNodeInfo) nodeInfo;
            if (!moduleNodeInfo.treeRoot && !moduleNodeInfo.isEvicted()) {
                result = result.and(moduleNodeInfo.moduleId, moduleNodeInfo.resolvedVersion);
            }
        }
        for (final JkDependencyNode child : children) {
            result = result.and(compute(child.nodeInfo(), child.children()));
        }
        return result;
    }

    public static final class FileNodeInfo implements Serializable, NodeInfo {

        private static final long serialVersionUID = 1L;

        public static FileNodeInfo of(Set<JkScope> scopes, JkFileDependency dependency) {
            if (dependency instanceof JkComputedDependency) {
                final JkComputedDependency computedDependency = (JkComputedDependency) dependency;
                return new FileNodeInfo(computedDependency.paths(), scopes, computedDependency);
            }
            return new FileNodeInfo(dependency.paths() ,scopes, null);
        }

        // for serialization we need to use File class instead of Path
        private List<File> files;

        private final Set<JkScope> scopes;

        private final JkComputedDependency computationOrigin;

        private FileNodeInfo(List<Path> files, Set<JkScope> scopes, JkComputedDependency origin) {
            this.files = Collections.unmodifiableList(new LinkedList<>(JkUtilsPath.toFiles(files)));
            this.scopes = Collections.unmodifiableSet(new HashSet<>(scopes));
            this.computationOrigin = origin;
        }

        /**
         * Returns <code>true</code> if this node come from a computed dependency
         */
        public boolean isComputed() {
            return computationOrigin != null;
        }

        /**
         * If this node comes from a computed dependency, it returns computed dependency in question.
         */
        public JkComputedDependency computationOrigin() {
            return computationOrigin;
        }

        @Override
        @Deprecated
        public List<Path> files() {
            return JkUtilsPath.toPaths(files);
        }

        public List<Path> paths() {
            return files.stream().map(file -> file.toPath()).collect(Collectors.toList());
        }

        @Override
        public Set<JkScope> declaredScopes() {
            return scopes;
        }

        @Override
        public String toString() {
            return files + (isComputed() ? " (computed)" : "");
        }
    }

}
