package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.tools.JkJdeps;
import dev.jeka.core.api.java.tools.JkJlink;
import dev.jeka.core.api.java.tools.JkJpackage;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.*;

import java.nio.file.Files;
import java.nio.file.Path;
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
public final class BundleKBean extends KBean {

    public static final String JPACKAGE_TEMP_DIR_NAME = "jpackage-input";

    @JkDoc("If true, a custom JRE is created including only Java modules used by the application.")
    public boolean customJre;

    @JkDoc("If true, creates a bundled application along with the regular JAR when executing 'project: pack'.")
    public boolean projectPack;

    @JkDoc("It true, the runtimes libs will be added in the packaged application, which may be not necessary whe using FAT jars.")
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
        JkJpackage jpackage = jpackage();

        // Set Project options
        setProjectOptions(jpackage);

        // Add extra options
        jpackageOptions.forEach(jpackage::addOptions);

        if (customJre) {

            // Create custom JRE
            JkJlink jkJlink = jlink();
            fillJklinkOptions(jkJlink);
            jlinkOptions.forEach(jkJlink::addOptions);
            JkUtilsPath.deleteQuietly(customJrePath(), false);
            jkJlink.run();

            jpackage.addOptions("--runtime-image", customJrePath().toString());
        }

        // Run jpackage
        jpackage.prepareOutputDir();
        jpackage.run();
    }

    @JkDoc("Prints JPMS module dependencies on the console.")
    public void showModuleDeps() {
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

        @JkDoc("Extra options to pass to jpackage (e.g. 'options.all=--name=myapp)")
        @JkSuggest({"--add-modules", "--java-options", "--name","--runtime-image", "--vendor", "--description", "--verbose"})
        public JkMultiValue<String> all = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jpackage only when running on Windows. (e.g. 'options.windows--icon=media/icon.ico)")
        @JkSuggest({"--icon","--type", "--add-launcher"})
        public JkMultiValue<String> windows = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jpackage only when running on Linux. (e.g. 'options.linux--icon=media/icon.png)")
        @JkSuggest({"--icon","--type"})
        public JkMultiValue<String> linux = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jpackage only when running on MacOS. (e.g. 'mac.options.--icon=media/icon.icns)")
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

    public static class JlinkOptions {

        private JlinkOptions() {}

        @JkDoc("Extra options to pass to jlink (e.g. 'options.all=--name=myapp)")
        @JkSuggest({"--add-modules", "--bind-services","--compress", "--disable-plugin", "--limit-module"})
        public JkMultiValue<String> all = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jlink only when running on Windows.")
        @JkSuggest({"--launcher","--type"})
        public JkMultiValue<String> windows = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jpackage only when running on Linux.)")
        @JkSuggest({"--icon","--type"})
        public JkMultiValue<String> linux = JkMultiValue.of(String.class);

        @JkDoc("Extra options to pass to jpackage only when running on MacOS.")
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
        List<String> moduleDeps = jpmsModules.getAddModules();
        JkPathSequence modulePath = jpmsModules.getModulePaths();
        jkJlink
                .addOptions("--add-modules", String.join(",", moduleDeps))
                .addOptions("--module-path", String.join(",", modulePath.toPath()))
                .addOptions("--output", customJrePath().toString());
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

    private void setProjectOptions(JkJpackage jkPackage) {
        JkProject project = projectKBean.project;

        Path inputDir = projectKBean.getOutputDir().resolve(JPACKAGE_TEMP_DIR_NAME);
        createInputDir(inputDir);

        String mainClass = project.pack.getOrFindMainClass();
        Path mainJar = project.artifactLocator.getMainArtifactPath().getFileName();
        if (!Files.exists(mainJar)) {
            project.pack.run();
        }

        String appName = JkUtilsString.substringBeforeLast(mainJar.toString(), ".jar");

        jkPackage
                .addOptions("--input", inputDir.toString())
                .addOptions("--dest", outputDir().toString())
                .addOptions("--main-class", mainClass)
                .addOptions("--main-jar", mainJar.toString())
                .addOptions("--name", appName);

        JkPathSequence modulePath = project.jpmsModules.getModulePaths();
        if (!modulePath.toList().isEmpty() && !customJre) {
            jkPackage.addOptions("--module-path", modulePath.toPath());
        }
        List<String> addModules = jdeps().getModuleDeps(project.artifactLocator.getMainArtifactPath(), modulePath);
        if (!addModules.isEmpty() && !customJre) {
            jkPackage.addOptions("--add-modules", String.join(",", addModules));
        }
    }

    private void createInputDir(Path inputDir) {
        JkUtilsPath.createDirectories(inputDir);

        JkProject project = projectKBean.project;
        Path jar = project.artifactLocator.getMainArtifactPath();

        if (!Files.exists(jar)) {
            project.pack.run();
        }
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
        String distribution = Optional.ofNullable(getRunbase().getProperties().get(JkConstants.JEKA_JAVA_DISTRIB_PROP))
                .orElse("temurin");
        return JkJpackage.ofJavaVersion(projectKBean.project.getEffectiveJavaVersion(), distribution);
    }

    private JkJlink jlink() {
        String distribution = Optional.ofNullable(getRunbase().getProperties().get(JkConstants.JEKA_JAVA_DISTRIB_PROP))
                .orElse("temurin");
        return JkJlink.ofJavaVersion(projectKBean.project.getEffectiveJavaVersion(), distribution);
    }

    private JkJdeps jdeps() {
        String distribution = Optional.ofNullable(getRunbase().getProperties().get(JkConstants.JEKA_JAVA_DISTRIB_PROP))
                .orElse("temurin");
        return JkJdeps.ofJavaVersion(projectKBean.project.getEffectiveJavaVersion(), distribution);
    }


}
