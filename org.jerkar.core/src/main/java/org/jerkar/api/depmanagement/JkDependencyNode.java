package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * A representation of a node in a dependency tree. A dependency tree may be
 * represented simply by its root node.
 *
 * @author Jerome Angibaud
 */
public class JkDependencyNode implements Serializable {

    private static final String INDENT = "    ";

    private static final long serialVersionUID = 1L;

    private final JkScopedDependency root;

    private final List<JkDependencyNode> children;

    /**
     * Constructs a node for the specified versioned module having the specified
     * direct descendants.
     */
    public static JkDependencyNode empty() {
        return new JkDependencyNode(null, new LinkedList<JkDependencyNode>());
    }

    /**
     * Constructs a node for the specified versioned module having the specified
     * direct descendants.
     */
    public JkDependencyNode(JkScopedDependency module, List<JkDependencyNode> children) {
        super();
        this.root = module;
        this.children = Collections.unmodifiableList(children);
    }

    /**
     * Returns the root module of this dependency node.
     */
    public JkScopedDependency root() {
        return root;
    }

    /**
     * Returns the children nodes for this node in the tree structure.
     */
    public List<JkDependencyNode> children() {
        return children;
    }


    /**
     * Returns a merge of this dependency node with the specified one. The
     * children of the merged node is a union of the two node children.
     */
    public JkDependencyNode merge(JkDependencyNode other) {
        final List<JkDependencyNode> list = new LinkedList<JkDependencyNode>(this.children);
        for (final JkDependencyNode otherNodeChild : other.children) {
            final JkScopedDependency otherScopedDependencyChild = otherNodeChild.root;
            final JkModuleDependency moduleDependency = (JkModuleDependency) otherScopedDependencyChild.dependency();
            if (!directChildrenContains(moduleDependency.moduleId())) {
                list.add(otherNodeChild);
            }
        }
        return new JkDependencyNode(this.root, list);
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
            final String label = indentLevel == 0 ? this.root.toString() : this.root().dependency().toString();
            result.add(JkUtilsString.repeat(INDENT, indentLevel) + label);
        }
        if (this.root == null || !expandeds.contains(this.moduleId())) {
            if (this.root != null) {
                expandeds.add(this.moduleId());
            }
            for (final JkDependencyNode child : children) {
                result.addAll(child.toStrings(true, indentLevel+1, expandeds));
            }
        }
        return result;
    }

    private JkModuleId moduleId() {
        final JkModuleDependency moduleDependency = (JkModuleDependency) this.root.dependency();
        return moduleDependency.moduleId();
    }

    @Override
    public String toString() {
        return JkUtilsString.join(this.toStrings(), "\n");
    }

}
