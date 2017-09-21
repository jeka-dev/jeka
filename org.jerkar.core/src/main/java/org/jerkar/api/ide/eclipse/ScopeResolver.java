package org.jerkar.api.ide.eclipse;

import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.ide.eclipse.DotClasspathModel.ClasspathEntry.Kind;

interface ScopeResolver {

    JkScope scopeOfLib(Kind kind, String path);

    JkScope scopeOfCon(String path);

}
