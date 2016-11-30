package org.jerkar.api.depmanagement;

import java.util.LinkedList;
import java.util.List;

public class JkDependencyNode {

    private final JkVersionedModule module;

    private final List<JkScopedDependency> children = new LinkedList<JkScopedDependency>();

    public JkDependencyNode(JkVersionedModule module) {
        super();
        this.module = module;
    }

    void addChild(JkScopedDependency dependency) {
        this.children.add(dependency);
    }



}
