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

package dev.jeka.core.api.scaffold;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import junit.framework.TestCase;

public class JkScaffoldTest extends TestCase {

    public void testInterpolateLastVersionOf() {
        JkRepoSet repos = JkRepo.ofMavenCentral().toSet();

        String noToken = "azertyuio azer {";
        String result = JkScaffold.interpolateLastVersionOf(noToken, 0, repos);
        assertEquals(noToken, result);

        String springbootTokenToken = "=${lastVersionOf:org.springframework.boot:spring-boot-dependencies}=";
        result = JkScaffold.interpolateLastVersionOf(springbootTokenToken, 0, repos);
        String extractedVersion = result.substring(1, result.length() - 1);
        JkVersion version = JkVersion.of(extractedVersion);
        int major = Integer.parseInt(version.getBlock(0));
        assertTrue(major >= 3);

        String contentWith2tokens = "=${lastVersionOf:org.springframework.boot:spring-boot-dependencies}=" +
                "${lastVersionOf:dev.jeka:jeka-core}d";
        result = JkScaffold.interpolateLastVersionOf(contentWith2tokens, 0, repos);
        assertFalse(result.contains("${"));
        assertTrue(result.length() > extractedVersion.length() + 2);
    }
}