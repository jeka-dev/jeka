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

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;

public class JkJarPackerTest {

    @Test
    @Ignore // Classloader issue prevents to run it in the test suite.
    public void testShade() {
        Path vincerDomJar = JkCoordinateFileProxy.of(JkRepo.ofMavenCentral().toSet(),
                "com.github.djeang:vincer-dom:1.4.1").get();
        Path jsonSimpleJar = JkCoordinateFileProxy.of(JkRepo.ofMavenCentral().toSet(),
                "com.googlecode.json-simple:json-simple:1.1.1").get();
        Path resultJar = JkUtilsPath.createTempFile("jk-test-", ".jar");
        JkUtilsPath.deleteIfExists(resultJar);
        JkJarPacker.makeShadeJar(vincerDomJar, jsonSimpleJar, resultJar);
    }
}