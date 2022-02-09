package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactProducer;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectConstruction;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkPom;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.core.tool.builtins.scaffold.JkScaffolder;
import dev.jeka.core.tool.builtins.scaffold.ScaffoldJkBean;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@JkDoc("Configure project KBean in order to produce bootable springboot jar and war files.")
public final class SpringbootJkBean extends JkBean {

    private static String DEFAULT_SPRINGBOOT_VERSION = "2.5.6";

    public static final JkArtifactId ORIGINAL_ARTIFACT = JkArtifactId.of("original", "jar");

    private static final String SPRINGBOOT_APPLICATION_ANNOTATION_NAME =
            "org.springframework.boot.autoconfigure.SpringBootApplication";

    private static final String BOM_COORDINATE = "org.springframework.boot:spring-boot-dependencies::pom:";

    public static final String SPRING_BOOT_VERSION_MANIFEST_ENTRY = "Spring-Boot-Version";

    @JkDoc("Version of Spring Boot version used to resolve dependency versions.")
    private String springbootVersion = DEFAULT_SPRINGBOOT_VERSION;

    @JkDoc("Command arg line to pass to springboot for #run method (e.g. '--server.port=8083 -Dspring.profiles.active=prod'")
    public String runArgs;

    @JkDoc("If true, generate a bootable jar artifact.")
    public boolean createBooJar = true;

    @JkDoc("If true, generate the original jar artifact (jar without embedded dependencies")
    public boolean createOriginalJar;

    @JkDoc("If true, generate a .war filed.")
    public boolean createWar;

    @JkDoc("For internal test purpose. If not null, scaffolded build class will reference this classpath for springboot plugin dependency.")
    public String scaffoldDefClasspath;

    private String mainClassName;

    private boolean useSpringRepos = true;

    ProjectJkBean projectBean = getBean(ProjectJkBean.class).configure(this::configure);

    private ScaffoldJkBean scaffoldBean = getBean(ScaffoldJkBean.class).configure(this::configure);


    public SpringbootJkBean setSpringbootVersion(String springbootVersion) {
        this.springbootVersion = springbootVersion;
        return this;
    }

    @JkDoc("Run Springboot application from the generated jar")
    public void run() {
        JkArtifactProducer artifactProducer = projectBean.getProject().getArtifactProducer();
        JkArtifactId mainArtifactId = artifactProducer.getMainArtifactId();
        artifactProducer.makeMissingArtifacts(mainArtifactId);
        Path mainArtifactFile = artifactProducer.getMainArtifactPath();
        String[] args = new String[0];
        if (!JkUtilsString.isBlank(this.runArgs)) {
            args = JkUtilsString.translateCommandline(this.runArgs);
        }
        JkJavaProcess.ofJavaJar(mainArtifactFile, null).exec(args);
    }

    private void configure(JkProject project) {

        // Add spring snapshot or milestone repos if necessary
        JkDependencyResolver dependencyResolver = project.getConstruction().getDependencyResolver();
        JkVersion version = JkVersion.of(springbootVersion);
        if (useSpringRepos && version.hasBlockAt(3)) {
            JkRepoSet repos = JkSpringRepos.getRepoForVersion(version.getBlock(3));
            dependencyResolver.addRepos(repos);
        }

        // run tests in forked mode
        project.getConstruction().getTesting().getTestProcessor().setForkingProcess(true);

        // Do not publish javadoc and sources
        project.includeJavadocAndSources(false, false);

        // Add springboot version to Manifest
        project.getConstruction().getManifest().addMainAttribute(SPRING_BOOT_VERSION_MANIFEST_ENTRY,
                this.springbootVersion);

        // resolve dependency versions upon springboot provided ones
        project.getConstruction().getCompilation().configureDependencies(deps -> deps
            .andBom(BOM_COORDINATE + springbootVersion));

        // define bootable jar as main artifact
        JkStandardFileArtifactProducer artifactProducer = project.getArtifactProducer();
        if (this.createBooJar) {
            Consumer<Path> bootJarMaker = path -> this.createBootJar(project, path);
            artifactProducer.putMainArtifact(bootJarMaker);
        }
        if (this.createWar) {
            Consumer<Path> warMaker = path -> JkJ2eWarProjectAdapter.of().generateWar(path, project);
            artifactProducer.putArtifact("", "war", warMaker);
        }

        // add original jar artifact
        if (createOriginalJar) {
            Consumer<Path> makeBinJar = project.getConstruction()::createBinJar;
            artifactProducer.putArtifact(ORIGINAL_ARTIFACT, makeBinJar);
        }
    }

