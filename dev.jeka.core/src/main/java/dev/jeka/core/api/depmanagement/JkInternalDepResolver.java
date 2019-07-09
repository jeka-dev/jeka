package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.File;
import java.util.List;


/**
 * Not part of the public API.
 */
public interface JkInternalDepResolver {

    /**
     * @param  module The resolved module. Only use for caching purpose. Can be <code>null</code>
     * @param resolvedScopes scopes to resolve. Generally only one is provided.
     * @param parameters can be null.
     */
    JkResolveResult resolve(JkVersionedModule module, JkDependencySet deps,
            JkResolutionParameters parameters, JkScope... resolvedScopes);

    File get(JkModuleDependency dependency);

    List<String> searchGroups();

    List<String> searchModules(String groupId);

    List<String> searchVersions(JkModuleId moduleId);

    static JkInternalDepResolver of(JkRepoSet repos) {
        final String factoryClassName = "dev.jeka.core.api.depmanagement.embedded.ivy.IvyInternalDepResolverFactory";
        Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(factoryClassName);
        if (factoryClass != null) {
            return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", repos);
        }
        return JkInternalEmbeddedClassloader.createCrossClassloaderProxy(
                JkInternalDepResolver.class, factoryClassName, "of", repos);
    }

}