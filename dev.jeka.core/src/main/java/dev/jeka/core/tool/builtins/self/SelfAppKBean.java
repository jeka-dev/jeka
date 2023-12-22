package dev.jeka.core.tool.builtins.self;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsJdk;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
    public String dockerRunParams = "-p 8080:8080";

    private String mainClass;

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
        JkDockerBuild.of()
                .setClasses(classTree())
                .setClasspath(libs())
                .setMainClass(effectiveMainClass())
                .buildImage(dockerImageTag);
    }

    @JkDoc("Run Docker image and wait until termination (Need Docker).")
    public void runImage() {
        String containerName = "jeka-" + dockerImageTag;
        JkDocker.run("-it --rm -e \"JVM_OPTIONS=%s\" -e \"PROGRAM_ARGS=%s\" " + dockerRunParams + " --name %s %s",
                jvmOptions, args,
                containerName, dockerImageTag);
    }

    @JkDoc("Create runnable fat jar.")
    public void packJar() {
        JkJarPacker.of(classTree())
                .withManifest(manifest())
                .makeFatJar(jarPath(), libs(), JkPathMatcher.of());
        JkLog.info("Jar created at : " + jarPath());
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

    protected void setMainClass(Class<?> mainClass) {
        this.mainClass = mainClass.getName();
    }

    private String findMainClass() {
        if (mainClass != null) {
            return mainClass;
        }
        JkUrlClassLoader ucl = JkUrlClassLoader.of(getBaseDir().resolve(JkConstants.DEF_BIN_DIR));
        List<String> candidates = ucl.toJkClassLoader().findClassesHavingMainMethod();
        JkUtilsAssert.state(!candidates.isEmpty(), "No class with main method found. Add one to " +
                "def folder in order to run.");
        JkUtilsAssert.state(candidates.size() == 1, "Multiple classes with main method found %s, please pickup " +
                "one and mention it in #setMainClass.", candidates);
        return candidates.get(0);
    }

    protected List<Path> appClasspath() {
        return getRuntime().getClasspath().getEntries();
    }

    protected List<Path> libs() {
        return appClasspath().stream()
                .filter(entry -> !entry.toAbsolutePath().normalize()
                        .equals(classTree().getRoot().toAbsolutePath().normalize()))
                .collect(Collectors.toList());
    }

    protected JkPathTree<?> classTree() {
        return JkPathTree.of(getBaseDir().resolve(JkConstants.DEF_BIN_DIR));
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

}