package dev.jeka.core.tool.builtins.self;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JkDoc(
        "Experimental !!! KBean for application self-contained in jeka/def dir.\n" +
        "The application must contain a class with main method in order to : \n" +
        "  - Run application\n" +
        "  - Create bootable jar\n" +
        "  - Create bootable fat jar, and create Docker images.\n" +
        "  - Note : The Docker feature requires a running Docker daemon running on host machine."
)
public abstract class SelfAppKBean extends KBean {

    /**
     * This constant represents the value "auto" and is used in {@link #setMainClass(String)}
     * to indicate that the main class should be discovered automatically..
     */
    public static final String AUTO_FIND_MAIN_CLASS = "auto";

    private final String dockerImageTag = getBaseDirName();

    @JkDoc("Space separated list of options to pass to the JVM that will run the program.")
    public String jvmOptions = "";

    @JkDoc("Space separated list of program arguments to pass to the command line running the program.")
    public String programArgs = "";

    public final JkConsumers<JkManifest> manifestCustomizers = JkConsumers.of();

    private String mainClass = AUTO_FIND_MAIN_CLASS;

    private Supplier<String> mainClassFinder = this::findMainClass;

    private JkModuleId moduleId;

    private Supplier<JkVersion> versionSupplier = () -> JkVersion.UNSPECIFIED;

    private Consumer<Path> jarMaker = this::fatJar;