    private void configure (JkScaffolder scaffolder) {
        String code = JkUtilsIO.read(SpringbootJkBean.class.getClassLoader().getResource("snippet/Build.java"));
        String defClasspath = scaffoldDefClasspath != null ? scaffoldDefClasspath.replace("\\", "/") : "dev.jeka:springboot-plugin";
        code = code.replace("${dependencyDescription}", defClasspath);
        code = code.replace("${springbootVersion}", latestSpringbootVersion(projectBean.getProject()));
        final String jkClassCode = code;
        scaffolder.setJekaClassCodeProvider(() -> jkClassCode);
        scaffolder.getExtraActions() .append(this::scaffoldSample);
    }

    private void createBootJar(JkProject project) {
        JkStandardFileArtifactProducer artifactProducer = project.getArtifactProducer();
        createBootJar(project, artifactProducer.getMainArtifactPath());
    }

    /**
     * Creates the bootable jar at the specified location.
     */
    private void createBootJar(JkProject project, Path target) {
        JkProjectConstruction construction = project.getConstruction();
        JkStandardFileArtifactProducer artifactProducer = project.getArtifactProducer();
        JkDependencyResolver dependencyResolver = construction.getDependencyResolver();
        JkVersionProvider versionProvider = JkVersionProvider.of().andBom(BOM_COORDINATE + springbootVersion)
                        .withResolvedBoms(project.getConstruction().getDependencyResolver().getRepos());
        JkVersion loaderVersion = versionProvider.getVersionOf(JkSpringModules.Boot.LOADER);
        JkDependencySet bootloaderDependency = JkDependencySet.of(JkModuleDependency.of(JkSpringModules.Boot.LOADER))
                .andBom(BOM_COORDINATE + springbootVersion);
        Path bootloader = dependencyResolver.resolve(bootloaderDependency).getFiles().getEntry(0);
        final JkPathSequence embeddedJars = construction.getDependencyResolver().resolve(
                construction.getRuntimeDependencies().normalised(project.getDuplicateConflictStrategy()))
                .getFiles();
        Path originalJarPath = project.getArtifactProducer().getArtifactPath(ORIGINAL_ARTIFACT);
        if (!Files.exists(originalJarPath)) {
            construction.createBinJar(originalJarPath);
        }
        createBootJar(originalJarPath, embeddedJars, bootloader,
                artifactProducer.getMainArtifactPath(), springbootVersion);
    }

    public void createBootJar() {
        createBootJar(projectBean.getProject());
    }

    public ProjectJkBean projectBean() {
        return projectBean;
    }

    private static JkPom getSpringbootBom(JkDependencyResolver dependencyResolver, String springbootVersion) {
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
        Path sourceDir = projectBean.getProject().getConstruction().getCompilation().getLayout()
                .getSources().getRootDirsOrZipFiles().get(0);
        Path pack = sourceDir.resolve(basePackage);
        URL url = SpringbootJkBean.class.getClassLoader().getResource("snippet/Application.java");
        JkPathFile.of(pack.resolve("Application.java")).createIfNotExist().fetchContentFrom(url);
        url = SpringbootJkBean.class.getClassLoader().getResource("snippet/Controller.java");
        JkPathFile.of(pack.resolve("Controller.java")).createIfNotExist().fetchContentFrom(url);
        Path testSourceDir = projectBean.getProject().getConstruction().getTesting().getCompilation().getLayout()
                .getSources().getRootDirsOrZipFiles().get(0);
        pack = testSourceDir.resolve(basePackage);
        url = SpringbootJkBean.class.getClassLoader().getResource("snippet/ControllerIT.java");
        JkPathFile.of(pack.resolve("ControllerIT.java")).createIfNotExist().fetchContentFrom(url);
    }

    private String pluginVersion() {
        return JkManifest.of().loadFromClass(SpringbootJkBean.class)
                .getMainAttribute(JkManifest.IMPLEMENTATION_VERSION);
    }

    private String latestSpringbootVersion(JkProject project) {
        try {
            List<String> springbootVersions = project.getConstruction().getDependencyResolver()
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

    public SpringbootJkBean setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
        return this;
    }

    /**
     *  If true, Spring Milestone or Snapshot Repository will be used to fetch non release version of spring modules.
     */
    public SpringbootJkBean setUseSpringRepos(boolean useSpringRepos) {
        this.useSpringRepos = useSpringRepos;
        return this;
    }
}
