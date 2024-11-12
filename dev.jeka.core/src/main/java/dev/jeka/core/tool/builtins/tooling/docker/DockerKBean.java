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
import dev.jeka.core.api.java.JkNativeImage;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.tooling.docker.JkDockerJvmBuild;
import dev.jeka.core.api.tooling.docker.JkDockerNative;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@JkDoc("Builds and runs image based on project.%n" +
        "This KBean can build JVM and Native (AOT) images from an existing project."
)
public final class DockerKBean extends KBean {

    @JkDoc("Explicit full name of the image to build. It may contains a tag to identify the version. %n" +
            "If not set, the image name will be inferred form project info.")
    public String jvmImageName;

    @JkDoc("Explicit full name of the native image to build. See imageName.")
    public String nativeImageName;

    @JkDoc("Base image to construct the Docker image.")
    public String jvmBaseImage = JkDockerJvmBuild.BASE_IMAGE;

    @JkDoc("Base image for the native Docker image to build")
    public String nativeBaseImage = "ubuntu:latest";

    @JkDoc("If true, the image will be configured for running with the 'nonroot' user.")
    public boolean nonrootUser = true;

    /*
     * Handler on the Docker build configuration for customizing built images.
     */
    private final JkConsumers<JkDockerJvmBuild> jvmImageCustomizer = JkConsumers.of();

    private final JkConsumers<JkDockerBuild.StepContainer> nativeImageCustomizer = JkConsumers.of();

    /**
     * Computes the name of the Docker image based on the specified project.
     *
     * @param buildable The JkProject ok baseKbean instance containing the module ID, version, and base directory.
     * @return The computed image name.
     */
    public static String computeImageName(JkBuildable buildable) {
        return computeImageName(buildable.getModuleId(), buildable.getVersion(), buildable.getBaseDir());
    }

    @Override
    protected void init() {
        JkBuildable buildable = getRunbase().getBuildable();
        JkLog.verbose("Configure docker for %", buildable);
        this.configureFor(buildable);
    }

    @JkDoc("Builds Docker image in local registry.")
    public void build() {
        String dirName = "docker-build-" + jvmImageName.replace(':', '#');
        jvmDockerBuild().buildImage(getOutputDir().resolve(dirName), jvmImageName);
    }

    @JkDoc("Displays info about the Docker image.")
    public void info() {
        String buildInfo = jvmDockerBuild().info(this.jvmImageName); // May trigger a compilation to find the main class
        JkLog.info("Image Name        : " + this.jvmImageName);
        JkLog.info(buildInfo);
    }

    @JkDoc("Builds native Docker image in local registry.")
    public void buildNative() {

        // compile java if not already done
       JkBuildable buildable = getRunbase().getBuildable();
        if (!JkPathTree.of(buildable.getClassDir()).containFiles()) {
            buildable.compileIfNeeded();
        }
        String dirName = "docker-build-native-" + jvmImageName.replace(':', '#');
        nativeDockerBuild(buildable).buildImage(getOutputDir().resolve(dirName), nativeImageName);
    }

    @JkDoc("Displays info about the native Docker image.")
    public void infoNative() {
        JkBuildable buildable = getRunbase().getBuildable();
        String buildInfo = nativeDockerBuild(buildable).render(); // May trigger a compilation to find the main class
        JkLog.info("Image Name        : " + this.jvmImageName);
        JkLog.info("Dockerfile : ");
        JkLog.info("-------------------------------------------------");
        JkLog.info(buildInfo);
        JkLog.info("-------------------------------------------------");
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
    public void customizeNativeImage(Consumer<JkDockerBuild.StepContainer> dockerBuildCustomizer) {
        nativeImageCustomizer.add(dockerBuildCustomizer);
    }

    private JkDockerJvmBuild jvmDockerBuild() {
        JkDockerJvmBuild dockerBuild = JkDockerJvmBuild.of();
        dockerBuild.setBaseImage(jvmBaseImage);
        jvmImageCustomizer.accept(dockerBuild);
        return dockerBuild;
    }

    private JkDockerBuild nativeDockerBuild(JkBuildable buildable) {
        NativeKBean nativeKBean = this.load(NativeKBean.class);
        JkNativeImage nativeImage =  nativeKBean.nativeImage(buildable);

        JkDockerBuild result = JkDockerNative.of(nativeImage)
                .setBaseImage(nativeBaseImage)
                .setUseNonrootUser(nonrootUser)
                .dockerBuild();
        nativeImageCustomizer.accept(result.rootSteps);
        return result;
    }

    private void configureFor(JkBuildable buildable) {
        JkLog.verbose("Configure DockerKBean for ProjectKBean %s", buildable);
        this.jvmImageName = !JkUtilsString.isBlank(jvmImageName)
                ? jvmImageName
                : computeImageName(buildable);
        this.nativeImageName = !JkUtilsString.isBlank(nativeImageName)
                ? nativeImageName
                : "native-" + computeImageName(buildable);
        this.customizeJvmImage(dockerBuild -> dockerBuild.adaptTo(buildable));
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

}
