package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkModuleDependency;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactProducer;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectConstruction;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkPom;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.scaffold.JkPluginScaffold;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@JkDoc("Provides enhancement to Java plugin in order to produce a startable Springboot jar for your application.\n" +
        "The main produced artifact is the springboot one (embedding all dependencies) while the artifact classified as 'original' stands for the vanilla jar.\n" +
        "Dependency versions are resolved against BOM provided by Spring Boot team according Spring Boot version you use.")
@JkDocPluginDeps(JkPluginJava.class)
public final class JkPluginSpringboot extends JkPlugin {

    private static String DEFAULT_SPRINGBOOT_VERSION = "2.5.6";

    public static final JkArtifactId ORIGINAL_ARTIFACT = JkArtifactId.of("original", "jar");

    private static final String SPRINGBOOT_APPLICATION_ANNOTATION_NAME =
            "org.springframework.boot.autoconfigure.SpringBootApplication";

    public static final String SPRING_BOOT_VERSION_MANIFEST_ENTRY = "Spring-Boot-Version";

    @JkDoc("Version of Spring Boot version used to resolve dependency versions.")
    private String springbootVersion = DEFAULT_SPRINGBOOT_VERSION;

    @JkDoc("Class name holding main method to start Spring Boot. If null, Jeka will try to guess it at build time.")
    public String mainClassName;

    @JkDoc("If true, Spring Milestone or Snapshot Repository will be used to fetch non release version of spring modules")
    public boolean autoSpringRepo = true;

    @JkDoc("Command arg line to pass to springboot for #run method (e.g. '--server.port=8083 -Dspring.profiles.active=prod'")
    public String runArgs;

    @JkDoc("If true, the build create also the original jar file (without embedded dependencies")
    public boolean createOriginalJar;

    @JkDoc("For internal test purpose. If not null, scaffolded build class will reference this classpath for springboot plugin dependency.")
    public String scaffoldDefClasspath;

    private final JkPluginJava java;

    private JkPom cachedSpringbootBom;

    /**
     * Right after to be instantiated, plugin instances are likely to configured by the owning build.
     * Therefore, every plugin members that are likely to be configured by the owning build must be
     * initialized in the constructor.
     */
    protected JkPluginSpringboot(JkClass jkClass) {
        super(jkClass);
        java = jkClass.getPlugins().get(JkPluginJava.class);
    }

    public void setSpringbootVersion(String springbootVersion) {
        this.springbootVersion = springbootVersion;
    }

    @Override
    @JkDoc("Modifies the Java project from Java plugin in such this project produces a SpringBoot jar as the main artifact.")
    protected void afterSetup() {
        configure(java.getProject());
    }

    @JkDoc("Run Springboot application from the generated jar")
    public void run() {
        JkArtifactProducer artifactProducer = java.getProject().getPublication().getArtifactProducer();
        JkArtifactId mainArtifactId = artifactProducer.getMainArtifactId();
        artifactProducer.makeMissingArtifacts(mainArtifactId);
        Path mainArtifactFile = artifactProducer.getMainArtifactPath();
        String[] args = new String[0];
        if (!JkUtilsString.isBlank(this.runArgs)) {
            args = JkUtilsString.translateCommandline(this.runArgs);
        }
        JkJavaProcess.ofJavaJar(mainArtifactFile, null).exec(args);
    }

    @JkDoc("Run Springboot application from the generated jar")
    public void runAsync() {
        JkArtifactProducer artifactProducer = java.getProject().getPublication().getArtifactProducer();
        JkArtifactId mainArtifactId = artifactProducer.getMainArtifactId();
        artifactProducer.makeMissingArtifacts(mainArtifactId);
        Path mainArtifactFile = artifactProducer.getMainArtifactPath();
        String[] args = new String[0];
        if (!JkUtilsString.isBlank(this.runArgs)) {
            args = JkUtilsString.translateCommandline(this.runArgs);
        }
        JkJavaProcess.ofJavaJar(mainArtifactFile, null).exec(args);
    }


