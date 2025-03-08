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

package dev.jeka.core.api.tooling.docker;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.tooling.nativ.JkNativeCompilation;
import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

class JkDockerNativeTest {

    @Test
    void runDefault() throws Exception {

        List<Path> cp = new LinkedList<>();
        cp.add( Paths.get(JkDockerBuildIT.class.getResource("hello-jeka.jar").toURI()) );
        cp.addAll( JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral())
                .resolveFiles("com.google.guava:guava:33.3.1-jre") );

        JkNativeCompilation nativeBuild = JkNativeCompilation.ofClasspath(cp);
        nativeBuild.setStaticLinkage(JkNativeCompilation.StaticLink.MUSL);

        JkDockerNativeBuild dockerBuild = JkDockerNativeBuild.of(nativeBuild);
        dockerBuild.setBaseImage(JkDockerNativeBuild.PopularBaseImage.DISTRO_LESS.imageName);

        System.out.println(dockerBuild.renderInfo());
    }

    @Test
    void distroLess() throws Exception {

        List<Path> cp = JkUtilsIterable.listOf(Paths.get("lib-a"),
                Paths.get("lib-b"), Paths.get("lib-c"),  Paths.get("/toto/lib-c"));
        JkNativeCompilation nativeImage = JkNativeCompilation.ofClasspath(cp);
        JkDockerNativeBuild dockerNative = JkDockerNativeBuild.of(nativeImage);
        dockerNative.setBaseImage(JkDockerNativeBuild.PopularBaseImage.DISTRO_LESS.imageName);
        System.out.println(dockerNative.renderInfo());
    }
}
