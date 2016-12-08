package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

    private final JkScopedDependency module;

    private final List<JkDependencyNode> children;

    /**
     * Constructs a node for the specified versioned module having the specified
     * direct descendants.
     */
    public JkDependencyNode(JkScopedDependency module, List<JkDependencyNode> children) {
        super();
        this.module = module;
        this.children = Collections.unmodifiableList(children);
    }

    /**
     * Returns a merge of this dependency node with the specified one. The
     * children of the merged node is a union of the two node children.
     */
    public JkDependencyNode merge(JkDependencyNode other) {
        System.out.println("---------------------------- merge " + this.module + " with " + other.module);
        final List<JkDependencyNode> list = new LinkedList<JkDependencyNode>(this.children);
        for (final JkDependencyNode otherNodeChild : other.children) {
            final JkScopedDependency otherScopedDependencyChild = otherNodeChild.module;
            final JkModuleDependency moduleDependency = (JkModuleDependency) otherScopedDependencyChild.dependency();
            if (!directChildrenContains(moduleDependency.moduleId())) {
                list.add(otherNodeChild);
            }
        }
        return new JkDependencyNode(this.module, list);
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
            result.add(JkUtilsString.repeat(INDENT, indentLevel) + this.module);
        }
        if (this.module == null || !expandeds.contains(this.moduleId())) {
            if (this.module != null) {
                expandeds.add(this.moduleId());
            }
            for (final JkDependencyNode child : children) {
                result.addAll(child.toStrings(true, indentLevel+1, expandeds));
            }
        }
        return result;
    }

    private JkModuleId moduleId() {
        final JkModuleDependency moduleDependency = (JkModuleDependency) this.module.dependency();
        return moduleDependency.moduleId();
    }

    @Override
    public String toString() {
        return this.module + " => " + this.children;
    }

}
