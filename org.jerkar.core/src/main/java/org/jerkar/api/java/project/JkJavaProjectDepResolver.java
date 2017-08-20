package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.*;

@Deprecated // Experimental !!!!
public final class JkJavaProjectDepResolver {

    private JkDependencyResolver resolver;

    public static JkJavaProjectDepResolver of(JkDependencies deps) {
        return new JkJavaProjectDepResolver(deps);
    }

    public static JkJavaProjectDepResolver of() {
        return new JkJavaProjectDepResolver(JkDependencies.of());
    }

    private JkJavaProjectDepResolver(JkDependencies dependencies) {
        resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), dependencies)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaDepScopes.DEFAULT_SCOPE_MAPPING));
    }

    public JkJavaProjectDepResolver setDependencies(JkDependencies dependencies) {
        resolver = resolver.withDeps(dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME));
        return this;
    }

    public JkJavaProjectDepResolver setRepos(JkRepo ... repos) {
        return setRepos(JkRepos.of(repos));
    }

    public JkJavaProjectDepResolver setRepos(JkRepos repos) {
        resolver = resolver.withRepos(repos);
        return this;
    }

    public JkJavaProjectDepResolver setForcedVersions(JkVersionProvider forcedVersions) {
        resolver = resolver.withVersions(forcedVersions);
        return this;
    }

    public JkJavaProjectDepResolver setForceRefresh(boolean refresh) {
        resolver = resolver.withParams(resolver.params().refreshed(refresh));
        return this;
    }

    public JkJavaProjectDepResolver setResolutionParameters(JkResolutionParameters resolutionParameters) {
        resolver = resolver.withParams(resolutionParameters);
        return this;
    }

    public JkDependencyResolver resolver() {
        return resolver;
    }

}
