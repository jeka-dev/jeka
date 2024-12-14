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

package dev.jeka.core.tool.builtins.tooling.docker;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.tooling.docker.JkDockerJvmBuild;
import dev.jeka.core.api.tooling.docker.JkDockerNativeBuild;
import dev.jeka.core.api.tooling.nativ.JkNativeCompilation;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.nio.file.Path;
import java.util.function.Consumer;

@JkDoc("Builds and runs image based on project.%n" +
        "This KBean can build JVM and Native (AOT) images from an existing project."
)
public final class DockerKBean extends KBean {

    @JkDoc("Explicit full name of the JVM image to build. It may includes placeholders such as '$version', '$groupId', and '$artifactId'.%n"
            + "If not specified, the image name will be inferred form the project information.")
    public String jvmImageName;

    @JkDoc("Explicit full name of the native image to build. It may includes placeholders such as '$version', '$groupId', and '$artifactId'.%n"
            + "If not specified, the image name will be inferred form the project information.")
    public String nativeImageName;

    @JkDoc("Base image to construct the Docker image.")
    public String jvmBaseImage = JkDockerJvmBuild.BASE_IMAGE;

    @JkDoc("Base image for the native Docker image to build")
    public String nativeBaseImage = "ubuntu:latest";

    @JkDoc("Specifies the policy for creating a non-root user in the native Docker image.")
    public JkDockerBuild.NonRootUserCreationMode nativeNonRootUser = JkDockerBuild.NonRootUserCreationMode.AUTO;

    @JkDoc("Specifies the policy for creating a non-root user in the JVM Docker image.")
    public JkDockerBuild.NonRootUserCreationMode jvmNonRootUser = JkDockerBuild.NonRootUserCreationMode.AUTO;

    /*
     * Handler on the Docker build configuration for customizing built images.
     */
    private final JkConsumers<JkDockerJvmBuild> jvmImageCustomizer = JkConsumers.of();

    private final JkConsumers<JkDockerNativeBuild> nativeImageCustomizer = JkConsumers.of();

    /**
     * Computes the name of the Docker image based on the specified project.
     *
     * @param buildable The JkProject ok baseKbean instance containing the module ID, version, and base directory.
     * @return The computed image name.
     */
    public static String computeImageName(JkBuildable buildable) {
        return computeImageName(buildable.getModuleId(), buildable.getVersion(), buildable.getBaseDir());
    }

    @JkDoc("Builds Docker image in local registry.")
    public void build() {
        JkBuildable buildable = getBuildable(true);
        String imageName = resolveJvmImageName();
        String dirName = "docker-build-" + imageName.replace(':', '#');
        jvmDockerBuild(buildable).buildImage(getOutputDir().resolve(dirName), imageName);
    }

    @JkDoc("Displays info about the Docker image.")
    public void info() {
        String imageName = resolveJvmImageName();
        JkBuildable buildable = getRunbase().getBuildable();
        String buildInfo = jvmDockerBuild(buildable).renderInfo(); // May trigger a compilation to find the main class
        JkLog.info("Image Name        : " + imageName);
        JkLog.info(buildInfo);
    }

    @JkDoc("Builds native Docker image in local registry.")
    public void buildNative() {
        JkBuildable buildable = getBuildable(true);
        String imageName = resolveNativeImageName();
        String dirName = "docker-build-" + imageName.replace(':', '#');
        nativeDockerBuild(buildable).buildImage(getOutputDir().resolve(dirName), imageName);
    }

    @JkDoc("Displays info about the native Docker image.")
    public void infoNative() {
        String imageName = resolveNativeImageName();
        JkBuildable buildable = getRunbase().getBuildable();
        JkLog.info("Image Name        : " + imageName);
        String info = nativeDockerBuild(buildable).renderInfo(); // May trigger a compilation to find the main class
        JkLog.info(info);
    }

    /**
     * Adds a customizer function for customizing the Docker JVM image to build.
     */
    public void customizeJvmImage(Consumer<JkDockerJvmBuild> dockerBuildCustomizer) {
        jvmImageCustomizer.add(dockerBuildCustomizer);
    }

    /**
     * Adds a customizer function for customizing the Dockerbuild that will generate the Native image.
     */
    public void customizeNativeImage(Consumer<JkDockerNativeBuild> dockerBuildCustomizer) {
        nativeImageCustomizer.add(dockerBuildCustomizer);
    }

    /**
     * Returns the resolved name of the built JVM image, substituting placeholders and falling back to a
     * default if no name is explicitly defined.
     */
    public String resolveJvmImageName() {
        JkBuildable buildable = getBuildable(false);
        return !JkUtilsString.isBlank(jvmImageName)?
                resolvePlaceHolder(jvmImageName, buildable)  :
                computeImageName(buildable);
    }

    /**
     * Returns the resolved name of the built native image, substituting placeholders and falling back to a
     * default if no name is explicitly defined.
     */
    public String resolveNativeImageName() {
        JkBuildable buildable = getBuildable(false);
        return !JkUtilsString.isBlank(nativeImageName)
                ? resolvePlaceHolder(nativeImageName, buildable)
                : "native-" + computeImageName(buildable);
    }

    private JkBuildable getBuildable(boolean ensureClassesAreCompiled) {

        // compile java if not already done
        JkBuildable buildable = getRunbase().getBuildable();
        if (!JkPathTree.of(buildable.getClassDir()).containFiles() && ensureClassesAreCompiled) {
            buildable.compileIfNeeded();
        }
        return buildable;
    }



    private JkDockerJvmBuild jvmDockerBuild(JkBuildable buildable) {
        JkDockerJvmBuild dockerBuild = JkDockerJvmBuild.of();
        dockerBuild.setNonRootUserCreationMode(jvmNonRootUser);
        JkLog.verbose("Configure JVM Docker image for %s", buildable);
        dockerBuild.adaptTo(buildable);
        jvmImageCustomizer.accept(dockerBuild);
        return dockerBuild;
    }

    private JkDockerNativeBuild nativeDockerBuild(JkBuildable buildable) {
        NativeKBean nativeKBean = this.load(NativeKBean.class);
        JkLog.verbose("Configure native Docker image for %s", buildable);
        JkNativeCompilation nativeImage =  nativeKBean.nativeImage(buildable);
        JkDockerNativeBuild dockerBuild = JkDockerNativeBuild.of(nativeImage);
        dockerBuild.setBaseImage(nativeBaseImage);
        dockerBuild.setNonRootUserCreationMode(nativeNonRootUser);
        nativeImageCustomizer.accept(dockerBuild);
        return dockerBuild;
    }

    private static String computeImageName(JkModuleId moduleId, JkVersion version, Path baseDir) {
        String name;
        String versionTag = computeVersion(version);
        if (moduleId != null) {
            name = moduleId.toString().replace(":", "-");
        } else {
            name =  baseDir.toAbsolutePath().getFileName().toString();
            if (name.contains("#")) {   // Remote dir may contains # for indicating version
                if (version.isSnapshot()) {
                    return name.replace("#", ":");
                }
                name = JkUtilsString.substringBeforeFirst(name, "#");
            }
        }
        return name + ":" + versionTag;
    }

    private static String computeVersion(JkVersion version) {
        if (version.isSnapshot()) {
            return "latest";
        }
        return version.toString();
    }

    private String resolvePlaceHolder(String candidateName, JkBuildable buildable) {
        return candidateName
                .replace("${groupId}", buildable.getModuleId().getGroup())
                .replace("${artifactId}", buildable.getModuleId().getName())
                .replace("${version}", buildable.getVersion().toString());
    }

}
