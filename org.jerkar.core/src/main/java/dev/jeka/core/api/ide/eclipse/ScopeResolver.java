package dev.jeka.core.api.ide.eclipse;

import org.jerkar.api.depmanagement.JkScope;

interface ScopeResolver {

    JkScope scopeOfLib(DotClasspathModel.ClasspathEntry.Kind kind, String path);

    JkScope scopeOfCon(String path);

}
