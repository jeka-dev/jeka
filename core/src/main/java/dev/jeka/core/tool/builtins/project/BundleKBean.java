package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.tools.JkJdeps;
import dev.jeka.core.api.java.tools.JkJlink;
import dev.jeka.core.api.java.tools.JkJpackage;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsJdk;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@JkDoc("""
        Bundle a self-contained Java app (containing JRE) using jpackage and jlink tools.
        
        This plugin creates a self-contained app and allows configuring the jpackage and jlink tools.
       
        This integrates with ProjectKBean for project-specific setups, handles file
        system operations to prepare input directories, and invokes jpackage and jlink tools.
        """)
@JkDocUrl("https://jeka-dev.github.io/jeka/reference/kbeans-bundle/")
public final class BundleKBean extends KBean {

    private static final String JDEPS = "JDEPS-COMPUTE";

    public static final String JPACKAGE_TEMP_DIR_NAME = "jpackage-input";

    private static final String ADD_MODULES = "--add-modules";

    private static final String MODULE_PATH = "--module-path";

    @JkDoc("If true, a custom JRE is created including only Java modules used by the application.")
    public boolean customJre;

    @JkDoc("If true, creates a bundled application along with the regular JAR when executing 'project: pack'.")
    public boolean projectPack;

    @JkDoc("If true, the runtime libs will be added in the packaged application, which may not be necessary when using FAT jars.")
    public boolean includeRuntimeLibs = true;

    @JkDoc
    public final JpackageConfig jpackage = new JpackageConfig();

    @JkDoc
    public final JlinkConfig jlink = new JlinkConfig();

    @JkInject
    private ProjectKBean projectKBean;

    private final List<String[]> jpackageOptions = new LinkedList<>();

    private final List<String[]> jlinkOptions = new LinkedList<>();

    @Override
    protected void init() {

        // Fill jpackage and jlink options with user setup properties
        jpackageOptions.addAll(jpackage.options.toOptions());
        jlinkOptions.addAll(jlink.options.toOptions());
    }

