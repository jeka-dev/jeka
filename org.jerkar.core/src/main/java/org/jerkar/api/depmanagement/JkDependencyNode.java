package org.jerkar.api.depmanagement;

import java.util.Collections;
import java.util.List;

public class JkDependencyNode {

    private final JkVersionedModule module;

    private final List<JkDependencyNode> children;

    public JkDependencyNode(JkVersionedModule module, List<JkDependencyNode> children) {
        super();
        this.module = module;
        this.children = Collections.unmodifiableList(children);
    }







}
