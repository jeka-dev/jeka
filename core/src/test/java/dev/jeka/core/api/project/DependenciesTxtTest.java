/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DependenciesTxtTest {

    @Test
    void readFile_ok()  {
        Path path = JkUtilsPath.getResourceAsPath(DependenciesTxtTest.class, "dependencies-ini.txt");
        DependenciesTxt dependenciesTxt = DependenciesTxt.parse(path);
        JkDependencySet compileDeps = dependenciesTxt.getDependencies(DependenciesTxt.COMPILE);
        JkDependencySet runtimeDeps = dependenciesTxt.getDependencies(DependenciesTxt.RUNTIME);
        JkDependencySet testDeps = dependenciesTxt.getDependencies(DependenciesTxt.TEST);

        // All compile deps are read
        assertEquals(4, compileDeps.getEntries().size());  // 3 + 1 from compile-only
        assertEquals(2, runtimeDeps.getEntries().size());  // includes bom
        assertEquals(2, testDeps.getEntries().size());

        // 'dev.jeka:jeka-core:0.11.24' interpreted as JkCoordinateDependencies
        assertEquals(JkCoordinateDependency.class, compileDeps.getEntries().get(0).getClass());

        // '../libs/my-lib.jar' interpreted as JkFileSystem dependencies
        assertEquals(JkFileSystemDependency.class, compileDeps.getEntries().get(2).getClass());

        JkVersionProvider versionProvider = dependenciesTxt.getVersionProvider();
        assertEquals(2, versionProvider.getBoms().size());
    }



}