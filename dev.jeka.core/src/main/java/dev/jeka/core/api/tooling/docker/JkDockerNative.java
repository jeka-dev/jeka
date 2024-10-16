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
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class JkDockerNative {

    private final JkNativeImage nativeImage;

    private String baseImage = "ubuntu:latest";

    private boolean useNonrootUser = true;

    private JkDockerNative(JkNativeImage nativeImage) {
        this.nativeImage = nativeImage;
    }

    public static JkDockerNative of(JkNativeImage nativeImage) {
        return new JkDockerNative(nativeImage);
    }

    public String getBaseImage() {
        return baseImage;
    }

    public JkDockerNative setBaseImage(String baseImage) {
        JkUtilsAssert.argument(!JkUtilsString.isBlank(baseImage), "baseImage must not be blank");
        this.baseImage = baseImage;
        return this;
    }

    public JkDockerNative setUseNonrootUser(boolean useNonrootUser) {
        this.useNonrootUser = useNonrootUser;
        return this;
    }

    public JkDockerBuild dockerBuild() {
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        dockerBuild.setUserId(null); // no nonroot user. We manage it manually
        addHeaderOfBuildSection(dockerBuild);

        String targetBase = "/root/cp";
        List<String> targetPaths = new ArrayList<>();
        for (Path entry : nativeImage.getClasspath())  {
            String candidateName = entry.getFileName().toString();
            while (targetPaths.contains(targetBase + "/" +candidateName)) {
                candidateName = "_" + candidateName;
            }
            String targetPath = targetBase + "/" + candidateName;
            targetPaths.add(targetPath);
            dockerBuild.rootSteps.addCopy(entry, targetPath);
        }
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
            dockerBuild.rootSteps.addCopy(metadatarepoPath, targetPath);
            argsAsString = argsAsString.replace(metadatarepoPath.toString().replace("\\", "/"),
                    targetPath);
        }

        JkPathFile.of(argFile).write(argsAsString);
        dockerBuild.rootSteps.addCopy(argFile, "/argfile");

        dockerBuild.rootSteps.add("RUN native-image @/argfile");

        dockerBuild.rootSteps.add("FROM " + baseImage);

        if (useNonrootUser) {
            addNonrootUser(dockerBuild);
        }
        if (useNonrootUser) {
            dockerBuild.rootSteps.add("COPY --chown=nonroot:nonrootgroup --from=build /my-app /app/myapp");
            dockerBuild.rootSteps.add("USER nonroot");
        } else {
            dockerBuild.rootSteps.add("COPY --from=build /my-app /app/myapp");
        }
        dockerBuild.rootSteps.add("WORKDIR /app");
        dockerBuild.rootSteps.add("ENTRYPOINT [\"/app/myapp\"]");
        return dockerBuild;
    }

    private void addHeaderOfBuildSection(JkDockerBuild dockerBuild) {
        dockerBuild.setBaseImage(buildImage() + " AS build");
    }

    private void addNonrootUser(JkDockerBuild dockerBuild) {
        if (baseImage.contains("alpine")) {
            dockerBuild.rootSteps.add(JkDockerBuild.ALPINE_ADD_USER_TEMPLATE
                    .replace("${UID}", "1001")
                    .replace("${GID}", "1002"));
        } else if (baseImage.contains("scratch") || baseImage.contains("distroless")) {
            // These distro does not contains useradd or adduser tool
            // A version of these image exists containing already the nonroot user
        } else {
            dockerBuild.rootSteps.add(JkDockerBuild.UBUNTU_ADD_USER_TEMPLATE
                    .replace("${UID}", "1001")
                    .replace("${GID}", "1002"));
        }
    }

    private String buildImage() {
        if (JkNativeImage.StaticLink.MUSL == nativeImage.getStaticLinkage()) {
            return "ghcr.io/graalvm/native-image-community:23-muslib";
        } else {
            return "ghcr.io/graalvm/native-image-community:23.0.0";
        }
    }

}
