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

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.docker.JkDockerAppTester;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.tooling.docker.JkDockerJvmBuild;
import dev.jeka.core.api.tooling.docker.JkDockerNativeBuild;
import dev.jeka.core.api.tooling.nativ.JkNativeCompilation;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.nio.file.Path;
import java.util.function.Consumer;

@JkDoc("Builds and runs image based on project.\n" +
        "This KBean can build JVM and Native (AOT) images from an existing project."
)
@JkDocUrl("https://jeka-dev.github.io/jeka/reference/kbeans-docker/")
public final class DockerKBean extends KBean {

    @JkDoc("Explicit full name of the JVM image to build. It may includes placeholders such as '$version', '$groupId', and '$artifactId'.%n"
            + "If not specified, the image name will be inferred form the project information.")
    public String jvmImageName;

    @JkDoc("Explicit full name of the native image to build. It may includes placeholders such as '$version', '$groupId', and '$artifactId'.%n"
            + "If not specified, the image name will be inferred form the project information.")
    public String nativeImageName;

    @JkDoc("Base image to construct the Docker image.")
    public String jvmBaseImage = JkDockerJvmBuild.DEFAULT_BASE_IMAGE;

    @JkDoc("Space-separated list of additional JVM options to use when running the container's Java process.")
    @JkDepSuggest(versionOnly = true, hint = "-Xms512m,-Xmx2g,-Xmn128m,-Xss1m,-Xlog:gc,-XX:+UseG1GC," +
            "-XX:+PrintGCDetails,-XX:+HeapDumpOnOutOfMemoryError,-Xdiag,-XshowSettings,-Xlog:exceptions")
    public String jvmOptions;



    @JkDoc("Base image for the native Docker image to build. " +
            "It can be replaced by a distro-less image as 'gcr.io/distroless/static-debian12:nonroot'")
    public String nativeBaseImage = JkDockerNativeBuild.DEFAULT_BASE_IMAGE;

    @JkDoc("Specifies the policy for creating a non-root user in the native Docker image.")
    public JkDockerBuild.NonRootMode nativeNonRootUser = JkDockerBuild.NonRootMode.AUTO;

    @JkDoc("Specifies the policy for creating a non-root user in the JVM Docker image.")
    public JkDockerBuild.NonRootMode jvmNonRootUser = JkDockerBuild.NonRootMode.AUTO;

    @JkDoc("Agents to bind to the JVM")
    public JkMultiValue<JvmAgentOptions> jvmAgents = JkMultiValue.of(JvmAgentOptions.class);

    /*
     * Handler on the Docker build configuration for customizing built images.
     */
    private final JkConsumers<JkDockerJvmBuild> jvmImageCustomizer = JkConsumers.of();

    private final JkConsumers<JkDockerNativeBuild> nativeImageCustomizer = JkConsumers.of();

    @Override
    protected void init() {
        jvmAgents.getValues().forEach(jvmAgentOptions -> {
            jvmImageCustomizer.append(dockerJvmBuild -> {
                String optionLine = JkUtilsString.nullToEmpty(jvmAgentOptions.optionLine);
                dockerJvmBuild.addAgent(jvmAgentOptions.coordinate, optionLine);
            });
        });
    }

    @JkRequire
    private static Class<? extends KBean> requireBuildable(JkRunbase runbase) {
        return runbase.getBuildableKBeanClass();
    }

    @JkDoc("Builds Docker image in local registry.")
    public void build() {
        JkBuildable buildable = getBuildable(true);
        String imageName = resolveJvmImageName();
        String dirName = "docker-build-" + imageName.replace(':', '#');
        JkLog.startTask("build-jvm-docker-image");
        JkLog.info("Image Name: " + imageName);
        Path contextDir = getOutputDir().resolve(dirName);
        jvmDockerBuild(buildable).buildImage(contextDir, imageName);
        JkLog.endTask();
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
        JkLog.startTask("build-native-docker-image");
        JkLog.info("Image Name: " + imageName);
        Path contextDir = getOutputDir().resolve(dirName);
        nativeDockerBuild(buildable).buildImage(contextDir, imageName);
        JkLog.endTask();
    }

