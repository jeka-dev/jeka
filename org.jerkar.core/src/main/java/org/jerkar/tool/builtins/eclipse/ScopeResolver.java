package org.jerkar.tool.builtins.eclipse;

import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.tool.builtins.eclipse.DotClasspath.ClasspathEntry.Kind;

interface ScopeResolver {

    JkScope scopeOfLib(Kind kind, String path);

    JkScope scopeOfCon(String path);

}
