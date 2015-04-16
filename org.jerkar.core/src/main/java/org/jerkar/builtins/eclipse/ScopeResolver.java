package org.jerkar.builtins.eclipse;

import org.jerkar.builtins.eclipse.DotClasspath.ClasspathEntry.Kind;
import org.jerkar.depmanagement.JkScope;

interface ScopeResolver {

	JkScope scopeOfLib(Kind kind, String path);

	JkScope scopeOfCon(String path);

}
