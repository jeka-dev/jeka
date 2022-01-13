package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkUrlFileProxy;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;

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

        private static JkInternalClassloader IVY_CLASSLOADER;

        public static JkInternalClassloader get() {
            if (IVY_CLASSLOADER != null) {
                return IVY_CLASSLOADER;
            }
            JkUrlFileProxy fileProxy = JkUrlFileProxy.of(
                    "https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.0/ivy-2.5.0.jar",
                    JkLocator.getCacheDir().resolve("downloads").resolve("for-jeka-internal-ivy-2.5.0"));;
            IVY_CLASSLOADER = JkInternalClassloader.ofMainEmbeddedLibs(fileProxy.get());
            return IVY_CLASSLOADER;
        }

    }

}