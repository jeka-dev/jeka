package org.jake.java.eclipse;

import org.jake.depmanagement.JakeScope;
import org.jake.java.eclipse.DotClasspath.ClasspathEntry.Kind;

interface ScopeResolver {

	JakeScope scopeOfLib(Kind kind, String path);

	JakeScope scopeOfCon(String path);

}
