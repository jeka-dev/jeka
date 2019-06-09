package dev.jeka.core.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * A representation of a node in a dependency tree.
 *
 * @author Jerome Angibaud
 */
public class JkDependencyNode implements Serializable {

    private static final String INDENT = "    ";

    private static final long serialVersionUID = 1L;

    private final JkNodeInfo nodeInfo;

    private final List<JkDependencyNode> children;

    private final JkVersionProvider resolvedVersions;

    private JkDependencyNode(JkNodeInfo nodeInfo, List<JkDependencyNode> children) {
        this.nodeInfo = nodeInfo;
        this.children = children;
        this.resolvedVersions = compute(nodeInfo, children);
    }

    /**
     * Returns an empty instance of tree.
     */
    static JkDependencyNode ofEmpty(JkNodeInfo nodeInfo) {
        return new JkDependencyNode(nodeInfo, new LinkedList<>());
    }

    /**
     * Constructs a node for the specified versioned module having the specified
     * direct flatten.
     */
    public static JkDependencyNode ofModuleDep(JkModuleNodeInfo moduleNodeInfo, List<JkDependencyNode> children) {
        return new JkDependencyNode(moduleNodeInfo, Collections.unmodifiableList(children));
    }

    public static JkDependencyNode ofFileDep(JkFileDependency dependency, Set<JkScope> scopes) {
        final JkNodeInfo moduleInfo = JkFileNodeInfo.of(scopes, dependency);
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
    public List<Path> getResolvedFiles() {
        final List<Path> list = new LinkedList<>();
        JkUtilsIterable.addAllWithoutDuplicate(list, this.nodeInfo.getFiles());
        for (final JkDependencyNode child : children) {
            JkUtilsIterable.addAllWithoutDuplicate(list, child.getResolvedFiles());
        }
        return list;
    }

    /**
     * Returns true if this node stands for a module dependency. It returns <code>false</code> if
     * it stands for a file dependency.
     */
    public boolean isModuleNode() {
        return this.nodeInfo instanceof JkModuleNodeInfo;
    }

    /**
     * Convenient method to return relative information about this node, assuming this node stands for a module dependency.
     */
    public JkModuleNodeInfo getModuleInfo() {
        if (this.nodeInfo instanceof JkModuleNodeInfo) {
            return (JkModuleNodeInfo) this.nodeInfo;
        }
        throw new IllegalStateException("The current node is type of " + this.nodeInfo.getClass().getName()
                + " (for " + this.nodeInfo + "), so is not a module dependency as expected. Caller must check if type is correct before calling this method.");
    }

    /**
     * Returns information relative to this dependency node.
     */
    public JkNodeInfo getNodeInfo() {
        return this.nodeInfo;
    }

    /**
     * Returns the children nodes for this node in the tree structure.
     */
    public List<JkDependencyNode> getChildren() {
        return children;
    }

    /**
     * Returns <code>true</code> if this node or one of its descendant stand for the specified module.
     * Evicted nodes are not taken in account.
     */
    public boolean contains(JkModuleId moduleId) {
        if (this.isModuleNode() && moduleId.equals(this.getModuleInfo().getModuleId()) && !this.getModuleInfo().isEvicted()) {
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
     * Returns the resolved version for this node and all its children.
     */
    public JkVersionProvider getResolvedVersions() {
        return this.resolvedVersions;
    }

    /**
     * Returns the versioned modules which with this result has been created.
     */
    public Set<JkVersionedModule> getChildModules() {
        return resolvedModules(true);
    }

    private Set<JkVersionedModule> resolvedModules(boolean root) {
        final Set<JkVersionedModule> result = new HashSet<>();
        if (!root && this.isModuleNode() && !this.getModuleInfo().isEvicted()) {
            result.add(this.getModuleInfo().moduleId.withVersion(this.getModuleInfo().resolvedVersion.getValue()));
        }
        for (final JkDependencyNode child : this.children) {
            result.addAll(child.resolvedModules(false));
        }
        return result;
    }

    /**
     * Returns the children nodes for this node having the specified getModuleId.
     */
    public List<JkDependencyNode> getChildren(JkModuleId moduleId) {
        final List<JkDependencyNode> result = new LinkedList<>();
        for (final JkDependencyNode child : getChildren()) {
            if (child.getModuleInfo().getModuleId().equals(moduleId)) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * Returns the getChild node having the specified getModuleId.
     */
    public JkDependencyNode getChild(JkModuleId moduleId) {
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
    public JkDependencyNode withMerging(JkDependencyNode other) {
        final List<JkDependencyNode> resultChildren = new LinkedList<>(this.children);
        for (final JkDependencyNode otherNodeChild : other.children) {
            if (!otherNodeChild.isModuleNode() || !directChildrenContains(otherNodeChild.getModuleInfo().getModuleId())) {
                resultChildren.add(otherNodeChild);
            }
        }
        return new JkDependencyNode(this.nodeInfo, resultChildren);
    }

    private static void addFileDepsToTree(JkDependencySet dependencies, Set<JkScope> scopes, List<JkDependencyNode> result,
                                          Set<JkFileDependency> addedFileDeps, JkModuleId moduleId) {
        for (final JkScopedDependency scopedDependency : depsUntilLast(dependencies, moduleId)) {
            if (scopes.isEmpty() || scopedDependency.isInvolvedInAnyOf(scopes)) {
                final JkFileDependency fileDep = (JkFileDependency) scopedDependency.getDependency();
                if (!addedFileDeps.contains(fileDep)) {
                    final JkDependencyNode fileNode = JkDependencyNode.ofFileDep(fileDep, scopedDependency.getScopes());
                    addedFileDeps.add(fileDep);
                    result.add(fileNode);
                }
            }
        }
    }

    /**
     * Returns all nodes descendant of this one, deep first.
     */
    public List<JkDependencyNode> toFlattenList() {
        final List<JkDependencyNode> result = new LinkedList<>();
        for (final JkDependencyNode child : this.getChildren()) {
            result.add(child);
            result.addAll(child.toFlattenList());
        }
        return result;
    }

    /**
     * Returns first node descendant of this one standing for the specified getModuleId, deep first.
     */
    public JkDependencyNode getFirst(JkModuleId moduleId) {
        if (this.isModuleNode() && moduleId.equals(this.moduleId())) {
            return this;
        }
        for (final JkDependencyNode child : this.toFlattenList()) {
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
        return JkUtilsIterable.listOf(this.getModuleInfo().toString());
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
    public String toStringTree() {
        final StringBuilder builder = new StringBuilder();
        for (final String line: toStrings()) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    private JkModuleId moduleId() {
        return getModuleInfo().getModuleId();
    }

    @Override
    public String toString() {
        return this.getNodeInfo().toString();
    }

    public interface JkNodeInfo extends Serializable {

        List<Path> getFiles();

        Set<JkScope> getDeclaredScopes();

    }

    public static final class JkModuleNodeInfo implements Serializable, JkNodeInfo {

        private static final long serialVersionUID = 1L;

        static JkModuleNodeInfo ofAnonymousRoot() {
            return new JkModuleNodeInfo(JkModuleId.of("anonymousGroup:anonymousName"), JkVersion.UNSPECIFIED,
                    new HashSet<>(), new HashSet<>(), JkVersion.UNSPECIFIED, new LinkedList<>());
        }

        static JkModuleNodeInfo ofRoot(JkVersionedModule versionedModule) {
            return new JkModuleNodeInfo(versionedModule.getModuleId(), versionedModule.getVersion(),
                    new HashSet<>(), new HashSet<>(), versionedModule.getVersion(), new LinkedList<>(), true);
        }

        private final JkModuleId moduleId;
        private final JkVersion declaredVersion;
        private final Set<JkScope> declaredScopes;  // the left conf mapping side in the caller dependency description
        private final Set<JkScope> rootScopes; // scopes fetching this node to baseTree
        private final JkVersion resolvedVersion;
        private final List<File> artifacts; // Path is not serializable
        private final boolean treeRoot;

        JkModuleNodeInfo(JkModuleId moduleId, JkVersion declaredVersion, Set<JkScope> declaredScopes,
                         Set<JkScope> rootScopes, JkVersion resolvedVersion, List<Path> artifacts) {
            this(moduleId, declaredVersion, declaredScopes, rootScopes, resolvedVersion, artifacts, false);
        }

        JkModuleNodeInfo(JkModuleId moduleId, JkVersion declaredVersion, Set<JkScope> declaredScopes,
                         Set<JkScope> rootScopes, JkVersion resolvedVersion, List<Path> artifacts, boolean treeRoot) {
            this.moduleId = moduleId;
            this.declaredVersion = declaredVersion;
            this.declaredScopes = declaredScopes;
            this.rootScopes = rootScopes;
            this.resolvedVersion = resolvedVersion;
            this.artifacts = Collections.unmodifiableList(new LinkedList<>(JkUtilsPath.toFiles(artifacts)));
            this.treeRoot = treeRoot;
        }

        public JkModuleId getModuleId() {
            return moduleId;
        }

        /**
         * Shorthand for {@link #moduleId} + {@link #getResolvedVersion()}
         */
        public JkVersionedModule getResolvedVersionedModule() {
            return moduleId.withVersion(resolvedVersion.getValue());
        }

        public JkVersion getDeclaredVersion() {
            return declaredVersion;
        }

        @Override
        public Set<JkScope> getDeclaredScopes() {
            return declaredScopes;
        }

        public Set<JkScope> getResolvedScopes() {
            return rootScopes;
        }

        public JkVersion getResolvedVersion() {
            return resolvedVersion;
        }

        @Override
        public String toString() {
            if (treeRoot) {
                return "Root";
            }
            final String resolvedVersionName = isEvicted() ? "(evicted)" : resolvedVersion.getValue();
            final String declaredVersionLabel = getDeclaredVersion().getValue().equals(resolvedVersionName) ? "" : " as " + getDeclaredVersion();
            return moduleId + ":" + resolvedVersion
                    + " (present in " + rootScopes + ")"
                    + " (declared" + declaredVersionLabel + " for scope " + declaredScopes + ")";
        }

        public boolean isEvicted() {
            return resolvedVersion == null;
        }

        @Override
        public List<Path> getFiles() {
            return JkUtilsPath.toPaths(artifacts);
        }


    }

    private static List<JkScopedDependency> depsUntilLast(JkDependencySet deps, JkModuleId to) {
        final List<JkScopedDependency> result = new LinkedList<>();
        final List<JkScopedDependency> partialResult = new LinkedList<>();
        for (final JkScopedDependency scopedDependency : deps) {
            if (scopedDependency.getDependency() instanceof JkModuleDependency) {
                final JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency.getDependency();
                if (moduleDependency.getModuleId().equals(to)) {
                    result.addAll(partialResult);
                    partialResult.clear();
                }
            } else if (scopedDependency.getDependency() instanceof JkFileDependency) {
                partialResult.add(scopedDependency);
            }
        }
        if (to == null) {
            result.addAll(partialResult);
        }
        return result;
    }

    private static JkVersionProvider compute(JkNodeInfo nodeInfo, List<JkDependencyNode> children) {
        JkVersionProvider result = JkVersionProvider.of();
        if (nodeInfo instanceof JkModuleNodeInfo) {
            final JkModuleNodeInfo moduleNodeInfo = (JkModuleNodeInfo) nodeInfo;
            if (!moduleNodeInfo.treeRoot && !moduleNodeInfo.isEvicted()) {
                result = result.and(moduleNodeInfo.moduleId, moduleNodeInfo.resolvedVersion);
            }
        }
        for (final JkDependencyNode child : children) {
            result = result.and(compute(child.getNodeInfo(), child.getChildren()));
        }
        return result;
    }

    public static final class JkFileNodeInfo implements Serializable, JkNodeInfo {

        private static final long serialVersionUID = 1L;

        public static JkFileNodeInfo of(Set<JkScope> scopes, JkFileDependency dependency) {
            if (dependency instanceof JkComputedDependency) {
                final JkComputedDependency computedDependency = (JkComputedDependency) dependency;
                return new JkFileNodeInfo(computedDependency.getFiles(), scopes, computedDependency);
            }
            return new JkFileNodeInfo(dependency.getFiles() ,scopes, null);
        }

        // for serialization we need to use File class instead of Path
        private List<File> files;

        private final Set<JkScope> scopes;

        private final JkComputedDependency computationOrigin;

        private JkFileNodeInfo(List<Path> files, Set<JkScope> scopes, JkComputedDependency origin) {
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
        public List<Path> getFiles() {
            return JkUtilsPath.toPaths(files);
        }

        @Override
        public Set<JkScope> getDeclaredScopes() {
            return scopes;
        }

        @Override
        public String toString() {
            return files + (isComputed() ? " (computed)" : "");
        }
    }

}
