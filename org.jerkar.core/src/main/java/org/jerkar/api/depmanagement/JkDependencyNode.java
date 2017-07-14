package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A representation of a node in a dependency tree. A dependency tree may be
 * represented simply by its asScopedDependency node.
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
        return new JkDependencyNode(null, new LinkedList<JkDependencyNode>());
    }

    /**
     * Constructs a node for the specified versioned module having the specified
     * direct flatten.
     */
    public static JkDependencyNode ofModuleDep(ModuleNodeInfo moduleNodeInfo, List<JkDependencyNode> children) {;
        return new JkDependencyNode(moduleNodeInfo, Collections.unmodifiableList(children));
    }

    public static JkDependencyNode ofFileDep(JkDependency.JkFileDependency dependency, Set<JkScope> scopes) {
        final NodeInfo moduleInfo;
        if (dependency instanceof JkFileSystemDependency) {
            moduleInfo = new FileNodeInfo(((JkFileSystemDependency) dependency).files(), scopes, false);
        } else {
            moduleInfo = new FileNodeInfo(((JkComputedDependency) dependency).files(), scopes, true);
        }
        return new JkDependencyNode(moduleInfo, Collections.unmodifiableList(new LinkedList<JkDependencyNode>()));
    }

    JkDependencyNode mergeNonModules(JkDependencies dependencies, Set<JkScope> scopes) {
        final List<JkDependencyNode> result = new LinkedList<JkDependencyNode>();
        final Set<JkDependency.JkFileDependency> addedFileDeps = new HashSet<JkDependency.JkFileDependency>();
        for (JkDependencyNode node : this.children) {
            if (node.isModuleNode()) {
                addFileDepsToTree(dependencies, scopes, result, addedFileDeps, node.moduleId());
                result.add(node);
            }
        }
        addFileDepsToTree(dependencies, scopes, result, addedFileDeps, null);
        return new JkDependencyNode(this.nodeInfo,result);
    }

    public List<File> allFiles() {
        List<File> list = new LinkedList<File>();
        JkUtilsIterable.addAllWithoutDplicate(list, this.nodeInfo.files());
        for (JkDependencyNode child : children) {
            JkUtilsIterable.addAllWithoutDplicate(list, child.allFiles());
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
     * @return
     */
    public ModuleNodeInfo moduleInfo() {
        if (this.nodeInfo instanceof ModuleNodeInfo) {
            return (ModuleNodeInfo) this.nodeInfo;
        }
        throw new IllegalStateException("The current node is type of " + this.nodeInfo.getClass() + " for " + this.nodeInfo + " is not related to a module dependency.");
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
        for (JkDependencyNode child : this.children) {
            boolean contains = child.contains(moduleId);
            if (contains) return true;
        }
        return false;
    }

    /**
     * Returns the resolved version for this node and all its flatten.
     */
    public JkVersionProvider flattenToVersionProvider() {
        return this.resolvedVersions;
    }

    /**
     * Returns the versioned modules which with this result has been created.
     */
    public Set<JkVersionedModule> resolvedModules() {
        Set<JkVersionedModule> result = new HashSet<JkVersionedModule>();
        if (this.isModuleNode() && !this.moduleInfo().isEvicted()) {
            result.add(this.moduleInfo().moduleId.version(this.moduleInfo().resolvedVersion.name()));
        }
        for (JkDependencyNode child : this.children) {
            result.addAll(child.resolvedModules());
        }
        return result;
    }

    /**
     * Returns the children nodes for this node having the specified moduleId.
     */
    public List<JkDependencyNode> children(JkModuleId moduleId) {
        List<JkDependencyNode> result = new LinkedList<JkDependencyNode>();
        for (JkDependencyNode child : children()) {
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
        for (JkDependencyNode node : children) {
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
        final List<JkDependencyNode> resultChildren = new LinkedList<JkDependencyNode>(this.children);
        for (final JkDependencyNode otherNodeChild : other.children) {
           if (!otherNodeChild.isModuleNode() || !directChildrenContains(otherNodeChild.moduleInfo().moduleId())) {
               resultChildren.add(otherNodeChild);
           }
        }
        return new JkDependencyNode(this.nodeInfo, resultChildren);
    }

    private static void addFileDepsToTree(JkDependencies dependencies, Set<JkScope> scopes, List<JkDependencyNode> result,
                                          Set<JkDependency.JkFileDependency> addedFileDeps, JkModuleId moduleId) {
        for (JkScopedDependency scopedDependency : depsUntilLast(dependencies, moduleId)) {
            if (scopes.isEmpty() || scopedDependency.isInvolvedInAnyOf(scopes)) {
                JkDependency.JkFileDependency fileDep = (JkDependency.JkFileDependency) scopedDependency.dependency();
                if (!addedFileDeps.contains(fileDep)) {
                    JkDependencyNode fileNode = JkDependencyNode.ofFileDep(fileDep, scopedDependency.scopes());
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
        List<JkDependencyNode> result = new LinkedList<JkDependencyNode>();
        for (JkDependencyNode child : this.children()) {
            result.add(child);
            result.addAll(child.flatten());
        }
        return result;
    }

    /**
     * Returns first node descendant of this one standing for the specified moduleId, deep first.
     */
    public JkDependencyNode find(JkModuleId moduleId) {
        if (moduleId.equals(this.moduleId())) {
            return this;
        }
        for (JkDependencyNode child : this.flatten()) {
            if (moduleId.equals(child.moduleId())) {
                return child;
            }
        }
        return null;
    }

    private boolean directChildrenContains(JkModuleId moduleId) {
        for (final JkDependencyNode dependencyNode : this.children) {
            if (dependencyNode.moduleId().equals(moduleId)) {
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
            return this.toStrings(false, -1, new HashSet<JkModuleId>());
        }
        return JkUtilsIterable.listOf(this.moduleInfo().toString());
    }

    private List<String> toStrings(boolean showRoot, int indentLevel, Set<JkModuleId> expandeds) {
        final List<String> result = new LinkedList<String>();
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
        StringBuilder builder = new StringBuilder();
        for (String line: toStrings()) {
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

    public interface NodeInfo {

        List<File> files();

        Set<JkScope> declaredScopes();

    }


    public static final class ModuleNodeInfo implements Serializable, NodeInfo {

        private static final long serialVersionUID = 1L;

        private final JkModuleId moduleId;
        private final JkVersionRange declaredVersion;
        private final Set<JkScope> declaredScopes;  // the left conf mapping side in the caller dependency description
        private final Set<JkScope> rootScopes; // scopes fetching this node from root
        private final JkVersion resolvedVersion;
        private final List<File> artifacts;
        private final boolean treeRoot;

        public ModuleNodeInfo(JkModuleId moduleId, JkVersionRange declaredVersion, Set<JkScope> declaredScopes,
                              Set<JkScope> rootScopes, JkVersion resolvedVersion, List<File> artifacts) {
            this(moduleId, declaredVersion, declaredScopes, rootScopes, resolvedVersion, artifacts, false);
        }

        ModuleNodeInfo(JkModuleId moduleId, JkVersionRange declaredVersion, Set<JkScope> declaredScopes,
                              Set<JkScope> rootScopes, JkVersion resolvedVersion, List<File> artifacts, boolean treeRoot) {
            this.moduleId = moduleId;
            this.declaredVersion = declaredVersion;
            this.declaredScopes = declaredScopes;
            this.rootScopes = rootScopes;
            this.resolvedVersion = resolvedVersion;
            this.artifacts = Collections.unmodifiableList(new LinkedList<File>(artifacts));
            this.treeRoot = treeRoot;
        }

        public JkModuleId moduleId() {
            return moduleId;
        }

        public JkVersionRange declaredVersion() {
            return declaredVersion;
        }

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
            String resolvedVersionName = isEvicted() ? "(evicted)" : resolvedVersion.name();
            String declaredVersionLabel = declaredVersion().definition().equals(resolvedVersionName) ? "" : " as " + declaredVersion();
            return moduleId + ":" + resolvedVersion
                    + " (present in " + rootScopes + ")"
                    + " (declared" + declaredVersionLabel + " for scope " + declaredScopes + ")";
        }

        public boolean isEvicted() {
            return resolvedVersion == null;
        }

        @Override
        public List<File> files() {
            return artifacts;
        }
    }



    private static List<JkScopedDependency> depsUntilLast(JkDependencies deps, JkModuleId to) {
        List<JkScopedDependency> result = new LinkedList<JkScopedDependency>();
        List<JkScopedDependency> partialResult = new LinkedList<JkScopedDependency>();
        for (JkScopedDependency scopedDependency : deps) {
            if (scopedDependency.dependency() instanceof JkModuleDependency) {
                JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency.dependency();
                if (moduleDependency.moduleId().equals(to)) {
                      result.addAll(partialResult);
                      partialResult.clear();
                }
            } else if (scopedDependency.dependency() instanceof JkDependency.JkFileDependency) {
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
            ModuleNodeInfo moduleNodeInfo = (ModuleNodeInfo) nodeInfo;
            if (!moduleNodeInfo.treeRoot && !moduleNodeInfo.isEvicted()) {
                result = result.and(moduleNodeInfo.moduleId, moduleNodeInfo.resolvedVersion);
            }
        }
        for (JkDependencyNode child : children) {
            result = result.and(compute(child.nodeInfo(), child.children()));
        }
        return result;
    }

    public static final class FileNodeInfo implements Serializable, NodeInfo {

        private static final long serialVersionUID = 1L;

        private final List<File> files;

        private final Set<JkScope> scopes;

        private final boolean computed;

        public FileNodeInfo(List<File> files, Set<JkScope> scopes, boolean computed) {
            this.files = Collections.unmodifiableList(new LinkedList<File>(files));
            this.scopes = Collections.unmodifiableSet(new HashSet<JkScope>(scopes));
            this.computed = computed;
        }

        public boolean isComputed() {
            return computed;
        }

        @Override
        public List<File> files() {
            return files;
        }

        @Override
        public Set<JkScope> declaredScopes() {
            return scopes;
        }

        @Override
        public String toString() {
            return files + (computed ? " (computed)" : "");
        }
    }

}