    @JkDoc("Displays info about the native Docker image.")
    public void infoNative() {
        String imageName = resolveNativeImageName();
        JkBuildable buildable = getRunbase().getBuildable();
        JkLog.info("Image Name        : " + imageName);
        JkLog.startTask("prepare-native-compilation-data");
        String info = nativeDockerBuild(buildable).renderInfo(); // May trigger a compilation to find the main class
        JkLog.endTask();
        JkLog.info(info);
    }

    /**
     * Computes the name of the Docker image based on the specified project.
     *
     * @param buildable The JkProject ok baseKbean instance containing the module ID, version, and base directory.
     * @return The computed image name.
     */
    public static String computeImageName(JkBuildable buildable) {
        return computeImageName(buildable.getModuleId(), buildable.getVersion(), buildable.getBaseDir());
    }

    /**
     * Adds a customizer function for customizing the Docker JVM image to build.
     */
    public void customizeJvmImage(Consumer<JkDockerJvmBuild> dockerBuildCustomizer) {
        jvmImageCustomizer.append(dockerBuildCustomizer);
    }

    /**
     * Adds a customizer function for customizing the Dockerbuild that will generate the Native image.
     */
    public void customizeNativeImage(Consumer<JkDockerNativeBuild> dockerBuildCustomizer) {
        nativeImageCustomizer.append(dockerBuildCustomizer);
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

    /**
     * Creates a {@link JkDockerAppTester} instance configured for testing a JVM-based Docker application.
     *
     * @param tester A consumer function that defines the tests to execute against the application,
     *               consuming the base URL and port of the running application.
     */
    public JkDockerAppTester createJvmAppTester(Consumer<String> tester) {
        JkDockerBuild dockerBuild = jvmDockerBuild(this.getBuildable(true));
        String imageName = resolveJvmImageName() + "-e2e-test";
        String dirName = "docker-build-" + imageName.replace(':', '#');
        return JkDockerAppTester.of(dockerBuild, tester)
                .setImageName(imageName)
                .setContextPath(getOutputDir().resolve(dirName));
    }

    /**
     * Creates a {@link JkDockerAppTester} instance configured for testing a native Docker application.
     *
     * @param tester A consumer function that defines the tests to execute against the application,
     *               consuming the base URL and port of the running application.
     */
    public JkDockerAppTester createNativeAppTester(Consumer<String> tester) {
        JkDockerBuild dockerBuild = nativeDockerBuild(this.getBuildable(true));
        String imageName = resolveNativeImageName() + "-e2e-test";
        String dirName = "docker-build-" + imageName.replace(':', '#');
        return JkDockerAppTester.of(dockerBuild, tester)
                .setImageName(imageName)
                .setContextPath(getOutputDir().resolve(dirName));
    }

    public static final class JvmAgentOptions {

        @JkDepSuggest
        @JkDoc("Coordinate of the JVM agent, e.g. 'io.opentelemetry.javaagent:opentelemetry-javaagent:1.32.0'")
        public String coordinate;

        @JkDoc("Option line to pass to the agent, e.g. '-Dotel.traces.exporter=otlp,-Dotel.metrics.exporter=otlp")
        public String optionLine;
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
        dockerBuild.addJvmOptions(JkUtilsString.splitWhiteSpaces(jvmOptions));
        dockerBuild.adaptTo(buildable);
        jvmImageCustomizer.accept(dockerBuild);
        return dockerBuild;
    }

    private JkDockerNativeBuild nativeDockerBuild(JkBuildable buildable) {
        NativeKBean nativeKBean = this.load(NativeKBean.class);
        JkLog.verbose("Configure native Docker image for %s", buildable);
        JkNativeCompilation nativeImage =  nativeKBean.createNativeCompilation(buildable);
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

    public static class jvmConfig {

        public String imageName;

        public String baseImage;

        public String options;

        public final JkMultiValue<Agent> agents = JkMultiValue.of(Agent.class);

        public static class Agent {

            @JkDepSuggest
            public String coordinate;

            public String options;
        }
    }

}
