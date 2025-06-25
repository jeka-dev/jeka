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

package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.tooling.maven.JkPom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JkPomIT {

    @Test
    void withResolvedProperties_andNestedPomImport() {
        JkRepoSet repoSet = JkRepo.ofMavenCentral().toSet();
        JkCoordinateFileProxy fileProxy = JkCoordinateFileProxy.of(repoSet,
                "org.springframework.boot:spring-boot-dependencies:3.5.3@pom");
        JkPom pom = JkPom.of(fileProxy.get());
        JkVersionProvider versionProvider = pom.withResolvedProperties().getVersionProvider(repoSet);
        assertEquals("1.12.2", versionProvider.getVersionOf("org.junit.platform:junit-platform-launcher"));
    }
}
