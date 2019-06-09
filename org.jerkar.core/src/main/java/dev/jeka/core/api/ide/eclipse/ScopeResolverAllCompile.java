package dev.jeka.core.api.ide.eclipse;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.depmanagement.JkScope;


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
