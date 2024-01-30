package dev.jeka.core.tool.builtins.self;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.scaffold.JkScaffoldOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JkDoc(
        "KBean for application/library self-contained in jeka-src dir.\n" +
        "The application must contain a class with main method in order to : \n" +
        "  - Run application\n" +
        "  - Create bootable jar\n" +
        "  - Create bootable fat jar, and create Docker images."
)
public final class SelfKBean extends KBean {

    /**
     * Represents the value "auto" usable in {@link #setMainClass(String)}
     * to indicate that the main class should be discovered automatically.
     */
    public static final String AUTO_FIND_MAIN_CLASS = "auto";

    @JkDoc("Space separated list of options to pass to the JVM that will run the program.")
    public String jvmOptions = "";

    @JkDoc("Space separated list of program arguments to pass to the command line running the program.")
    public String programArgs = "";

    @JkDoc
    final SelfScaffoldOptions scaffold = new SelfScaffoldOptions();

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
        Path tempDirClass = JkUtilsPath.createTempDirectory("jk-");
        getAppClasses().copyTo(tempDirClass);
        JkJavaProcess.ofJava(getMainClass())
                .setClasspath(JkPathSequence.of(tempDirClass).and(getAppClasspath()))
                .setInheritIO(true)
                //.setInheritSystemProperties(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                .addParams(JkUtilsString.parseCommandline(programArgs))
                .exec();
        JkPathTree.of(tempDirClass).createIfNotExist();
    }

    @JkDoc("Launch test suite")
    public void test() {
        JkTestSelection testSelection = JkTestSelection.of().addTestClassRoots(getAppClasses().getRoot());
        JkTestProcessor.of()
                .setForkingProcess(true)
                .launch(JkClassLoader.ofCurrent().getClasspath(), testSelection);
    }

    @JkDoc("Create runnable fat jar.")
    public void buildJar() {
        jarMaker.accept(getJarPath());
    }

    @JkDoc("Run fat jar.")
    public void runJar() {
        this.prepareRunJar().exec();
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
        this.getAppClasses().getRelativeFiles().stream()
                .forEach(path -> sb.append("  " + path + "\n"));
        sb.append("Classpath    : ").append("\n");
        this.getAppClasspath().forEach(path -> sb.append("  " + path + "\n"));
        sb.append("Manifest     : ").append("\n");
        Arrays.stream(getManifest().asString().split("\n"))
                .forEach(line -> sb.append("  " + line + "\n"));

        JkLog.info(sb.toString());
    }

    @JkDoc("Display exported dependency tree on console.")
    public void showDepTree() {
        String output = getRunbase().getDependencyResolver().resolve(getRunbase().getExportedDependencies())
                .getDependencyTree().toStringTree();
        JkLog.info(output);
    }

    @JkDoc("Creates a skeleton in the current working directory.")
    public void scaffold() {
        //JkScaffold scaffolder = JkScaffold.of(Paths.get(""));
        //this.scaffold.configure(scaffolder);
        //scaffolder.run();
    }

    /**
     * Returns the path to the JAR file created by {@link #buildJar()} method.
     */
    public Path getJarPath() {
        return Paths.get(getJarPathBaseName() + ".jar");
    }

    /**
     * Returns the base name of the JAR file path created by the {@link #buildJar()} method.
     */
    public String getJarPathBaseName() {
        return getBaseDir().resolve(JkConstants.OUTPUT_PATH).resolve(getBaseDirName()).toString();
    }

    /**
     * Sets the jarMaker for creating a runnable fat jar.
     */
    public SelfKBean setJarMaker(Consumer<Path> jarMaker) {
        this.jarMaker = jarMaker;
        return this;
    }

    /**
     * Sets the version supplier for this SelfKBean. The version represents the
     * version of the application/library.
     */
    public SelfKBean setVersionSupplier(Supplier<JkVersion> versionSupplier) {
        this.versionSupplier = versionSupplier;
        return this;
    }

    /**
     * Sets the version for this SelfKBean. The version represents the
     * version of the application/library.
     */
    public SelfKBean setVersion(String version) {
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
    public SelfKBean setModuleId(String moduleId) {
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
    public SelfKBean setMainClassFinder(Supplier<String> mainClassFinder) {
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
    public List<Path> getAppLibs() {
        return getAppClasspath().stream()
                .filter(entry -> !entry.toAbsolutePath().normalize()
                        .equals(getAppClasses().getRoot().toAbsolutePath().normalize()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a {@link JkPathTree} representing the class files and sub-folders contained in the "bin" directory.
     * The tree includes all files in the root directory and its subdirectories,
     * except for files matching the specified patterns.
     */
    public JkPathTree getAppClasses() {
        return JkPathTree.of(getBaseDir().resolve(JkConstants.JEKA_SRC_CLASSES_DIR))
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

    /**
     * Creates a Javadoc Jar file at the specified target path.
     */
    public void createJavadocJar(Path target) {
        JkUtilsPath.deleteIfExists(target);
        Path tempFolder = JkUtilsPath.createTempDirectory("jk-self-sources");
        JkJavadocProcessor.of()
                .make(getAppLibs(), getAppSources().toSet(), tempFolder);
        JkPathTree.of(tempFolder).zipTo(target);
        JkPathTree.of(tempFolder).deleteRoot();
    }

    /**
     * Creates a source JAR file at the specified target path.
     */
    public void createSourceJar(Path target) {
        JkUtilsPath.deleteIfExists(target);
        getAppSources().zipTo(target);
    }

    /**
     * Returns a JkPathTree representing the application sources.
     */
    public JkPathTree getAppSources() {
        return JkPathTree.of(getBaseDir().resolve(JkConstants.JEKA_SRC_DIR)).withMatcher(
                JkConstants.PRIVATE_IN_DEF_MATCHER.negate());
    }

    /**
     * Prepares a {@link JkJavaProcess ready to run.
     */
    public JkJavaProcess prepareRunJar() {
        Path jarPath = getJarPath();
        if (!Files.exists(jarPath)) {
            buildJar();
        }
        return JkJavaProcess.ofJavaJar(getJarPath())
                .setLogCommand(true)
                .setInheritIO(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.parseCommandline(jvmOptions))
                .addParams(JkUtilsString.parseCommandline(programArgs));
    }

    private String findMainClass() {
        JkUrlClassLoader ucl = JkUrlClassLoader.of(getBaseDir().resolve(JkConstants.JEKA_SRC_CLASSES_DIR));
        return ucl.toJkClassLoader().findClassesHavingMainMethod().stream()
                .filter(candidate -> !candidate.startsWith("_"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No main class found in classpath"));
    }

    private void fatJar(Path jarPath) {
        JkLog.startTask("Making fat jar. It may takes a while ... ");
        JkJarPacker.of(getAppClasses())
                .withManifest(getManifest())
                .makeFatJar(jarPath, getAppLibs(), JkPathMatcher.of());
        JkLog.endTask();
        JkLog.info("Jar created at : " + jarPath);
    }

    public static class SelfScaffoldOptions extends JkScaffoldOptions {

        @JkDoc("Kind of Jeka base to generate.")
        public JkSelfScaffold.Kind kind = JkSelfScaffold.Kind.JEKA_SCRIPT;

    }

}