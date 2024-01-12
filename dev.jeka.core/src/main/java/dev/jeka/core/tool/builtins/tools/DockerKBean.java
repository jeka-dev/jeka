package dev.jeka.core.tool.builtins.tools;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.self.SelfAppKBean;

import java.util.Optional;

@JkDoc("Build and run image based on SelfAppKBean or ProjectKBean present on the runtime. " +
        "A running Docker daemon is mandatory to use this KBean.")
public class DockerKBean extends KBean {

    @JkDoc("Extra parameters to pass to 'docker run' command while invoking '#runImage' (such as '-p 8080:8080')")
    public String dockerRunParams = "";

    @JkDoc("Explicit full name of the image to build. It may contains a tag to identify the version.")
    public String dockerImageName;

    /**
     * Handler on the Docker build configuration for customizing built images.
     */
    @JkDoc(hide = true)
    public final JkDockerBuild dockerBuild = JkDockerBuild.of();

    // Used only for running image. Not for building.
    private String jvmOptions = "";

    // Used only for running image. Not for building.
    private String programArgs = "";

    @Override
    protected void init() {
        Optional<SelfAppKBean> optionalSelfAppKBean = getRuntime().findInstanceOf(SelfAppKBean.class);
        Optional<ProjectKBean> optionalProjectKBean = getRuntime().find(ProjectKBean.class);
       /*
        if (optionalProjectKBean.isPresent() && optionalProjectKBean.isPresent()) {
            throw new IllegalStateException("Both a SelfAppKBean and ProjectKBean are present in the JkRuntime. " +
                    "Cannot configure for Both. You need to remove one of these KBeans in order to use DockerKBean.");
        }

        */
        optionalSelfAppKBean.ifPresent(this::configureForSelfApp);
        optionalProjectKBean.ifPresent(this::configureForProject);
    }

    @JkDoc("Build Docker image in local registry.")
    public void build() {
        dockerBuild.buildImage(dockerImageName);
    }

    @JkDoc("Run Docker image and wait until termination.")
    public void run() {
        String containerName = "jeka-" + dockerImageName;
        JkDocker.run("-it --rm %s-e \"JVM_OPTIONS=%s\" -e \"PROGRAM_ARGS=%s\" " + dockerRunParams + " --name %s %s",
                dockerBuild.portMappingArgs(),
                jvmOptions,
                programArgs,
                containerName,
                dockerImageName);
    }

    @JkDoc("Displays info about the Docker image.")
    public void info() {
        JkLog.info("Image Name       : " + this.dockerImageName);
        JkLog.info(this.dockerBuild.info());
    }

    private void configureForSelfApp(SelfAppKBean selfAppKBean) {
        JkLog.info("Configure DockerKBean for SelAppKBean " + selfAppKBean);
        final String effectiveName = !JkUtilsString.isBlank(dockerImageName)
                ? dockerImageName
                : selfAppKBean.getBaseDirName();
        this.dockerImageName = effectiveName;
        this.jvmOptions = selfAppKBean.jvmOptions;
        this.programArgs = selfAppKBean.programArgs;
        this.dockerBuild
                .setClasses(selfAppKBean.classTree())
                .setClasspath(selfAppKBean.appClasspath())
                .setMainClass(selfAppKBean.actualMainClass());
    }

    private void configureForProject(ProjectKBean projectKBean) {
        JkLog.info("Configure DockerKBean for ProjectKBean " + projectKBean.project);
        JkProject project = projectKBean.project;
        final String effectiveName = !JkUtilsString.isBlank(dockerImageName)
                ? dockerImageName
                : project.getBaseDir().toAbsolutePath().getFileName().toString();
        this.dockerImageName = effectiveName;
        this.jvmOptions = projectKBean.run.jvmOptions;
        this.programArgs = projectKBean.run.programArgs;
        this.dockerBuild
                .setClasses(JkPathTree.of(project.compilation.layout.resolveClassDir()))
                .setClasspath(project.packaging.resolveRuntimeDependenciesAsFiles())
                .setMainClass(project.getMainClass());
    }

}
