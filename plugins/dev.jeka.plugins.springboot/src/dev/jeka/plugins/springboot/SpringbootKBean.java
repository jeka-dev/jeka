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

package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.JkNativeImage;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;


/*
 *  Inspired from :
 *     - https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-tools/spring-boot-maven-plugin/src/main/java/org/springframework/boot/maven
 */
@JkDoc(
        "Adapt projectKBean or baseKBean for Spring-Boot.\n" +
        "- Produce bootable jars\n" +
        "- Customize .war file for projectKBean\n" +
        "- Adapt scaffold\n" +
        "The project or the baseApp is automatically configured during this KBean initialization. "
)
public final class SpringbootKBean extends KBean {

    //@JkDoc("Version of Spring Boot version used to resolve dependency versions.")
    //@JkDepSuggest(versionOnly = true, hint = "org.springframework.boot:spring-boot-dependencies:")
    //private String springbootVersion;

    @JkDoc("If true, create a bootable jar artifact.")
    private final boolean createBootJar = true;

    @JkDoc("If true, create original jar artifact for publication (jar without embedded dependencies")
    private boolean createOriginalJar;

    @JkDoc("If true, create a .war filed.")
    private boolean createWarFile;

    @JkDoc("Specific Spring repo where to download spring artifacts. Not needed if you use official release.")
    private JkSpringRepo springRepo;

    @JkDoc("Options related to native image creation")
    private NativeOptions nativeOps = new NativeOptions();

    @Override
    protected void init() {

        Optional<ProjectKBean> optionalProjectKBean = getRunbase().find(ProjectKBean.class);
        Optional<BaseKBean> optionalBaseKBean = getRunbase().find(BaseKBean.class);

        // Use Project KBean if Project KBean is present or if BaseKBean is absent
        if (optionalProjectKBean.isPresent() || !optionalBaseKBean.isPresent()) {
            customizeProjectKBean(load(ProjectKBean.class));

            // Otherwise, force use BaseKBean
        } else {
            customizeBaseKBean(load(BaseKBean.class));
        }

        // Configure Docker KBean to add port mapping on run
        Optional<DockerKBean> optionalDockerKBean = getRunbase().find(DockerKBean.class);
        optionalDockerKBean.ifPresent(dockerKBean -> dockerKBean.customize(dockerBuild -> {
            if (dockerBuild.getExposedPorts().isEmpty()) {
                dockerBuild.setExposedPorts(8080);
            }
        }));

    }

    @JkDoc("Provides info about this plugin configuration")
    public void info() {
 //       JkLog.info("Spring-Boot version : " + springbootVersion);
        JkLog.info("Create Bootable Jar : " + this.createBootJar);
        JkLog.info("Create original Jar : " + this.createOriginalJar);
        JkLog.info("Create .war file : " + this.createWarFile);
    }

    @JkDoc("Create native executable springboot application")
    public void makeNative() {
        Path execPath = JkUtilsString.isBlank(nativeOps.execName) ?
                defaultNaliveImagePath() :
                getOutputDir().resolve(nativeOps.execName);
        makeNative(execPath);
    }

    @JkDoc("Create a docker image running a native executable of the springboot app")
    public void makeNativeDocker() {
        String execName = defaultNativeExecName() + "-for-container";
        JkDocker.execJeka("springboot:",  "makeNative",
                "nativeOps.execName=" + execName);
        JkDockerBuild dockerBuild = dockerBuildForNative(execName);
        String imageName = DockerKBean.computeImageName(load(ProjectKBean.class).project);
        dockerBuild.buildImage(imageName);
    }

    @JkDoc("Displays the DockerBuild file used to create Docker native image")
    public void renderDockerBuild() {
        String execName = defaultNativeExecName() + "-for-container";
        JkDockerBuild dockerBuild = dockerBuildForNative(execName);
        System.out.println(dockerBuild.render());
    }

