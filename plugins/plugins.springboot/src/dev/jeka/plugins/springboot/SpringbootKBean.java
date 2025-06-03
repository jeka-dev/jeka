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

import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;


/*
 *  Inspired from :
 *     - https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-tools/spring-boot-maven-plugin/src/main/java/org/springframework/boot/maven
 */
@JkDoc(
        "Adapt `project` or `base` KBean for Spring-Boot:\n" +
        "- Produce bootable jars\n" +
        "- Customize .war file for projectKBean\n" +
        "- Adapt scaffolding\n" +
        "- Include Spring Maven repositories for resolution\n" +
        "- Adapt Docker image generator to include port exposure"
)
@JkDocUrl("https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.springboot/README.md")
public final class SpringbootKBean extends KBean {

    @JkDoc("If true, create a bootable jar artifact.")
    private final boolean createBootJar = true;

    @JkDoc("If true, create original jar artifact for publication (jar without embedded dependencies")
    private boolean createOriginalJar;

    @JkDoc("If true, create a .war filed.")
    private boolean createWarFile;

    @JkDoc("Specific Spring repo where to download spring artifacts. Not needed if you use official release.")
    private JkSpringRepo springRepo;

    @JkDoc("The springboot profiles that should be activated while processing AOT")
    public String aotProfiles;

    @JkDoc("Space separated string of ports to expose. This is likely to be used by external tool as Docker.")
    public String exposedPorts="8080";

    @JkDoc("Provides info about this plugin configuration")
    public void info() {
        JkLog.info("Create Bootable Jar : " + this.createBootJar);
        JkLog.info("Create original Jar : " + this.createOriginalJar);
        JkLog.info("Create .war file : " + this.createWarFile);
    }

    @JkRequire
    private static Class<? extends KBean> requireBuildable(JkRunbase runbase) {
        return runbase.getBuildableKBeanClass();
    }

    @JkDoc("Sets test progress style to PLAIN to display JVM messages gracefully.")
    @JkPreInit
    private static void initProjectKbean(ProjectKBean projectKBean) {
        projectKBean.project.testing.testProcessor.engineBehavior
                .setProgressDisplayer(JkTestProcessor.JkProgressStyle.STEP);
    }

    @JkDoc("Adapts project: " +
            "creates Bootable JAR on #pack, " +
            "adds Springboot Maven repositories to dependency resolutions, " +
            "forces tests to run in separated process")
    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.getProjectScaffold().addCustomizer(SpringbootScaffold::customize);

        JkSpringbootProject springbootProject = JkSpringbootProject.of(projectKBean.project)
                .configure(this.createBootJar, this.createWarFile, this.createOriginalJar);
        if (springRepo != null) {
            springbootProject.addSpringRepo(springRepo);
        }
    }

    @JkDoc("Adapts base KBean: " +
            "creates Bootable JAR on #pack, " +
            "adds Springboot Maven repositories to dependency resolutions, " +
            "forces tests to run in separated process")
    @JkPostInit
    private void postInit(BaseKBean baseKBean) {
        if (find(ProjectKBean.class).isPresent()) {
            return;
        }
        baseKBean.getBaseScaffold().addCustomizer(SpringbootScaffold::customize);

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

    @JkDoc("Adds exposed ports to the built images")
    @JkPostInit
    private void postInit(DockerKBean dockerKBean) {
        dockerKBean.customizeJvmImage(this::customizeDockerBuild);
        dockerKBean.customizeNativeImage(this::customizeDockerBuild);
    }

    @JkDoc("Adds Springboot AOT step when building native executable")
    @JkPostInit
    private void postInit(NativeKBean nativeKBean) {
        nativeKBean.includeMainClassArg = false;
        nativeKBean.setAotAssetDirs(() -> {
            JkBuildable buildable = getRunbase().getBuildable();
            return this.generateAotEnrichment(buildable);
        });
    }

    /**
     * Creates a {@link JkSpringbootAppTester} instance for testing a Spring Boot application.
     * The tester is configured with a specified consumer that executes testing logic
     * against the application once it is started.
     * <p>
     * To run properly, the actuator needs to be in classpath.
     * </p>
     *
     * @param tester a {@link Consumer} of {@link String} that contains the logic for testing
     *               the application. The consumer receives the base URL and port of the
     *               running application.
     */
    public JkSpringbootAppTester createE2eAppTester(Consumer<String> tester) {
        return JkSpringbootAppTester.of(() -> this.getRunbase().getBuildable(), tester);
    }

    private List<Path> generateAotEnrichment(JkBuildable buildable) {
        JkLog.startTask("process-springboot-aot");
        AotPreProcessor aotPreProcessor = AotPreProcessor.of(buildable);
        if (hasAotProfile()) {
            aotPreProcessor.profiles.addAll(Arrays.asList(aotProfiles()));
        }
        aotPreProcessor.generate();

        List<Path> classpath = new LinkedList<>(aotPreProcessor.getClasspath());
        classpath.add(aotPreProcessor.getGeneratedClassesDir());
        Path compiledGeneratedSources = buildable.getOutputDir()
                .resolve("spring-aot/compiled-generated-sources");
        JkLog.verbose("Compile generated sources to %s",  compiledGeneratedSources);
        JkJavaCompileSpec compileSpec = JkJavaCompileSpec.of()
                .setSources(JkPathTreeSet.ofRoots(aotPreProcessor.getGeneratedSourcesDir()))
                .setClasspath(classpath)
                .setOutputDir(compiledGeneratedSources);
        JkUtilsAssert.state(buildable.compile(compileSpec),
                "Error while compiling classes generated for AOT");
        JkLog.endTask();

        List<Path> result = new LinkedList<>();
        result.add(aotPreProcessor.getGeneratedClassesDir());
        result.add(aotPreProcessor.getGeneratedResourcesDir());
        result.add(compiledGeneratedSources);
        return result;
    }

    private void customizeDockerBuild(JkDockerBuild dockerBuild) {
        if (dockerBuild.getExposedPorts().isEmpty()) {
            dockerBuild.setExposedPorts(exposedPortAsArray());
        }
    }

    private boolean hasAotProfile() {
        return !JkUtilsString.isBlank(this.aotProfiles);
    }

    private String[] aotProfiles() {
        if (hasAotProfile()) {
            return aotProfiles.split(",");
        }
        return new String[0];
    }

    private Integer[] exposedPortAsArray() {
        return Arrays.stream(exposedPorts.split(" "))
                .map(Integer::valueOf)
                .toArray(Integer[]::new);
    }

}
