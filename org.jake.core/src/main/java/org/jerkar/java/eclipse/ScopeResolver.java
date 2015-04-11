package org.jerkar.java.eclipse;

import org.jerkar.depmanagement.JkScope;
import org.jerkar.java.eclipse.DotClasspath.ClasspathEntry.Kind;

interface ScopeResolver {

	JkScope scopeOfLib(Kind kind, String path);

	JkScope scopeOfCon(String path);

}
