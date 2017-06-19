package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsAssert;
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

    private JkDependencyNode(NodeInfo nodeInfo, List<JkDependencyNode> children) {
        this.nodeInfo = nodeInfo;
        this.children = children;
    }

    /**
     * Returns an empty instance of tree.
     */
    public static JkDependencyNode empty() {
        return new JkDependencyNode(null, new LinkedList<JkDependencyNode>());
    }

    /**
     * Constructs a node for the specified versioned module having the specified
     * direct descendants.
     */
    public static JkDependencyNode ofModuleDep(ModuleNodeInfo moduleNodeInfo, List<JkDependencyNode> children) {;
        return new JkDependencyNode(moduleNodeInfo, Collections.unmodifiableList(children));
    }

    public static JkDependencyNode ofFileDep(JkDependency.JkFileDependency dependency, Set<JkScope> scopes) {
        final NodeInfo moduleInfo;
        if (dependency instanceof JkFileSystemDependency) {
            moduleInfo = new FileNodeInfo(((JkFileSystemDependency) dependency).files(), false);
        } else {
            moduleInfo = new FileNodeInfo(((JkComputedDependency) dependency).files(), true);
        }
        return new JkDependencyNode(moduleInfo, Collections.unmodifiableList(new LinkedList<JkDependencyNode>()));
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
        if (this.nodeInfo instanceof NodeInfo) {
            return (ModuleNodeInfo) this.nodeInfo;
        }
        throw new IllegalStateException("The current node is type of " + this.nodeInfo.getClass() + ". It is not related to a module dependency.");
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

    /**
     * Returns all nodes descendant of this one, deep first.
     */
    public List<JkDependencyNode> descendants() {
        List<JkDependencyNode> result = new LinkedList<JkDependencyNode>();
        for (JkDependencyNode child : this.children()) {
            result.add(child);
            result.addAll(child.descendants());
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
        for (JkDependencyNode child : this.descendants()) {
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
        return this.toStrings(false, -1, new HashSet<JkModuleId>());
    }

    private List<String> toStrings(boolean showRoot, int indentLevel, Set<JkModuleId> expandeds) {
        final List<String> result = new LinkedList<String>();
        if (showRoot) {
            final String label = nodeInfo.toString();
            result.add(JkUtilsString.repeat(INDENT, indentLevel) + label);
        }
        if (this.nodeInfo == null || !expandeds.contains(this.moduleId())) {
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

    private interface NodeInfo {

    }

    public static final class ModuleNodeInfo implements Serializable, NodeInfo {

        private static final long serialVersionUID = 1L;

        private final JkModuleId moduleId;
        private final JkVersionRange declaredVersion;
        private final Set<JkScope> declaredScopes;  // the left conf mapping side in the caller dependency description
        private final Set<JkScope> rootScopes; // scopes fetching this node from root
        private final JkVersion resolvedVersion;

        public ModuleNodeInfo(JkModuleId moduleId, JkVersionRange declaredVersion, Set<JkScope> declaredScopes,
                              Set<JkScope> rootScopes, JkVersion resolvedVersion) {
            this.moduleId = moduleId;
            this.declaredVersion = declaredVersion;
            this.declaredScopes = declaredScopes;
            this.rootScopes = rootScopes;
            this.resolvedVersion = resolvedVersion;
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
    }

    private static final class FileNodeInfo implements Serializable, NodeInfo {

        private static final long serialVersionUID = 1L;

        private final List<File> files;

        private final boolean computed;

        public FileNodeInfo(List<File> files, boolean computed) {
            this.files = files;
            this.computed = computed;
        }

        public boolean isComputed() {
            return computed;
        }

        public List<File> getFiles() {
            return files;
        }

        @Override
        public String toString() {
            return files + (computed ? " (computed)" : "");
        }
    }

}
