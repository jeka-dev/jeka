package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.JkScope;

interface ScopeResolver {

    JkScope scopeOfLib(DotClasspathModel.ClasspathEntry.Kind kind, String path);

    JkScope scopeOfCon(String path);

}
