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

package dev.jeka.core.api.java;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface JkInternalJarShader {

    void shade(Path mainJar, Set<Path> extraJars, Path outputJar);

    static JkInternalJarShader of(JkRepoSet repoSet) {
        return JkInternalJarShader.Cache.get(repoSet);
    }

    class Cache {

        private static JkInternalJarShader CACHED_INSTANCE;

        private final static String IMPL_CLASS = "dev.jeka.core.api.java.embedded.shade.MavenJarShader";

        private static JkInternalJarShader get(JkRepoSet repos) {
            if (CACHED_INSTANCE != null) {
                return CACHED_INSTANCE;
            }

            JkDependencyResolver dependencyResolver = JkDependencyResolver
                    .of(repos)
                    .setUseFileSystemCache(true);
            JkDependencySet dependencies = JkDependencySet.of()
                    .and("org.slf4j:slf4j-simple:2.0.13")
                    .and("org.apache.maven.plugins:maven-shade-plugin:3.6.0")
                        .withLocalExclusions("org.slf4j:slf4j-simple")
                    .and("org.apache.maven:maven-plugin-api:3.9.8");
            List<Path> classpath = dependencyResolver.resolveFiles(dependencies);

            ClassLoader classLoader = JkInternalChildFirstClassLoader.of(classpath,
                    JkInternalJarShader.class.getClassLoader());
            Class<?> clazz = JkClassLoader.of(classLoader).load(IMPL_CLASS);
            CACHED_INSTANCE = JkUtilsReflect.invokeStaticMethod(clazz, "of");
            return CACHED_INSTANCE;
        }
    }

    class MavenShaderClassLoader extends ClassLoader {}

}
