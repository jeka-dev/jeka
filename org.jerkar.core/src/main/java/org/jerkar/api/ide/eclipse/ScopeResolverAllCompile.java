package org.jerkar.api.ide.eclipse;

import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.ide.eclipse.DotClasspathModel.ClasspathEntry.Kind;


class ScopeResolverAllCompile implements ScopeResolver {

    @Override
    public JkScope scopeOfLib(Kind kind, String path) {
        return JkJavaDepScopes.COMPILE;
    }

    @Override
    public JkScope scopeOfCon(String path) {
        return JkJavaDepScopes.COMPILE;
    }

}
