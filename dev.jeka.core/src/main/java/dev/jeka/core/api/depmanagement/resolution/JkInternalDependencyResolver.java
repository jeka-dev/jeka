package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkUrlFileProxy;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;
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
        return resolve(module, JkQualifiedDependencySet.ofDependencies(depList)
                        .withGlobalExclusions(deps.getGlobalExclusions()), parameters);
    }

    JkResolveResult resolve(JkVersionedModule module, JkQualifiedDependencySet deps, JkResolutionParameters parameters);

    File get(JkModuleDependency dependency);

    List<String> searchGroups();

    List<String> searchModules(String groupId);

    List<String> searchVersions(JkModuleId moduleId);

    /**
     * @param groupCriteria
     * @param moduleNameCtriteria
     * @param searchExpression
     * @return
     */
    List<String> search(String groupCriteria, String moduleNameCriteria, String versionCriteria);

    static JkInternalDependencyResolver of(JkRepoSet repos) {
        final String factoryClassName = "dev.jeka.core.api.depmanagement.embedded.ivy.IvyInternalDepResolverFactory";
        Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(factoryClassName);
        if (factoryClass != null) {
            return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", repos);
        }
        return InternalVvyClassloader.get().createCrossClassloaderProxy(
                JkInternalDependencyResolver.class, factoryClassName, "of", repos);
    }

    static class InternalVvyClassloader {

        private static JkInternalEmbeddedClassloader IVY_CLASSLOADER;

        public static JkInternalEmbeddedClassloader get() {
            if (IVY_CLASSLOADER != null) {
                return IVY_CLASSLOADER;
            }
            JkUrlFileProxy fileProxyIvy = JkUrlFileProxy.of(
                    "https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.0/ivy-2.5.0.jar",
                    JkModuleDependency.of("org.apache.ivy:ivy:2.5.0").cachePath());
            IVY_CLASSLOADER = JkInternalEmbeddedClassloader.ofMainEmbeddedLibs(fileProxyIvy.get());
            return IVY_CLASSLOADER;
        }

    }

}