    private void configure(JkJavaProject project) {

        // Add spring snapshot or milestone repos if necessary
        JkDependencyResolver dependencyResolver = project.getConstruction().getDependencyResolver();
        JkVersion version = JkVersion.of(springbootVersion);
        if (autoSpringRepo && version.hasBlockAt(3)) {
            JkRepoSet repos = JkSpringRepos.getRepoForVersion(version.getBlock(3));
            dependencyResolver.addRepos(repos);
        }

        // run tests in forked mode
        project.getConstruction().getTesting().getTestProcessor().setForkingProcess(true);

        // Do not publish javadoc and sources
        project.getPublication().includeJavadocAndSources(false);

        // Add springboot version to Manifest
        project.getConstruction().getManifest().addMainAttribute(SPRING_BOOT_VERSION_MANIFEST_ENTRY,
                this.springbootVersion);

        // resolve dependency versions upon springboot provided ones
        JkVersionProvider versionProvider = getSpringbootPom(dependencyResolver, springbootVersion).getVersionProvider();
        project.getConstruction().getCompilation().setDependencies(deps -> deps
            .andVersionProvider(versionProvider));

        // define bootable jar as main artifact
        JkStandardFileArtifactProducer artifactProducer = project.getPublication().getArtifactProducer();
        Consumer<Path> bootJar = this::createBootJar;
        artifactProducer.putMainArtifact(bootJar);

        // add original jar artifact
        if (createOriginalJar) {
            Consumer<Path> makeBinJar = project.getConstruction()::createBinJar;
            artifactProducer.putArtifact(ORIGINAL_ARTIFACT, makeBinJar);
        }

        // Add template build class to scaffold
        if (this.getJkClass().getPlugins().hasLoaded(JkPluginScaffold.class)) {
            JkPluginScaffold scaffold = this.getJkClass().getPlugins().get(JkPluginScaffold.class);
            String code = JkUtilsIO.read(JkPluginSpringboot.class.getClassLoader().getResource("snippet/Build.java"));
            String defClasspath = scaffoldDefClasspath != null ? scaffoldDefClasspath.replace("\\", "/") : "dev.jeka:springboot-plugin";
            code = code.replace("${dependencyDescription}", defClasspath);
            code = code.replace("${springbootVersion}", latestSpringbootVersion());
            final String jkClassCode = code;
            scaffold.getScaffolder().setJekaClassCodeProvider(() -> jkClassCode);
            scaffold.getScaffolder().getExtraActions()
                .append(this::scaffoldSample);
        }
    }

    /**
     * Creates the bootable jar at the standard location.
     */
    public void createBootJar() {
        JkStandardFileArtifactProducer artifactProducer = java.getProject().getPublication().getArtifactProducer();
        createBootJar(artifactProducer.getMainArtifactPath());
    }

    /**
     * Creates the bootable jar at the specified location.
     */
    public void createBootJar(Path target) {
        JkJavaProjectConstruction construction = java.getProject().getConstruction();
        JkStandardFileArtifactProducer artifactProducer = java.getProject().getPublication().getArtifactProducer();
        JkDependencyResolver dependencyResolver = construction.getDependencyResolver();
        JkVersionProvider versionProvider = getSpringbootPom(dependencyResolver, springbootVersion).getVersionProvider();
        JkVersion loaderVersion = versionProvider.getVersionOf(JkSpringModules.Boot.LOADER);
        JkModuleDependency bootloaderDep =
                JkModuleDependency.of(JkSpringModules.Boot.LOADER.withVersion(loaderVersion.getValue()));
        Path bootloader = dependencyResolver.resolve(bootloaderDep).getFiles().getEntry(0);
        final JkPathSequence embeddedJars = construction.getDependencyResolver().resolve(
                construction.getRuntimeDependencies().normalised(java.getProject().getDuplicateConflictStrategy()))
                .getFiles();
        Path originalJarPath = java.getProject().getPublication().getArtifactProducer().getArtifactPath(ORIGINAL_ARTIFACT);
        if (!Files.exists(originalJarPath)) {
            construction.createBinJar(originalJarPath);
        }
        createBootJar(originalJarPath, embeddedJars, bootloader,
                artifactProducer.getMainArtifactPath(), springbootVersion);
    }

