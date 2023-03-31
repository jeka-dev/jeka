package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkUrlFileProxy;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Not part of the public API.
 */
public interface JkInternalDependencyResolver {

    String IVY_URL_PATH = "org/apache/ivy/ivy/2.5.0/ivy-2.5.0.jar";

    JkCoordinate IVY_COORDINATE = JkCoordinate.of("org.apache.ivy:ivy:2.5.0");

    /**
     * @param  coordinate The coordinate of the module to be resolved. Only used for caching purpose. Can be <code>null</code>
     * @param parameters can be null.
     */
    default JkResolveResult resolve(JkCoordinate coordinate, JkDependencySet deps,
                                    JkResolutionParameters parameters) {
        List<JkDependency> depList = deps.normalised(JkCoordinate.ConflictStrategy.FAIL)
                .getVersionedDependencies();
        return resolve(coordinate, JkQualifiedDependencySet.ofDependencies(depList)
                        .withGlobalExclusions(deps.getGlobalExclusions()), parameters);
    }

    JkResolveResult resolve(JkCoordinate coordinate, JkQualifiedDependencySet deps, JkResolutionParameters parameters);

    File get(JkCoordinate coordinate);

    List<String> searchGroups();

    List<String> searchModules(String groupId);

    List<String> searchVersions(JkModuleId jkModuleId);

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

    class InternalVvyClassloader {

        private static JkInternalEmbeddedClassloader IVY_CLASSLOADER;

        public static JkInternalEmbeddedClassloader get() {
            if (IVY_CLASSLOADER != null) {
                return IVY_CLASSLOADER;
            }
            Path targetPath = IVY_COORDINATE.cachePath();
            if (!Files.exists(targetPath)) {
                downloadIvy(targetPath);
            }
            IVY_CLASSLOADER = JkInternalEmbeddedClassloader.ofMainEmbeddedLibs(targetPath);
            return IVY_CLASSLOADER;
        }

    }

    static void downloadIvy(Path path) {
        Path globalPropertiesFile = JkLocator.getJekaUserHomeDir().resolve("global.properties");
        Path localPropertiesFile = Paths.get("jeka").resolve("local.properties");
        JkProperties properties = JkProperties.SYS_PROPS_THEN_ENV;
        if (Files.exists(localPropertiesFile)) {
            properties = properties.withFallback(JkProperties.ofFile(localPropertiesFile));
        }
        if (Files.exists(globalPropertiesFile)) {
            properties = properties.withFallback(JkProperties.ofFile(globalPropertiesFile));
        }
        JkRepoSet repos = JkRepoProperties.of(properties).getDownloadRepos();
        for (JkRepo repo : repos.getRepos()) {
            String url = repo.getUrl().toString();
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            String fullUrl = url + IVY_URL_PATH;
            try {
                JkLog.info("Trying to download ivy from (jeka.repos.download) " + fullUrl);
                JkUrlFileProxy.of(fullUrl, path).get();
                return;
            } catch (UncheckedIOException e) {
                JkLog.info("Failed to download ivy from " + fullUrl);
            }
        }
        String fullUrl = "https://repo1.maven.org/maven2/" + IVY_URL_PATH;
        try {
            JkLog.info("Trying to download ivy from " + fullUrl);
            JkUrlFileProxy.of(fullUrl, path).get();
        } catch (UncheckedIOException e) {
            JkLog.error("Failed to download ivy from " + fullUrl);
            JkLog.error("set environment variable JEKA_REPOS_DOWNLOAD or a property 'jeka.repos.download' " +
                    "such as $JEKA_REPOS_DOWNLOAD/" + IVY_URL_PATH + " point to an accessible jar file." );
            throw e;
        }

    }



}