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

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.java.JkNativeImage;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Docker Builder assistant for creating Docker images that run native applications compiled with GraalVM.<br/>
 *
 * Provides a default Docker build configuration using a multi-stage build process:
 * <ul>
 *   <li>The first stage is the build image, which compiles the JVM application into a native executable.</li>
 *   <li>The native executable is then copied into the production image to serve as the entrypoint.</li>
 * </ul>
 */
public class JkDockerNativeBuild extends JkDockerBuild {

    public enum PopularBaseImage {

        UBUNTU("ubuntu:latest"),

        DISTRO_LESS("gcr.io/distroless/static-debian12:nonroot");

        public final String imageName;

        PopularBaseImage(String imageName) {
            this.imageName = imageName;
        }
    }

    private final JkNativeImage nativeImage;



    private JkDockerNativeBuild(JkNativeImage nativeImage) {
        this.nativeImage = nativeImage;
        this.setBaseImage(PopularBaseImage.UBUNTU.imageName);

        // Create build image
        dockerfileTemplate.moveCursorBefore("FROM ");
        dockerfileTemplate.add("FROM ${buildImage} AS build");
        this.addBuildCopySteps();
        dockerfileTemplate.add("RUN native-image @/argfile").add("");

        // Configure production image
        dockerfileTemplate
            .moveCursorBeforeNonRootUserSwitch()  // we are now in the production image
            .add("COPY ${chownFlag} --from=build /my-app /app/myapp")
            .moveCursorNext()                    // move after 'USER nonroot'
            .add("WORKDIR /app")
            .addEntrypoint("/app/myapp");

        this.addTokenResolver("buildImage", this::buildImage);
        this.addTokenResolver("chownFlag", this::chownFlag);
    }

    public static JkDockerNativeBuild of(JkNativeImage nativeImage) {
        return new JkDockerNativeBuild(nativeImage);
    }

    private String chownFlag() {
        return hasNonRootUserCreation() ? "--chown=nonroot:nonrootgroup" : "";
    }

    private void addBuildCopySteps() {
        String targetBase = "/root/cp";
        List<String> targetPaths = new ArrayList<>();
        List<Path> reverseClasspath = new LinkedList<>(nativeImage.getClasspath());
        Collections.reverse(reverseClasspath);
        for (Path entry : reverseClasspath)  {
            String candidateName = entry.getFileName().toString();
            while (targetPaths.contains(targetBase + "/" +candidateName)) {
                candidateName = "_" + candidateName;
            }
            String targetPath = targetBase + "/" + candidateName;
            targetPaths.add(targetPath);
            dockerfileTemplate.addCopy(entry, targetPath);
        }

        Collections.reverse(targetPaths);
        String classpathString = String.join(":", targetPaths);
        String myAppPath = "/my-app";
        List<String> nativeImageArgs = nativeImage.getNativeImageParams(myAppPath, classpathString);
        if (JkUtilsSystem.IS_WINDOWS) {
            nativeImageArgs = nativeImageArgs.stream()
                    .map(arg -> arg.replace("\\", "/"))
                    .collect(Collectors.toList());
        }
        Path argFile = JkUtilsPath.createTempFile("jeka-native-image-arg-file-", ".txt");
        String argsAsString = String.join(" ", nativeImageArgs);
        for (Path metadatarepoPath : nativeImage.getAotMetadataRepoPaths()) {
            String targetPath = targetBase + "/root/metadata-repo/" + metadatarepoPath.getParent().getFileName() + "/"
                    + metadatarepoPath.getFileName();
           dockerfileTemplate.addCopy(metadatarepoPath, targetPath);
            argsAsString = argsAsString.replace(metadatarepoPath.toString().replace("\\", "/"),
                    targetPath);
        }
        JkPathFile.of(argFile).write(argsAsString);
        dockerfileTemplate.addCopy(argFile, "/argfile");
    }

    private String buildImage() {
        if (JkNativeImage.StaticLink.MUSL == nativeImage.getStaticLinkage()) {
            return "ghcr.io/graalvm/native-image-community:23-muslib";
        } else {
            return "ghcr.io/graalvm/native-image-community:23.0.0";
        }
    }

}
