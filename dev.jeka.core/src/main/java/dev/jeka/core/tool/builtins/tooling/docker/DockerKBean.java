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
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.tooling.docker.JkDockerJvmBuild;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@JkDoc("Builds and runs image based on project or 'jeka-src' (Requires a Docker client)")
public final class DockerKBean extends KBean {

    @JkDoc("Program arguments to pass to 'docker run' command while invoking 'run' (such as '-p 8080:8080')")
    public String programArgs = "";

    @JkDoc("JVM options to pass to 'build' or 'run' command")
    public String jvmOptions = "";

    @JkDoc("Explicit full name of the image to build. It may contains a tag to identify the version. %n" +
            "If not set, the image name will be inferred form project info.")
    public String imageName;

    @JkDoc("Base image to construct the Docker image.")
    public String baseImage = JkDockerJvmBuild.BASE_IMAGE;

    @JkDoc("Run docker with '-it' option")
    public boolean it = true;

    /**
     * Handler on the Docker build configuration for customizing built images.
     */
    private final JkConsumers<JkDockerJvmBuild> customizer = JkConsumers.of();

    @Override
    protected void init() {
        Optional<BaseKBean> optionalBaseKBean = getRunbase().find(BaseKBean.class);;
        if (!optionalBaseKBean.isPresent()) {
            JkLog.verbose("Configure docker for project");
            this.configureForProject(this.load(ProjectKBean.class));
        } else {
            JkLog.verbose("Configure docker for base");
            this.configureForBase(optionalBaseKBean.get());
        }

    }

    @JkDoc("Builds Docker image in local registry.")
    public void build() {
        dockerBuild().buildImage(imageName);
    }

    @JkDoc("Runs Docker image and wait until termination.")
    public void run() {
        JkDocker.assertPresent();
        String containerName = "jeka-" + imageName.replace(':', '-');
        String itOption = it ? "-it " : "";
        String args = String.format("%s--rm %s-e \"JVM_OPTIONS=%s\" -e \"PROGRAM_ARGS=%s\" "
                        +  " --name %s %s",
                itOption,
                dockerBuild().portMappingArgs(),
                jvmOptions,
                programArgs,
                containerName,
                imageName);
        JkDocker.execCmdLine("run", args);
    }

    @JkDoc("Displays info about the Docker image.")
    public void info() {
        String buildInfo = dockerBuild().info(this.imageName); // May trigger a compilation to find the main class
        JkLog.info("Image Name        : " + this.imageName);
        JkLog.info(buildInfo);
    }

    public void customize(Consumer<JkDockerJvmBuild> dockerBuildCustomizer) {
        customizer.add(dockerBuildCustomizer);
    }

    private JkDockerJvmBuild dockerBuild() {
        JkDockerJvmBuild dockerBuild = JkDockerJvmBuild.of();
        dockerBuild.setBaseImage(this.baseImage);
        customizer.accept(dockerBuild);
        return dockerBuild;
    }

    private void configureForBase(BaseKBean baseKBean) {
        JkLog.verbose("Configure DockerKBean for Base %s", baseKBean);
        this.imageName = !JkUtilsString.isBlank(imageName)
                ? imageName
                : computeImageName(baseKBean.getModuleId(), baseKBean.getVersion(), baseKBean.getBaseDir());

        this.customize(dockerBuild ->  dockerBuild
                .setMainClass(baseKBean.getMainClass())
                .setClasses(baseKBean.getAppClasses())
                .setClasspath(baseKBean.getAppClasspath())
        );
    }

    private void configureForProject(ProjectKBean projectKBean) {
        JkLog.verbose("Configure DockerKBean for ProjectKBean %s", projectKBean.project);
        JkProject project = projectKBean.project;
        this.imageName = !JkUtilsString.isBlank(imageName)
                ? imageName
                : computeImageName(project.getModuleId(), project.getVersion(), project.getBaseDir());
        this.customize(dockerBuild -> dockerBuild.adaptTo(project));
    }

    private String computeImageName(JkModuleId moduleId, JkVersion version, Path baseDir) {
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

    private String computeVersion(JkVersion version) {
        if (version.isSnapshot()) {
            return "latest";
        }
        return version.toString();
    }

}