    private JkDockerBuild dockerBuildForNative(String execName) {
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        dockerBuild.nonRootSteps
                .addNonRootMkdirs("app", "workdir")
                .addCopy(getOutputDir().resolve(execName), "/app/" + execName)
                .add("WORKDIR /workdir")
                .add("ENTRYPOINT [\"/app/" + execName + "\"]");
        this.nativeOps.dockerImageCustomizers.accept(dockerBuild);
        return dockerBuild;
    }

    private void makeNative(Path execPath) {

        JkProject project = load(ProjectKBean.class).project;
        JkLog.startTask("process-springboot-aot");
        new NativeMaker(project).prepareAot();
        JkLog.endTask();

        // compile
        JkLog.startTask("compile-generated-sources");
        project.compilation.layout.addSource("jeka-output/spring-aot/generated-sources");
        project.compilation.layout.addResource(project.getOutputDir().resolve("spring-aot/generated-resources"));
        Path generatedClassesDir = project.getOutputDir().resolve("spring-aot/generated-classes/");
        project.compilation.dependencies.add(generatedClassesDir);
        project.compilation.run();
        JkPathTree.of(generatedClassesDir).copyTo(project.compilation.layout.resolveClassDir());
        JkLog.endTask();

        // create native image
        JkPathSequence pathSequence = JkPathSequence.of(project.compilation.layout.resolveClassDir())
                .and(project.packaging.resolveRuntimeDependenciesAsFiles());
        JkNativeImage nativeImage = JkNativeImage.ofClasspath(pathSequence.getEntries());

        // -- set reachability metadata info
        nativeImage.reachabilityMetadata.setDependencies(() ->
                project.packaging.resolveRuntimeDependencies().getDependencyTree().getDescendantModuleCoordinates());
        nativeImage.reachabilityMetadata.setExtractDir(
                project.getOutputDir().resolve("graalvm-reachability-metadata-repo"));

        nativeImage.make(execPath);
    }

    private String defaultNativeExecName() {
        JkProject project = load(ProjectKBean.class).project;
        String exeFilename = project.artifactLocator.getMainArtifactPath().getFileName().toString();
        return JkUtilsString.substringBeforeLast(exeFilename, ".jar");
    }

    private Path defaultNaliveImagePath() {
        return getOutputDir().resolve(defaultNativeExecName());
    }

    private void customizeProjectKBean(ProjectKBean projectKBean) {

        // Customize scaffold
        projectKBean.getProjectScaffold().addCustomizer(SpringbootScaffold::customize);

        JkSpringbootProject springbootProject = JkSpringbootProject.of(projectKBean.project)
                .configure(this.createBootJar, this.createWarFile, this.createOriginalJar);
        /*
        if (springbootVersion != null) {
            springbootProject.includeParentBom(springbootVersion);
        }

         */
        if (springRepo != null) {
            springbootProject.addSpringRepo(springRepo);
        }
    }

    private void customizeBaseKBean(BaseKBean baseKBean) {

        // customize scaffold
        baseKBean.getBaseScaffold().addCustomizer(SpringbootScaffold::customize);

        baseKBean.setMainClass(BaseKBean.AUTO_FIND_MAIN_CLASS);
        baseKBean.setMainClassFinder(() -> JkSpringbootJars.findMainClassName(
                getBaseDir().resolve(JkConstants.JEKA_SRC_CLASSES_DIR)));

        baseKBean.setJarMaker(path -> JkSpringbootJars.createBootJar(
                baseKBean.getAppClasses(),
                baseKBean.getAppLibs(),
                getRunbase().getDependencyResolver().getRepos(),
                path,
                baseKBean.getManifest())
        );
    }

    public static class NativeOptions {

        private NativeOptions() {
        }

        @JkDoc("Name of the native executable to create. A default relevant name will be peaked if left to null.")
        public String execName;

        public String dockerImageName;

        /**
         * Allows to customize generated Docker image for native exec.
         */
        public final JkConsumers<JkDockerBuild> dockerImageCustomizers = JkConsumers.of();



    }



}