    public JkPluginJava javaPlugin() {
        return java;
    }

    private JkPom getSpringbootPom(JkDependencyResolver dependencyResolver, String springbootVersion) {
        if (cachedSpringbootBom == null) {
            cachedSpringbootBom = getSpringbootBom(dependencyResolver, springbootVersion);
        }
        return cachedSpringbootBom;
    }

    public static JkPom getSpringbootBom(JkDependencyResolver dependencyResolver, String springbootVersion) {
        JkModuleDependency moduleDependency = JkModuleDependency.of(
                "org.springframework.boot:spring-boot-dependencies::pom:" + springbootVersion);
        JkLog.info("Fetch Springboot dependency versions from " + moduleDependency);
        Path pomFile = dependencyResolver.resolve(moduleDependency).getFiles().getEntries().get(0);
        if (pomFile == null || !Files.exists(pomFile)) {
            throw new IllegalStateException(moduleDependency + " not found");
        }
        JkLog.info("Springboot dependency versions will be resolved from " + pomFile);
        return JkPom.of(pomFile);
    }


    public static void createBootJar(Path original, JkPathSequence libsToInclude, Path bootLoaderJar, Path targetJar,
                                     String springbootVersion) {
        JkUtilsAssert.argument(Files.exists(original), "Original jar not found at " + original);
        JkClassLoader classLoader = JkUrlClassLoader.of(original, ClassLoader.getSystemClassLoader().getParent())
                .toJkClassLoader();
        List<String> mainClasses = classLoader.findClassesHavingMainMethod();
        List<String> classWithSpringbootAppAnnotation = classLoader.findClassesMatchingAnnotations(
                annotationNames -> annotationNames.contains(SPRINGBOOT_APPLICATION_ANNOTATION_NAME));
        for (String name : mainClasses) {
            if (classWithSpringbootAppAnnotation.contains(name)) {
                SpringbootPacker.of(libsToInclude, bootLoaderJar, name,
                        springbootVersion).makeExecJar(original, targetJar);
                return;
            }
        }
        throw new IllegalStateException("No @SpringBootApplication class with main method found.");
    }

    @JkDoc("Scaffold a basic example application in package org.example")
    public void scaffoldSample() {
        String basePackage = "your/basepackage";
        Path sourceDir = java.getProject().getConstruction().getCompilation().getLayout()
                .getSources().getRootDirsOrZipFiles().get(0);
        Path pack = sourceDir.resolve(basePackage);
        URL url = JkPluginSpringboot.class.getClassLoader().getResource("snippet/Application.java");
        JkPathFile.of(pack.resolve("Application.java")).createIfNotExist().fetchContentFrom(url);
        url = JkPluginSpringboot.class.getClassLoader().getResource("snippet/Controller.java");
        JkPathFile.of(pack.resolve("Controller.java")).createIfNotExist().fetchContentFrom(url);
        Path testSourceDir = java.getProject().getConstruction().getTesting().getCompilation().getLayout()
                .getSources().getRootDirsOrZipFiles().get(0);
        pack = testSourceDir.resolve(basePackage);
        url = JkPluginSpringboot.class.getClassLoader().getResource("snippet/ControllerIT.java");
        JkPathFile.of(pack.resolve("ControllerIT.java")).createIfNotExist().fetchContentFrom(url);
    }

    private String pluginVersion() {
        return JkManifest.of().loadFromClass(JkPluginSpringboot.class)
                .getMainAttribute(JkManifest.IMPLEMENTATION_VERSION);
    }

    private String latestSpringbootVersion() {
        try {
            List<String> springbootVersions = java.getProject().getConstruction().getDependencyResolver()
                    .searchVersions(JkSpringModules.Boot.STARTER_PARENT);
            return springbootVersions.stream()
                    .sorted(JkVersion.VERSION_COMPARATOR.reversed())
                    .findFirst().get();
        } catch (Exception e) {
            JkLog.warn(e.getMessage());
            JkLog.warn("Cannot find latest springboot version, choose default : " + DEFAULT_SPRINGBOOT_VERSION);
            return DEFAULT_SPRINGBOOT_VERSION;
        }
    }
}
