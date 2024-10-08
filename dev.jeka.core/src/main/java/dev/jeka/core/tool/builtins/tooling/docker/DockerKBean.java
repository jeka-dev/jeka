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

    @JkDoc("Explicit full name of the image to build. It may contains a tag to identify the version. %n" +
            "If not set, the image name will be inferred form project info.")
    public String imageName;

    public final RunOptions run = new RunOptions();

    public final BuildOptions build = new BuildOptions();

    private String lastRunningContainerName;

    /**
     * Computes the name of the Docker image based on the specified project.
     *
     * @param project The JkProject instance containing the module ID, version, and base directory.
     * @return The computed image name.
     */
    public static String computeImageName(JkProject project) {
        return computeImageName(project.getModuleId(), project.getVersion(), project.getBaseDir());
    }

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
        String dirName = "docker-build-" + imageName.replace(':', '#');
        dockerBuild().buildImage(getOutputDir().resolve(dirName), imageName);
    }

    @JkDoc("Runs Docker image and wait until termination.")
    public void run() {
        JkDocker.assertPresent();
        this.lastRunningContainerName = "jeka-" + imageName.replace(':', '-');
        String runOptions = JkUtilsString.isBlank(run.options) ? "" : run.options.trim() + " ";
        String args = String.format("%s--rm %s-e \"JAVA_TOOL_OPTIONS=%s\" --name %s %s %s",
                runOptions,
                dockerBuild().portMappingArgs(),
                run.jvmOptions,
                this.lastRunningContainerName,
                imageName,
                run.programArgs);
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



    /**
     * Retrieves the name of the last or current running container, launched using {@link #run()} method.
     */
    public String getLastRunningContainerName() {
        return lastRunningContainerName;
    }

    private JkDockerJvmBuild dockerBuild() {
        JkDockerJvmBuild dockerBuild = JkDockerJvmBuild.of();
        dockerBuild.setBaseImage(build.baseImage);
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
                : computeImageName(project);
        this.customize(dockerBuild -> dockerBuild.adaptTo(project));
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

    public static class RunOptions {

        @JkDoc("Java program arguments to pass to the Java application when run with 'docker: run'")
        public String programArgs = "";

        @JkDoc("JVM options to pass to the Java application when run with 'docker: run'")
        public String jvmOptions = "";

        @JkDoc("Space separated options to pass 'docker run' command, as '--interactive --tty'")
        public String options = "";
    }

    public static class BuildOptions {

        @JkDoc("Base image to construct the Docker image.")
        public String baseImage = JkDockerJvmBuild.BASE_IMAGE;

    }

}
