package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.File;
import java.util.List;


/**
 * Not part of the public API.
 */
public interface JkInternalDependencyResolver {

    /**
     * @param  module The resolved module. Only use for caching purpose. Can be <code>null</code>
     * @param parameters can be null.
     */
    default JkResolveResult resolve(JkVersionedModule module, JkDependencySet deps,
                                    JkResolutionParameters parameters) {
        List<JkDependency> depList = deps.normalised(JkVersionedModule.ConflictStrategy.FAIL)
                .getVersionedDependencies();
        return resolve(module, JkQualifiedDependencies.ofDependencies(depList)
                        .withGlobalExclusions(deps.getGlobalExclusions()), parameters);
    }

    JkResolveResult resolve(JkVersionedModule module, JkQualifiedDependencies deps, JkResolutionParameters parameters);

    File get(JkModuleDependency dependency);

    List<String> searchGroups();

    List<String> searchModules(String groupId);

    List<String> searchVersions(JkModuleId moduleId);

    static JkInternalDependencyResolver of(JkRepoSet repos) {
        final String factoryClassName = "dev.jeka.core.api.depmanagement.embedded.ivy.IvyInternalDepResolverFactory";
        Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(factoryClassName);
        if (factoryClass != null) {
            return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", repos);
        }
        return JkInternalClassloader.ofMainEmbeddedLibs().createCrossClassloaderProxy(
                JkInternalDependencyResolver.class, factoryClassName, "of", repos);
    }

}