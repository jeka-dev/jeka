package dev.jeka.core.tool.builtins.self;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.utils.JkUtilsJdk;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Experimental
 */
@JkDoc(
        "Experimental !!! KBean for application self-contained in jeka/def dir.\n" +
        "The application must contain a class with main method in order to : \n" +
        "  - Run application\n" +
        "  - Create bootable jar\n" +
        "  - Create bootable fat jar, and create Docker images.\n" +
        "  - Note : The Docker feature requires a running Docker daemon running on host machine."
)
public abstract class SelfAppKBean extends KBean {

    private final String dockerImageTag = getBaseDirName();

    @JkDoc("Space separated list of options to pass to the JVM that will run the program.")
    public String jvmOptions = "";

    @JkDoc("Space separated list of program arguments to pass to the command line running the program.")
    public String args = "";

    @JkDoc("Extra parameters to pass to 'docker run' command while invoking '#runImage")
    public String dockerRunParams = "";

    private String mainClass;

    private Consumer<Path> jarMaker = this::fatJar;

    /**
     * {@link JkDockerBuild} customizer to control the effective docker build.
     * Use this field to add your own customizer.
     */
    public final JkConsumers<JkDockerBuild> dockerBuildCustomizers = JkConsumers.of();

    // We can not just run Application#main cause Spring-Boot seems
    // requiring that the Java process is launched using Spring-Boot application class
    @JkDoc("Launch application")
    public void runMain() {
        JkJavaProcess.ofJava(effectiveMainClass())
                .setClasspath(appClasspath())
                .setInheritIO(true)
                .setInheritSystemProperties(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.translateCommandline(jvmOptions))
                .addParams(JkUtilsString.translateCommandline(args))
                .exec();
    }

    @JkDoc("Launch test suite")
    public void test() {
        JkTestSelection testSelection = JkTestSelection.of().addTestClassRoots(classTree().getRoot());
        JkTestProcessor.of()
                .setForkingProcess(true)
                .launch(JkClassLoader.ofCurrent().getClasspath(), testSelection);
    }

    @JkDoc("Build Docker image (Need Docker).")
    public void buildImage() {
        JkDockerBuild dockerBuild = JkDockerBuild.of()
                .setClasses(classTree())
                .setClasspath(libs())
                .setMainClass(effectiveMainClass());
        dockerBuildCustomizers.accept(dockerBuild);
        dockerBuild.buildImage(dockerImageTag);
    }

    @JkDoc("Run Docker image and wait until termination (Need Docker).")
    public void runImage() {
        String containerName = "jeka-" + dockerImageTag;
        JkDocker.run("-it --rm -e \"JVM_OPTIONS=%s\" -e \"PROGRAM_ARGS=%s\" " + dockerRunParams + " --name %s %s",
                jvmOptions, args,
                containerName, dockerImageTag);
    }

    @JkDoc("Create runnable fat jar.")
    public void buildJar() {
        jarMaker.accept(jarPath());
    }

    @JkDoc("Run fat jar.")
    public void runJar() {
        JkJavaProcess.ofJavaJar(jarPath())
                .setLogCommand(true)
                .setInheritIO(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.translateCommandline(jvmOptions))
                .addParams(JkUtilsString.translateCommandline(args))
                .run();
    }

    public Path jarPath() {
        return getBaseDir().resolve(JkConstants.OUTPUT_PATH).resolve(getBaseDirName() + ".jar");
    }

    public SelfAppKBean setJarMaker(Consumer<Path> jarMaker) {
        this.jarMaker = jarMaker;
        return this;
    }

    protected void setMainClass(Class<?> mainClass) {
        this.mainClass = mainClass.getName();
    }

    private String findMainClass() {
        if (mainClass != null) {
            return mainClass;
        }
        JkUrlClassLoader ucl = JkUrlClassLoader.of(getBaseDir().resolve(JkConstants.DEF_BIN_DIR));
        return ucl.toJkClassLoader().findUniqueMainClass();
    }

    protected List<Path> appClasspath() {
        return getRuntime().getClasspath().getEntries();
    }

    public List<Path> libs() {
        return appClasspath().stream()
                .filter(entry -> !entry.toAbsolutePath().normalize()
                        .equals(classTree().getRoot().toAbsolutePath().normalize()))
                .collect(Collectors.toList());
    }

    public JkPathTree<?> classTree() {
        return JkPathTree.of(getBaseDir().resolve(JkConstants.DEF_BIN_DIR))
                .andMatching(false, "_*", "_*/**");
    }

    protected JkManifest manifest() {
        return JkManifest.of()
                .addMainAttribute(JkManifest.CREATED_BY, "JeKa")
                .addMainClass(effectiveMainClass())
                .addMainAttribute(JkManifest.BUILD_JDK, "" + JkUtilsJdk.runningMajorVersion());
    }

    private String effectiveMainClass() {
        return Optional.ofNullable(mainClass).orElse(findMainClass());
    }

    private void fatJar(Path jarPath) {
        JkJarPacker.of(classTree())
                .withManifest(manifest())
                .makeFatJar(jarPath, libs(), JkPathMatcher.of());
        JkLog.info("Jar created at : " + jarPath);
    }

}