    @JkDoc("Adds the bundled application to 'project: pack' actions, if 'projectPack=true'.")
    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        if (this.projectPack) {
            projectKBean.project.pack.actions.append("Bundling in a self-contained app", this::pack);
        }
    }

    @JkDoc("Packages the application into a bundle.")
    public void pack() {

        // Create jar if not exist
        Path jar = projectKBean.project.artifactLocator.getMainArtifactPath();
        if (!Files.exists(jar)) {
            JkLog.verbose("Jar %s not yet created: force creation to place it in bundle input dir ", jar);
            projectKBean.project.pack.run();
        }

        JkLog.startTask("create-bundled-application");
        JkJpackage jkJpackage = jpackage();

        // Set Project options
        setProjectOptions(jkJpackage, jar);

        // Add extra options
        jpackageOptions.forEach(jkJpackage::addOptions);


        if (customJre) {

            // Create custom JRE
            JkLog.startTask("custom-jre");
            JkJlink jkJlink = jlink();
            fillJklinkOptions(jkJlink);
            jlinkOptions.forEach(jkJlink::addOptions);
            JkUtilsPath.deleteQuietly(customJrePath(), false);
            jkJlink.run();
            JkLog.endTask();

            jkJpackage.addOptions("--runtime-image", customJrePath().toString());
        }

        // Run jpackage
        jkJpackage.prepareOutputDir();
        jkJpackage.run();
        JkLog.endTask();
    }

    @JkDoc("Prints JPMS module dependencies computed by jdeps.")
    public void computeModuleDeps() {
        jdeps().getModuleDeps(mainJar(), projectKBean.project.jpmsModules.getModulePaths());
    }

    @JkDoc("Prints jpackage help on the console.")
    public void jpackageHelp() {
        jpackage().printHelp();
    }

    @JkDoc("Prints jlink help on the console.")
    public void jlinkHelp() {
        jlink().printHelp();
    }

    /**
     * Adds the provided options to the list of jpackage options for this bundle.
     *
     * @param options the jpackage options to be added
     */
    public BundleKBean addJpackageOptions(String... options) {
        this.jpackageOptions.add(options);
        return this;
    }

    /**
     * Adds the provided options to the list of jlink options for this bundle.
     *
     * @param options the jlink options to be added
     */
    public BundleKBean addJlinkOptions(String... options) {
        this.jlinkOptions.add(options);
        return this;
    }

    public static class JpackageConfig {

        private JpackageConfig() {}

        @JkDoc
        public final JpackageOptions options = new JpackageOptions();

    }

    public static class JlinkConfig {

        private JlinkConfig() {}

        @JkDoc
        public final JlinkOptions options = new JlinkOptions();

    }

    public static class JpackageOptions {

        private JpackageOptions() {}

        @JkDoc("Extra options to pass to jpackage (e.g. 'jpackage.options.all.--name=myapp')")
        @JkSuggest(value = {ADD_MODULES, "--java-options", "--name", "--runtime-image", "--vendor", "--description",
                "--verbose"},
                    multiValues = {"ALL-MODULE-PATH", JDEPS, "app-image" })

        public JkMultiValue<String> all = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jpackage only when running on Windows. (e.g. 'jpackage.options.windows.--icon=media/icon.ico')")
        @JkSuggest(value = {"--icon","--type", "--add-launcher"}, multiValues = {"app-image", "exe", "msi"})
        public JkMultiValue<String> windows = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jpackage only when running on Linux. (e.g. 'jpackage.options.linux.--icon=media/icon.png')")
        @JkSuggest(value = {"--icon","--type"}, multiValues = {"app-image", "deb", "rpm"})
        public JkMultiValue<String> linux = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jpackage only when running on MacOS. (e.g. 'jpackage.options.mac.--icon=media/icon.icns')")
        @JkSuggest(value = {"--icon","--type"}, multiValues = {"app-image", "dmg", "pkg"})
        public JkMultiValue<String> mac = JkMultiValue.of(String.class);

        private List<String[]> toOptions() {
            List<String[]> options = new LinkedList<>(toOptionArray(all));
            JkMultiValue<String> osOptions = getCurrentOsOptions();
            options.addAll(toOptionArray(osOptions));
            return options;
        }

        private JkMultiValue<String> getCurrentOsOptions() {
            if (JkUtilsSystem.IS_WINDOWS) {
                return windows;
            }
            if (JkUtilsSystem.IS_MACOS) {
                return mac;
            }
            return linux;
        }

        private String getEffectiveOption(String optionName) {
            return Optional.ofNullable(getCurrentOsOptions().get(optionName))
                    .orElse(all.get(optionName));
        }
    }

    public static class JlinkOptions {

        private JlinkOptions() {}

        @JkDoc("Extra options to pass to jlink (e.g. 'jlink.options.all.--compress=zip-6')")
        @JkSuggest({ADD_MODULES, "--bind-services","--compress", "--disable-plugin", "--limit-module"})
        public JkMultiValue<String> all = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jlink only when running on Windows.")
        @JkSuggest({"--launcher","--type"})
        public JkMultiValue<String> windows = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jlink only when running on Linux.")
        @JkSuggest({"--icon","--type"})
        public JkMultiValue<String> linux = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jlink only when running on MacOS.")
        @JkSuggest({"--icon","--type"})
        public JkMultiValue<String> mac = JkMultiValue.of(String.class);

        private List<String[]> toOptions() {
            List<String[]> options = new LinkedList<>(toOptionArray(all));
            if (JkUtilsSystem.IS_WINDOWS) {
                options.addAll(toOptionArray(windows));
            } else if (JkUtilsSystem.IS_MACOS) {
                options.addAll(toOptionArray(mac));
            } else {
                options.addAll(toOptionArray(linux));
            }
            return options;
        }
    }

    private void fillJklinkOptions(JkJlink jkJlink) {
        JkProject.JpmsModules jpmsModules = projectKBean.project.jpmsModules;
        JkPathSequence modulePath = jpmsModules.getModulePaths();
        boolean addModulesSetByUser = jlinkOptions.stream()
                .flatMap(Arrays::stream)
                .anyMatch(option -> option.equals(ADD_MODULES) || option.startsWith(ADD_MODULES + "="));

        // jlink does not accept empty values
        List<String> modules = jpmsModules.getAddModules();
        if (modules.isEmpty() && !addModulesSetByUser) {
            modules = computeModules(modulePath);
        }
        if (!modules.isEmpty()) {
            jkJlink.addOptions(ADD_MODULES, String.join(",", modules));
        }
        if (!modulePath.toList().isEmpty()) {
            jkJlink.addOptions(MODULE_PATH, modulePath.toPath());
        }
        jkJlink.addOptions("--output", customJrePath().toString());
    }

    // Computes the modules required by the main jar and, if they are bundled, the runtime libs.
    private List<String> computeModules(JkPathSequence modulePath) {
        List<Path> jars = new LinkedList<>();
        jars.add(mainJar());
        if (includeRuntimeLibs) {
            jars.addAll(projectKBean.project.pack.resolveRuntimeDependenciesAsFiles());
        }
        JkJdeps jdeps = jdeps();
        return jars.stream()
                .filter(Files::isRegularFile)
                .flatMap(jar -> jdeps.getModuleDeps(jar, modulePath).stream())
                .map(String::trim)
                .filter(module -> !module.isEmpty())
                .distinct()
                .toList();
    }

    private Path customJrePath() {
        return getOutputDir().resolve("jlink/custom-jre");
    }

    private Path mainJar() {
        return projectKBean.project.artifactLocator.getMainArtifactPath();
    }

    private static List<String[]> toOptionArray(JkMultiValue<String> multiValue) {
        return multiValue.getEntries().stream()
                        .map(entry -> Stream.concat(
                                Stream.of(entry.getKey()),
                                Arrays.stream(JkUtilsString.parseCommandline(entry.getValue()))).toArray(String[]::new)
                        )
                        .toList();
    }

    private void setProjectOptions(JkJpackage jkPackage, Path jar) {
        JkProject project = projectKBean.project;

        Path inputDir = projectKBean.getOutputDir().resolve(JPACKAGE_TEMP_DIR_NAME);
        createInputDir(inputDir, jar);

        String mainClass = project.pack.getOrFindMainClass();
        String mainJarFileName = project.artifactLocator.getMainArtifactPath().getFileName().toString();

        String appName = JkUtilsString.substringBeforeLast(mainJarFileName, ".jar");

        jkPackage
                .addOptions("--input", inputDir.toString())
                .addOptions("--dest", outputDir().toString())
                .addOptions("--main-class", mainClass)
                .addOptions("--main-jar", mainJarFileName)
                .addOptions("--name", appName);

        JkPathSequence projectModulePath = project.jpmsModules.getModulePaths();
        JkLog.debug("Module path from project.jpmsModules: " + projectModulePath);
        if (!projectModulePath.toList().isEmpty()
                && !customJre
                && this.jpackage.options.getEffectiveOption(MODULE_PATH) == null) {
            jkPackage.addOptions(MODULE_PATH, projectModulePath.toPath());
        }

        if (JDEPS.equals(jpackage.options.getEffectiveOption(ADD_MODULES))
                && !customJre) {
            List<String> addModules = jdeps().getModuleDeps(project.artifactLocator.getMainArtifactPath(), projectModulePath);

            if (JkLog.isDebug()) {
                JkLog.debug("Detected Modules by JDeps:", addModules);
                JkLog.debug("List of modules present in JDK:");
                ModuleLayer.boot()
                        .modules()
                        .forEach(m -> JkLog.debug(m.getName()));
            }
            jkPackage.addOptions(ADD_MODULES, String.join(",", addModules));
        }
    }

    private void createInputDir(Path inputDir, Path jar) {
        JkUtilsPath.createDirectories(inputDir);

        JkProject project = projectKBean.project;

        JkUtilsPath.createDirectories(inputDir);

        // Copy main jar
        JkUtilsPath.copy(jar, inputDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);

        // Copy libs
        if (includeRuntimeLibs) {
            Path libDir = inputDir.resolve("lib");
            JkUtilsPath.createDirectories(libDir);
            project.pack.resolveRuntimeDependenciesAsFiles().forEach(file ->
                    JkUtilsPath.copy(file, libDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING));
        }
    }

    private Path outputDir() {
        return projectKBean.project.getOutputDir();
    }

    private JkJpackage jpackage() {
        return JkJpackage.of(jdkTool("jpackage"));
    }

    private JkJlink jlink() {
        return JkJlink.of(jdkTool("jlink"));
    }

    private JkJdeps jdeps() {
        String javaVersion = projectKBean.project.getEffectiveJavaVersion();

        // Analyze the classes that run on the bundled Java version
        return JkJdeps.of(jdkTool("jdeps")).setMultiRelease(javaVersion);
    }

    private Path jdkTool(String name) {
        return jdkHome().resolve("bin").resolve(name + (JkUtilsSystem.IS_WINDOWS ? ".exe" : ""));
    }

    // Non-private for testing. As for compilation, a JDK set with 'jeka.jdk.[version]' prevails.
    Path jdkHome() {
        String javaVersion = projectKBean.project.getEffectiveJavaVersion();
        String explicitHome = getRunbase().getProperties().get("jeka.jdk." + javaVersion);
        if (!JkUtilsString.isBlank(explicitHome)) {
            return Paths.get(explicitHome.trim());
        }
        String distribution = Optional.ofNullable(getRunbase().getProperties().get(JkConstants.JEKA_JAVA_DISTRIB_PROP))
                .orElse("temurin");
        return JkUtilsJdk.getJdk(distribution, javaVersion);
    }


}
