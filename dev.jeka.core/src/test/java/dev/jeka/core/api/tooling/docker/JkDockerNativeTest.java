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

import dev.jeka.core.api.java.JkNativeImage;
import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class JkDockerNativeTest {

    @Test
    public void runDefault() throws Exception {
        List<Path> cp = JkUtilsIterable.listOf(Paths.get("lib-a"),
                Paths.get("lib-b"), Paths.get("lib-c"),  Paths.get("/toto/lib-c"));
        JkNativeImage nativeImage = JkNativeImage.ofClasspath(cp);
        JkDockerNativeBuild dockerNative = JkDockerNativeBuild.of(nativeImage);
        System.out.println(dockerNative.renderInfo());
    }

    @Test
    public void distroless() throws Exception {
        List<Path> cp = JkUtilsIterable.listOf(Paths.get("lib-a"),
                Paths.get("lib-b"), Paths.get("lib-c"),  Paths.get("/toto/lib-c"));
        JkNativeImage nativeImage = JkNativeImage.ofClasspath(cp);
        JkDockerNativeBuild dockerNative = JkDockerNativeBuild.of(nativeImage);
        dockerNative.setBaseImage(JkDockerNativeBuild.PopularBaseImage.DISTRO_LESS.imageName);
        System.out.println(dockerNative.renderInfo());
    }
}
