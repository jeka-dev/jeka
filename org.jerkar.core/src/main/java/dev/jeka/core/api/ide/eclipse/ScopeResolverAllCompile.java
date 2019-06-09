package dev.jeka.core.api.ide.eclipse;

import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkScope;


class ScopeResolverAllCompile implements ScopeResolver {

    @Override
    public JkScope scopeOfLib(DotClasspathModel.ClasspathEntry.Kind kind, String path) {
        return JkJavaDepScopes.COMPILE;
    }

    @Override
    public JkScope scopeOfCon(String path) {
        return JkJavaDepScopes.COMPILE;
    }

}
