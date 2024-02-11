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
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.self.SelfKBean;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

@JkDoc("Builds and runs image based on project or 'jeka-src' (Requires a Docker client)")
public final class DockerKBean extends KBean {

    @JkDoc("Extra parameters to pass to 'docker run' command while invoking '#runImage' (such as '-p 8080:8080')")
    public String dockerRunParams = "";

    @JkDoc("Explicit full name of the image to build. It may contains a tag to identify the version.")
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
        Optional<SelfKBean> optionalSelfAppKBean = getRunbase().findInstanceOf(SelfKBean.class);
        Optional<ProjectKBean> optionalProjectKBean = getRunbase().find(ProjectKBean.class);
       /*
        if (optionalProjectKBean.isPresent() && optionalProjectKBean.isPresent()) {
            throw new IllegalStateException("Both a SelfKBean and ProjectKBean are present in the JkRunbase. " +
                    "Cannot configure for Both. You need to remove one of these KBeans in order to use DockerKBean.");
        }

        */
        optionalSelfAppKBean.ifPresent(this::configureForSelfApp);
        optionalProjectKBean.ifPresent(this::configureForProject);
    }

    @JkDoc("Build Docker image in local registry.")
    public void build() {
        dockerBuild().buildImage(dockerImageName);
    }

    @JkDoc("Run Docker image and wait until termination.")
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

    private void configureForSelfApp(SelfKBean selfKBean) {
        JkLog.verbose("Configure DockerKBean for SelAppKBean " + selfKBean);
        this.dockerImageName = !JkUtilsString.isBlank(dockerImageName)
                ? dockerImageName
                : computeImageName(selfKBean.getModuleId(), selfKBean.getVersion(), selfKBean.getBaseDir());
        this.jvmOptions = JkUtilsString.nullToEmpty(selfKBean.jvmOptions);
        this.programArgs = JkUtilsString.nullToEmpty(selfKBean.programArgs);

        this.customize(dockerBuild ->  dockerBuild
                .setMainClass(selfKBean.getMainClass())
                .setClasses(selfKBean.getAppClasses())
                .setClasspath(selfKBean.getAppClasspath())
        );
    }

    private void configureForProject(ProjectKBean projectKBean) {
        JkLog.verbose("Configure DockerKBean for ProjectKBean " + projectKBean.project);
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