    // We can not just run Application#main cause Spring-Boot seems
    // requiring that the Java process is launched using Spring-Boot application class
    @JkDoc("Launch application")
    public void runMain() {
        JkJavaProcess.ofJava(getMainClass())
                .setClasspath(getAppClasspath())
                .setInheritIO(true)
                .setInheritSystemProperties(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                .addParams(JkUtilsString.parseCommandline(programArgs))
                .exec();
    }

    @JkDoc("Launch test suite")
    public void test() {
        JkTestSelection testSelection = JkTestSelection.of().addTestClassRoots(getClassTree().getRoot());
        JkTestProcessor.of()
                .setForkingProcess(true)
                .launch(JkClassLoader.ofCurrent().getClasspath(), testSelection);
    }

    @JkDoc("Create runnable fat jar.")
    public void buildJar() {
        jarMaker.accept(jarPath());
    }

    @JkDoc("Run fat jar.")
    public void runJar() {
        Path jarPath = jarPath();
        if (!Files.exists(jarPath)) {
            buildJar();
        }
        JkJavaProcess.ofJavaJar(jarPath())
                .setLogCommand(true)
                .setInheritIO(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                .addParams(JkUtilsString.parseCommandline(programArgs))
                .run();
    }

    @JkDoc("Displays info about this SelfApp")
    public void info() {
        StringBuilder sb = new StringBuilder();
        sb.append("Module Id    : " + this.moduleId).append("\n");
        sb.append("Version      : " + this.getVersion()).append("\n");
        sb.append("Main Class   : " + this.getMainClass()).append("\n");
        sb.append("JVM Options  : " + jvmOptions).append("\n");
        sb.append("Program Args : " + programArgs).append("\n");
        sb.append("Class Files  : ").append("\n");
        this.getClassTree().getRelativeFiles().stream()
                .forEach(path -> sb.append("  " + path + "\n"));
        sb.append("Classpath    : ").append("\n");
        this.getAppClasspath().forEach(path -> sb.append("  " + path + "\n"));
        sb.append("Manifest     : ").append("\n");
        Arrays.stream(getManifest().asString().split("\n"))
                .forEach(line -> sb.append("  " + line + "\n"));

        JkLog.info(sb.toString());
    }

    /**
     * Returns the path to the JAR file created by {@link #buildJar()} method.
     */
    public Path jarPath() {
        return getBaseDir().resolve(JkConstants.OUTPUT_PATH).resolve(getBaseDirName() + ".jar");
    }

    /**
     * Sets the jarMaker for creating a runnable fat jar.
     */
    public SelfAppKBean setJarMaker(Consumer<Path> jarMaker) {
        this.jarMaker = jarMaker;
        return this;
    }

    /**
     * Sets the version supplier for this SelfAppKBean. The version represents the
     * version of the application/library.
     */
    public SelfAppKBean setVersionSupplier(Supplier<JkVersion> versionSupplier) {
        this.versionSupplier = versionSupplier;
        return this;
    }

    /**
     * Sets the version for this SelfAppKBean. The version represents the
     * version of the application/library.
     */
    public SelfAppKBean setVersion(String version) {
        this.versionSupplier = ()  -> JkVersion.of(version);
        return this;
    }

    /**
     * Returns the version of the application/library. The version might be used in Manifest file,
     * docker image name or Maven publication.
     */
    public JkVersion getVersion() {
        return versionSupplier.get();
    }

    /**
     * Returns the module ID for this object.
     * The module ID is used for naming Docker image and in Maven publication.
     */
    public JkModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Sets the module ID for this object. The module id might be used for naming Docker image
     * and in Maven publication.
     *
     * @param moduleId The module ID formatted as <i>group:name</i>.
     */
    public SelfAppKBean setModuleId(String moduleId) {
        this.moduleId = JkModuleId.of(moduleId);
        return this;
    }

    /**
     * Returns the actual main class to be used for launching the application or executable JAR.
     * If the main class is already set, it will be returned. Otherwise, the main class will be determined
     * by searching for the unique main class in the JAR file created by the {@link #buildJar()} method.
     */
    public String getMainClass() {
        if (AUTO_FIND_MAIN_CLASS.equals(mainClass)) {
            return mainClassFinder.get();
        }
        return mainClass;
    }

    /**
     * Sets the main class. The main class is used to launch the application or executable JAR.
     *
     * @param mainClass The main class to be set.
     */
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    /**
     * Sets the main class finder for this project. The main class finder is responsible for
     * providing the name of the main class to use in the project. This can be used for running
     * the project or building Docker images.
     */
    public SelfAppKBean setMainClassFinder(Supplier<String> mainClassFinder) {
        this.mainClassFinder = mainClassFinder;
        return this;
    }

    /**
     * Returns the application classpath. This contains class dir + libraries.
     */
    public List<Path> getAppClasspath() {
        return getRunbase().getExportedClasspath().getEntries();
    }

    /**
     * Returns a List of Path objects representing the libraries used by the application.
     * It contains the classpath minus the class dir.
     */
    public List<Path> getLibs() {
        return getAppClasspath().stream()
                .filter(entry -> !entry.toAbsolutePath().normalize()
                        .equals(getClassTree().getRoot().toAbsolutePath().normalize()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a {@link JkPathTree} representing the class files and sub-folders contained in the "bin" directory.
     * The tree includes all files in the root directory and its subdirectories,
     * except for files matching the specified patterns.
     */
    public JkPathTree getClassTree() {
        return JkPathTree.of(getBaseDir().resolve(JkConstants.DEF_BIN_DIR))
                .andMatching(false, "_*", "_*/**", ".*", "**/.*");
    }

    /**
     * Returns the {@link JkManifest} for the application.
     * The manifest includes the created by attribute,
     * the main class attribute, and the build JDK attribute.
     */
    public JkManifest getManifest() {
        JkManifest manifest = JkManifest.of()
                .addImplementationInfo(getModuleId(), getVersion())
                .addMainClass(getMainClass())
                .addBuildInfo();
        manifestCustomizers.accept(manifest);
        return manifest;
    }

    private String findMainClass() {
        JkUrlClassLoader ucl = JkUrlClassLoader.of(getBaseDir().resolve(JkConstants.DEF_BIN_DIR));
        return ucl.toJkClassLoader().findUniqueMainClass();
    }

    private void fatJar(Path jarPath) {
        JkLog.startTask("Making fat jar. It may takes a while ... ");
        JkJarPacker.of(getClassTree())
                .withManifest(getManifest())
                .makeFatJar(jarPath, getLibs(), JkPathMatcher.of());
        JkLog.endTask();
        JkLog.info("Jar created at : " + jarPath);
    }

}