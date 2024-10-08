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

import dev.jeka.core.api.file.*;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkNativeImage;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


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
        makeNative(nativeExecPath(), this.nativeOps.staticLinkage);
    }

    @JkDoc("Create a docker image running a native executable of the springboot app")
    public void makeNativeDocker() {
        JkDocker.assertPresent();;
        final Path execPath;
        if (JkUtilsSystem.IS_LINUX) {
            execPath = nativeExecPath();
            if (!Files.exists(execPath)) {
                makeNative(execPath, this.nativeOps.dockerImageStaticLinkage);
            }
        } else {
            JkLog.startTask("Creating native image using Docker");
            Path projectDirForLinux = this.getOutputDir().resolve("project-for-container");
            if (Files.exists(projectDirForLinux)) {
                JkPathTree.of(projectDirForLinux).deleteRoot();
            }
            JkPathTree pathTree = JkPathTree.of(this.getBaseDir()).andMatching(false,
                      "jeka-output/**/*")
                    .andMatcher(path -> !path.toString().startsWith("."));
            if (nativeOps.hasCopyExcludePatterns()) {
                pathTree = pathTree.andMatcher(JkPathMatcher.of(false, nativeOps.copyExcludePatterns()));
            }

            // We need to copy the project in another dir and apply a dos2Unix transformation
            // Then, we can invoke docker on this directory
            if (JkUtilsSystem.IS_WINDOWS) {
                pathTree.stream()
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            Path relativePath = this.getBaseDir().relativize(path);
                            Path targetPath = projectDirForLinux.resolve(relativePath);
                            JkUtilsPath.createDirectories(targetPath.getParent());
                            try {
                                JkPathFile.of(path).dos2Unix(targetPath);
                            } catch (Exception e) {
                                JkLog.warn("Error while dos2unix file %s. Just copy as is.", path);
                                JkUtilsPath.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        });
            } else {
                pathTree.copyTo(projectDirForLinux);
            }
            // Invoke native compilation via a container
            String jekaVersion = Optional.ofNullable(this.nativeOps.jekaVersionInDocker).orElse(JkInfo.getJekaVersion());
            JkDocker.prepareExecJeka(projectDirForLinux,"-Djeka.version=" + jekaVersion
                    , "springboot:",  "makeNative")
                    .addParams("nativeOps.staticLinkage=" + this.nativeOps.dockerImageStaticLinkage)
                    .addParamsIf(nativeOps.hasAotProfile(), "nativeOps.aotProfiles=" + nativeOps.aotProfiles)
                    .addParamsIf(JkLog.isVerbose(), "--verbose")
                    .addParamsIf(JkLog.isDebug(), "--debug")
                    .exec();
            execPath = projectDirForLinux.resolve("jeka-output").resolve(nativeExecName());
            JkLog.endTask();
        }

        // Create image using the created native exec
        JkDockerBuild dockerBuild = dockerBuildForNative(execPath);
        String imageName = DockerKBean.computeImageName(load(ProjectKBean.class).project);
        dockerBuild.buildImage(imageName);
    }

    @JkDoc("Displays the DockerBuild file used to create Docker native image")
    public void renderDockerBuild() {
        JkDockerBuild dockerBuild = dockerBuildForNative(nativeExecPath());
        System.out.println(dockerBuild.render());
    }

    private List<Path> generateAotEnrichment(JkProject project) {
        JkLog.startTask("process-springboot-aot");
        AotEnricher aotEnricher = AotEnricher.of(project);
        if (nativeOps.hasAotProfile()) {
            aotEnricher.profiles.addAll(Arrays.asList(nativeOps.aotProfiles()));
        }
        aotEnricher.generate();

        List<Path> classpath = new LinkedList<>(aotEnricher.getClasspath());
        classpath.add(aotEnricher.getGeneratedClassesDir());
        Path compiledGeneratedSources = project.getOutputDir()
                .resolve("spring-aot/compiled-generated-sources");
        JkJavaCompileSpec compileSpec = JkJavaCompileSpec.of()
                .setSources(JkPathTreeSet.ofRoots(aotEnricher.getGeneratedSourcesDir()))
                .setClasspath(classpath)
                .setOutputDir(compiledGeneratedSources);
        JkUtilsAssert.state(project.compilerToolChain.compile(compileSpec),
                "Error while compiling classes generated for AOT");
        JkLog.endTask();

        List<Path> result = new LinkedList<>();
        result.add(aotEnricher.getGeneratedClassesDir());
        result.add(aotEnricher.getGeneratedResourcesDir());
        result.add(compiledGeneratedSources);
        return result;
    }

    private JkDockerBuild dockerBuildForNative(Path execPath) {
        JkDockerBuild dockerBuild = JkDockerBuild.of();
        String nativeExecName = nativeExecName();
        dockerBuild.setBaseImage("ubuntu:latest");  // fail with alpine cause the generated image is dynamically linked to glibc
        dockerBuild.rootSteps
                .addNonRootMkdirs("app", "workdir");
        dockerBuild.nonRootSteps
                .addCopyNonRoot(execPath, "/app/" + nativeExecName)
                .add("WORKDIR /workdir")
                .add("ENTRYPOINT [\"/app/" + nativeExecName + "\"]");
        this.nativeOps.dockerImageCustomizers.accept(dockerBuild);
        return dockerBuild;
    }

    private void makeNative(Path execPath, JkNativeImage.StaticLinkage linkage) {

        JkProject project = load(ProjectKBean.class).project;
        JkLog.startTask("process-springboot-aot");
        AotEnricher aotEnricher = AotEnricher.of(project);
        if (nativeOps.hasAotProfile()) {
            aotEnricher.profiles.addAll(Arrays.asList(nativeOps.aotProfiles()));
        }
        aotEnricher.generate();

        List<Path> classpath = new LinkedList<>(aotEnricher.getClasspath());
        classpath.add(aotEnricher.getGeneratedClassesDir());
        Path compiledGeneratedSources = project.getOutputDir()
                .resolve("jeka-output/spring-aot/compiled-generated-sources");
        JkJavaCompileSpec compileSpec = JkJavaCompileSpec.of()
                        .setSources(JkPathTreeSet.ofRoots(aotEnricher.getGeneratedSourcesDir()))
                        .setClasspath(classpath)
                        .setOutputDir(compiledGeneratedSources);
        JkUtilsAssert.state(project.compilerToolChain.compile(compileSpec),
                "Error while compiling classes generated for AOT");
        JkLog.endTask();

        // create native image
        classpath.add(compiledGeneratedSources);
        classpath.add(aotEnricher.getGeneratedResourcesDir());
        JkNativeImage nativeImage = JkNativeImage.ofClasspath(classpath);

        // -- set reachability metadata info
        nativeImage.reachabilityMetadata.setDependencies(
                project.packaging.resolveRuntimeDependencies().getDependencyTree().getDescendantModuleCoordinates());
        nativeImage.reachabilityMetadata.setExtractDir(
                project.getOutputDir().resolve("graalvm-reachability-metadata-repo"));

        nativeImage
                .setStaticLinkage(linkage)
                .make(execPath);
    }

    private String nativeExecName() {
        JkProject project = load(ProjectKBean.class).project;
        String exeFilename = project.artifactLocator.getMainArtifactPath().getFileName().toString();
        return JkUtilsString.substringBeforeLast(exeFilename, ".jar");
    }

    private Path nativeExecPath() {
        return getOutputDir().resolve(nativeExecName());
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

        // Configure native kbean
        getRunbase().find(NativeKBean.class).ifPresent(nativeKBean -> {
            nativeKBean.includeMainClassArg = false;
            nativeKBean.setAotAssetDirs(() ->
                    this.generateAotEnrichment(projectKBean.project));
        });
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

        @JkDoc("The name of the docker image containing the springboot native app")
        public String dockerImageName;

        @JkDoc("The springboot profiles that should be activated while processing AOT")
        public String aotProfiles;

        @JkDoc("For testing purpose : force the jeka version executed in docker container while invoking makeNativeDocker")
        public String jekaVersionInDocker;

        @JkDoc("When creating a Docker native image on Windows or macOS, " +
                "the project is copied to another directory " +
                "to generate the native image using Docker from that location. " +
                "Use these exclude patterns to avoid copying specific files.")
        public String copyProjectExcludePatterns;

        @JkDoc("How the native image should be linked when created for local host.")
        public JkNativeImage.StaticLinkage staticLinkage = JkNativeImage.StaticLinkage.NONE;

        @JkDoc("How the native image should be linked when created for Docker image. " +
                "FULLY is the most portable has it does not require any lib installed on the base image.")
        public JkNativeImage.StaticLinkage dockerImageStaticLinkage = JkNativeImage.StaticLinkage.NONE;

        /**
         * Allows to customize generated Docker image for native exec.
         */
        public final JkConsumers<JkDockerBuild> dockerImageCustomizers = JkConsumers.of();

        private boolean hasAotProfile() {
            return !JkUtilsString.isBlank(this.aotProfiles);
        }

        private String[] aotProfiles() {
            if (hasAotProfile()) {
                return aotProfiles.split(",");
            }
            return new String[0];
        }

        private boolean hasCopyExcludePatterns() {
            return !JkUtilsString.isBlank(this.copyProjectExcludePatterns);
        }

        private String[] copyExcludePatterns() {
            if (hasCopyExcludePatterns()) {
                return copyProjectExcludePatterns.split(",");
            }
            return new String[0];
        }

    }

}
