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
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
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

    @JkDoc("Extra parameters to pass to 'docker run' command while invoking '#runImage' (such as '-p 8080:8080')")
    public String dockerRunParams = "";

    @JkDoc("Explicit full name of the image to build. It may contains a tag to identify the version. %n" +
            "If not set, the image name will be inferred form project info.")
    public String dockerImageName;

    /**
     * Handler on the Docker build configuration for customizing built images.
     */
    private final JkConsumers<JkDockerBuild> customizer = JkConsumers.of();

    // Used only for running image. Not for building.
    private String jvmOptions = "";

    // Used only for running image. Not for building.
    private String programArgs = "";


    @Override
    protected void init() {
        Optional<BaseKBean> optionalSelfAppKBean = getRunbase().find(BaseKBean.class);
        Optional<ProjectKBean> optionalProjectKBean = getRunbase().find(ProjectKBean.class);
       /*
        if (optionalProjectKBean.isPresent() && optionalProjectKBean.isPresent()) {
            throw new IllegalStateException("Both a BaseKBean and ProjectKBean are present in the JkRunbase. " +
                    "Cannot configure for Both. You need to remove one of these KBeans in order to use DockerKBean.");
        }

        */
        optionalSelfAppKBean.ifPresent(this::configureForSelfApp);
        optionalProjectKBean.ifPresent(this::configureForProject);
    }

    @JkDoc("Builds Docker image in local registry.")
    public void build() {
        dockerBuild().buildImage(dockerImageName);
    }

    @JkDoc("Runs Docker image and wait until termination.")
    public void run() {
        JkDocker.assertPresent();
        String containerName = "jeka-" + dockerImageName.replace(':', '-');
        String args = String.format("-it --rm %s-e \"JVM_OPTIONS=%s\" -e \"PROGRAM_ARGS=%s\" "
                        + dockerRunParams + " --name %s %s",
                dockerBuild().portMappingArgs(),
                jvmOptions,
                programArgs,
                containerName,
                dockerImageName);
        JkDocker.execCmdLine("run", args);
    }

    @JkDoc("Displays info about the Docker image.")
    public void info() {
        String buildInfo = dockerBuild().info(); // May trigger a compilation to find the main class
        JkLog.info("Image Name        : " + this.dockerImageName);
        JkLog.info(buildInfo);
    }

    public void customize(Consumer<JkDockerBuild> dockerBuildCustomizer) {
        customizer.add(dockerBuildCustomizer);
    }

    private JkDockerBuild dockerBuild() {
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        customizer.accept(dockerBuild);
        return dockerBuild;
    }

    private void configureForSelfApp(BaseKBean baseKBean) {
        JkLog.verbose("Configure DockerKBean for SelAppKBean %s", baseKBean);
        this.dockerImageName = !JkUtilsString.isBlank(dockerImageName)
                ? dockerImageName
                : computeImageName(baseKBean.getModuleId(), baseKBean.getVersion(), baseKBean.getBaseDir());
        this.jvmOptions = JkUtilsString.nullToEmpty(baseKBean.jvmOptions);
        this.programArgs = JkUtilsString.nullToEmpty(baseKBean.programArgs);

        this.customize(dockerBuild ->  dockerBuild
                .setMainClass(baseKBean.getMainClass())
                .setClasses(baseKBean.getAppClasses())
                .setClasspath(baseKBean.getAppClasspath())
        );
    }

    private void configureForProject(ProjectKBean projectKBean) {
        JkLog.verbose("Configure DockerKBean for ProjectKBean %s", projectKBean.project);
        JkProject project = projectKBean.project;
        this.dockerImageName = !JkUtilsString.isBlank(dockerImageName)
                ? dockerImageName
                : computeImageName(project.getModuleId(), project.getVersion(), project.getBaseDir());
        this.jvmOptions = JkUtilsString.nullToEmpty(projectKBean.run.jvmOptions);
        this.programArgs = JkUtilsString.nullToEmpty(projectKBean.run.programArgs);
        this.customize(dockerBuild -> dockerBuild.adaptTo(project));
    }

    private String computeImageName(JkModuleId moduleId, JkVersion version, Path baseDir) {
        String name;
        if (moduleId != null) {
            name = moduleId.toString().replace(":", "-");
        } else {
            name =  baseDir.toAbsolutePath().getFileName().toString();
        }
        return name + ":" + computeVersion(version);
    }

    private String computeVersion(JkVersion version) {
        if (version.isSnapshot()) {
            return "latest";
        }
        return version.toString();
    }

}
