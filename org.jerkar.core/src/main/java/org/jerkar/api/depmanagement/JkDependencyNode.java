package org.jerkar.api.depmanagement;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A representation of a node in a dependency tree. A dependency tree may be represented simply by its root node.
 *
 * @author Jerome Angibaud
 */
public class JkDependencyNode {

    private final JkVersionedModule module;

    private final List<JkDependencyNode> children;

    /**
     * Constructs a node for the specified versioned module having the specified direct descendants.
     */
    public JkDependencyNode(JkVersionedModule module, List<JkDependencyNode> children) {
        super();
        this.module = module;
        this.children = Collections.unmodifiableList(children);
    }

    /**
     * Returns a merge of this dependency node with the specified one. The root versioned module id of the two node must be equals.
     * The children of the merged node is a union of the two node children.
     */
    public JkDependencyNode merge(JkDependencyNode other) {
        if (!this.module.equals(other.module)) {
            throw new IllegalArgumentException("Try to merge dependency tree of " + this.module + " with dependency tree of " + other.module + ". Should be the same.");
        }
        final List<JkDependencyNode> list = new LinkedList<JkDependencyNode>(this.children);
        for (final JkDependencyNode otherNode : other.children) {
            final JkVersionedModule otherChild = other.module;
            if (! childrenContains(otherChild.moduleId())) {
                list.add(otherNode);
            }
        }
        return new JkDependencyNode(module, list);
    }

    private boolean childrenContains(JkModuleId moduleId) {
        for (final JkDependencyNode dependencyNode : this.children) {
            if (dependencyNode.module.moduleId().equals(moduleId)) {
                return true;
            }
        }
        return false;
    }


}
