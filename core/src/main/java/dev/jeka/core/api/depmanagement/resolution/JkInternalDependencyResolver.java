/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.http.JkHttpRequest;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalChildFirstClassLoader;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Not part of the public API.
 */
public interface JkInternalDependencyResolver {

    String IVY_URL_PATH = "org/apache/ivy/ivy/2.5.3/ivy-2.5.3.jar";

    JkCoordinate IVY_COORDINATE = JkCoordinate.of("org.apache.ivy:ivy:2.5.3");

    /**
     * @param  coordinate The coordinate of the module to be resolved. Only used for caching purpose. Can be <code>null</code>
     * @param parameters can be null.
     */
    default JkResolveResult resolve(JkCoordinate coordinate,
                                    JkDependencySet deps,
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
        /*
        return InternalVvyClassloader.get().createCrossClassloaderProxy(
                JkInternalDependencyResolver.class, factoryClassName, "of", repos);

         */
        factoryClass = JkClassLoader.of(InternalIvyClassloader.get()).load(factoryClassName);
        return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", repos);
    }

    class InternalIvyClassloader {

        private static ClassLoader IVY_CLASSLOADER;

        public static ClassLoader get() {
            if (IVY_CLASSLOADER != null) {
                return IVY_CLASSLOADER;
            }
            Path targetPath = IVY_COORDINATE.cachePath();
            final boolean needDownload;
            if (!Files.exists(targetPath)) {
                System.err.println("Ivy jar file not found at: " + targetPath);
                needDownload = true;
            } else if (!ivyJarValid(targetPath)) {
                System.err.println("Ivy jar file " + targetPath + " seems invalid.");
                needDownload = true;
            } else {
                needDownload = false;
            }
            if (needDownload) {
                downloadIvy(targetPath);
            }
            ClassLoader parentClassloader = InternalIvyClassloader.class.getClassLoader();
            IVY_CLASSLOADER = JkInternalChildFirstClassLoader.of(targetPath, parentClassloader);
            //IVY_CLASSLOADER = JkInternalEmbeddedClassloader.ofMainEmbeddedLibs(targetPath);
            return IVY_CLASSLOADER;
        }
    }

    static void downloadIvy(Path path) {
        JkProperties properties = JkProperties.ofStandardProperties();
        JkRepoSet repos = JkRepoProperties.of(properties).getDownloadRepos();
        for (JkRepo repo : repos.getRepos()) {
            String url = repo.getUrl().toString();
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            String fullUrl = url + IVY_URL_PATH;
            try {
                System.err.println("Trying to download ivy from (jeka.repos.download) " + fullUrl);
                JkUtilsPath.deleteIfExists(path);
                URL downloadUrl = JkUtilsIO.toUrl(fullUrl);
                JkHttpRequest.of(downloadUrl.toString())
                                .customize(repo::customizeUrlConnection)
                                .downloadFile(path);
                //JkUtilsNet.downloadFile(downloadUrl, path, repo::customizeUrlConnection);
                if (checkDownloadOk(path)) {
                    return;
                }
            } catch (UncheckedIOException e) {
                System.err.println("Failed to download ivy from " + fullUrl);
                e.printStackTrace(System.err);
            }
        }
        String fullUrl = "https://repo1.maven.org/maven2/" + IVY_URL_PATH;
        try {
            System.err.println("Trying to download ivy from " + fullUrl);
            JkUtilsPath.deleteIfExists(path);
            JkHttpRequest.of(fullUrl).downloadFile(path);
            //JkUtilsNet.downloadFile(fullUrl, path);
            if (!checkDownloadOk(path)) {
                throw new UncheckedIOException(new IOException("Ivy download not completed"));
            }
        } catch (UncheckedIOException e) {
            System.err.println("Failed to download ivy from " + fullUrl);
            System.err.println("set environment variable JEKA_REPOS_DOWNLOAD or a property 'jeka.repos.download' " +
                    "such as $JEKA_REPOS_DOWNLOAD/" + IVY_URL_PATH + " pointing to an accessible jar file." );
            throw e;
        }
    }


    static boolean checkDownloadOk(Path path) {
        if (!ivyJarValid(path)) {
            System.err.println("Ivy download not completed.");
            return false;
        }
        System.err.println("Ivy downloaded successfully");
        return true;
    }

    // May be improved by checking checksum
    static boolean ivyJarValid(Path path) {
        return JkUtilsPath.size(path) > 10000;
    }

}