package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.tooling.JkScope;
import dev.jeka.core.api.system.JkLog;

/**
 * A structure to manage consistently dependencies and their resolution.
 * It contains a cache, a default scope for dependencies declared without scope.
 * @param <T> Parent type for chaining
 */
public class JkDependencyManagement<T> {

    private JkResolveResult cachedResult = null;

    private final JkDependencyResolver<JkDependencyManagement<T>> resolver;

    private JkScope[] defaultScope = JkScope.COMPILE_AND_RUNTIME;



    /**
     * For parent chaining
     */
    public final T __;

    private JkDependencySet dependencies = JkDependencySet.of();

    private JkDependencyManagement(T __) {
        this.__ = __;
        resolver = JkDependencyResolver.ofParent(this);
        resolver.addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral());
    }

    public static <T> JkDependencyManagement<T> ofParent(T parent) {
       return new JkDependencyManagement(parent);
    }

    public static JkDependencyManagement<Void> of() {
        return ofParent(null);
    }

    public JkDependencySet getDependencies() {
        return this.dependencies;
    }

    public JkDependencyManagement<T> removeDependencies() {
        cachedResult = null;
        this.dependencies = JkDependencySet.of();
        return this;
    }

    public JkDependencyManagement<T> addDependencies(JkDependencySet dependencies) {
        cachedResult = null;
        this.dependencies = this.dependencies.and(dependencies);
        return this;
    }

    public JkDependencyResolver<JkDependencyManagement<T>> getResolver() {
        return resolver;
    }



    public JkScope[] getDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(JkScope[] defaultScope) {
        this.defaultScope = defaultScope;
    }

    // ------------

    public JkDependencyManagement<T> cleanCache() {
        cachedResult = null;
        return this;
    }

    /**
     * Returns lib paths standing for the resolution of this project dependencies for the specified dependency scopes.
     */
    public JkResolveResult resolveDependencies() {
        if (cachedResult == null) {
            cachedResult = this.fetchDependencies();
        }
        return cachedResult;
    }

    private JkResolveResult fetchDependencies()  {
        JkResolveResult resolveResult = resolver.resolve(dependencies);
        JkResolveResult.JkErrorReport report = resolveResult.getErrorReport();
        if (report.hasErrors()) {
            if (resolver.getParams().isFailOnDependencyResolutionError()) {
                throw new IllegalStateException(report.toString());
            }
            JkLog.warn(report.toString());
        }
        return resolveResult;
    }

}
