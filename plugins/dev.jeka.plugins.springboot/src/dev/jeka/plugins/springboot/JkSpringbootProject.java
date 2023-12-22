package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectBuildClassTemplate;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.builtins.scaffold.JkScaffold;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;

/**
 *  Configurator for {@link JkProject} to add Spring-Boot configuration.
 */
public final class JkSpringbootProject {

    public enum ScaffoldBuildKind {
        PURE_API,
        KBEAN
    }

    static final String DEFAULT_SPRINGBOOT_VERSION = "3.2.0";

    public static final JkArtifactId ORIGINAL_ARTIFACT = JkArtifactId.of("original", "jar");

    private static final String SPRINGBOOT_APPLICATION_ANNOTATION_NAME =
            "org.springframework.boot.autoconfigure.SpringBootApplication";
    static final String BOM_COORDINATE = "org.springframework.boot:spring-boot-dependencies::pom:";

    private static final String LOADER_COORDINATE = "org.springframework.boot:spring-boot-loader:";

    public static final String SPRING_BOOT_VERSION_MANIFEST_ENTRY = "Spring-Boot-Version";

    private String springbootVersion = DEFAULT_SPRINGBOOT_VERSION;

    private final JkProject project;

    private boolean createBootJar = true;

    private boolean createOriginalJar;

    private boolean createWarFile;

    private boolean useSpringRepos = true;

    private JkSpringbootProject(JkProject project) {
        this.project = project;
    }

    public static JkSpringbootProject of(JkProject project) {
        return new JkSpringbootProject(project);
    }

    /**
     * Sets if the project build should create a .war artifact. Initial value is <code>false</code>.
     */
    public JkSpringbootProject setCreateWarFile(boolean createWarFile) {
        this.createWarFile = createWarFile;
        return this;
    }

    /**
     *  If true, Spring Milestone or Snapshot Repository will be used to fetch non release version of spring modules.
     */
    public JkSpringbootProject setUseSpringRepos(boolean useSpringRepos) {
        this.useSpringRepos = useSpringRepos;
        return this;
    }

    public JkSpringbootProject setSpringbootVersion(@JkDepSuggest(versionOnly = true, hint = "org.springframework.boot:spring-boot-dependencies:") String springbootVersion) {
        this.springbootVersion = springbootVersion;
        return this;
    }

    public JkSpringbootProject setCreateBootJar(boolean createBootJar) {
        this.createBootJar = createBootJar;
        return this;
    }

    public JkSpringbootProject setCreateOriginalJar(boolean createOriginalJar) {
        this.createOriginalJar = createOriginalJar;
        return this;
    }

    /**
     * Configures the specified project for being a Spring-Boot project.
     */
    public void configure() {

        // Add spring snapshot or milestone repos if necessary
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        JkVersion version = JkVersion.of(springbootVersion);
        if (useSpringRepos && version.hasBlockAt(3)) {
            JkRepoSet repos = JkSpringRepos.getRepoForVersion(version.getBlock(3));
            dependencyResolver.addRepos(repos);
        }

        // run tests in forked mode
        project.testing.testProcessor.setForkingProcess(true);

        // Do not publish javadoc and sources
        project.includeJavadocAndSources(false, false);

        // Add spring-Boot version to Manifest
        project.packaging.manifest.addMainAttribute(SPRING_BOOT_VERSION_MANIFEST_ENTRY,
                this.springbootVersion);

        // resolve dependency versions upon springboot provided ones
        project.compilation.configureDependencies(deps -> deps
            .and(BOM_COORDINATE + springbootVersion));

        // define bootable jar as main artifact
        JkStandardFileArtifactProducer artifactProducer = project.artifactProducer;
        if (this.createBootJar) {
            Consumer<Path> bootJarMaker = path -> this.createBootJar(path);
            artifactProducer.putMainArtifact(bootJarMaker);
        }
        if (this.createWarFile) {
            Consumer<Path> warMaker = path -> JkJ2eWarProjectAdapter.of().generateWar(path, project);
            artifactProducer.putArtifact(JkArtifactId.MAIN_ARTIFACT_NAME, "war", warMaker);
        }

        // add original jar artifact
        if (createOriginalJar) {
            Consumer<Path> makeBinJar = project.packaging::createBinJar;
            artifactProducer.putArtifact(ORIGINAL_ARTIFACT, makeBinJar);
        }

        // To deploy spring-Boot app in a container, we don't need to create a jar
        // This is more efficient to keep the structure exploded to have efficient image layering.
        // In this case, just copy manifest in class dir is enough.
        if (!createBootJar && !createOriginalJar && !createWarFile) {
            artifactProducer.putMainArtifact(project.packaging::includeManifestInClassDir);
        }
    }

    /**
     * Creates the bootable jar at the conventional location.
     * @see #createBootJar(Path)
     */
    public Path createBootJar() {
        JkStandardFileArtifactProducer artifactProducer = project.artifactProducer;
        Path target = artifactProducer.getMainArtifactPath();
        createBootJar(target);
        return target;
    }

    /**
     * Creates the bootable jar at the specified location.
     */
    public void createBootJar(Path target) {
        JkPathTree<?> classTree = JkPathTree.of(project.compilation.layout.resolveClassDir());
        if (!classTree.exists()) {
            project.testing.runIfNeeded();
        }
        JkLog.startTask("Packaging bootable jar");
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        final JkPathSequence embeddedJars = dependencyResolver.resolve(
                project.packaging.getRuntimeDependencies().normalised(project.getDuplicateConflictStrategy()))
                .getFiles();
        createBootJar(classTree, embeddedJars.getEntries(), dependencyResolver.getRepos(), target, springbootVersion);
        JkLog.endTask();
    }

    /**
     * Scaffolds a sample Spring-Boot application at sources and test location.
     */
    public void scaffoldSample() {
        String basePackage = "your/basepackage";
        Path sourceDir = project.compilation.layout
                .getSources().getRootDirsOrZipFiles().get(0);
        Path pack = sourceDir.resolve(basePackage);
        URL url = SpringbootKBean.class.getClassLoader().getResource("snippet/Application.java");
        JkPathFile.of(pack.resolve("Application.java")).createIfNotExist().fetchContentFrom(url);
        url = SpringbootKBean.class.getClassLoader().getResource("snippet/Controller.java");
        JkPathFile.of(pack.resolve("Controller.java")).createIfNotExist().fetchContentFrom(url);
        Path testSourceDir = project.testing.compilation.layout
                .getSources().getRootDirsOrZipFiles().get(0);
        pack = testSourceDir.resolve(basePackage);
        url = SpringbootKBean.class.getClassLoader().getResource("snippet/ControllerIT.java");
        JkPathFile.of(pack.resolve("ControllerIT.java")).createIfNotExist().fetchContentFrom(url);
        JkPathFile.of(project.compilation.layout.getResources()
                .getRootDirsOrZipFiles().get(0).resolve("application.properties")).createIfNotExist();

    }

    void configureScaffold(
            JkScaffold scaffold,
            ScaffoldBuildKind scaffoldBuildKind,
            String scaffoldDefClasspath,  // nullable
            JkProjectBuildClassTemplate projectBuildClassTemplate) {

        String buildClassSource = scaffoldBuildKind == ScaffoldBuildKind.KBEAN ?
                "Build.java" : "BuildPureApi.java";
        String code = JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/" + buildClassSource));
        String defClasspath = scaffoldDefClasspath != null ? scaffoldDefClasspath.replace("\\", "/") : "dev.jeka:springboot-plugin";
        code = code.replace("${dependencyDescription}", defClasspath);
        code = code.replace("${springbootVersion}", springbootVersion);
        final String jkClassCode = code;
        if (projectBuildClassTemplate != JkProjectBuildClassTemplate.CODE_LESS) {
            scaffold.setJekaClassCodeProvider(() -> jkClassCode);
        }
        scaffold.extraActions.append(this::scaffoldSample);
        String readmeContent = JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/README.md"));
        scaffold.extraActions.append(() -> {
            JkPathFile readmeFile = JkPathFile.of(project.getBaseDir().resolve("README.md")).createIfNotExist();
            readmeFile.write(readmeContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        });

    }

    /**
     * Creates a bootable jar from the original jar (the one without Springboot dependencies).
     */
    public static void createBootJar(JkPathTree<?> classTree, List<Path> libsToInclude, JkRepoSet downloadRepo,
                                     Path targetJar,
                                     String springbootVersion) {
        JkUtilsAssert.argument(classTree.exists(), "class dir not found " + classTree.getRoot());
        String mainClassName = findMainClassName(classTree.getRoot());
        JkCoordinateFileProxy loaderProxy = JkCoordinateFileProxy.of(downloadRepo,
                LOADER_COORDINATE + DEFAULT_SPRINGBOOT_VERSION);

        SpringbootPacker.of(libsToInclude, loaderProxy.get(), mainClassName)
                .makeExecJar(classTree, targetJar, springbootVersion);
    }


    private static String findMainClassName(Iterable<Path> jarOrFolder) {
        JkClassLoader classLoader = JkUrlClassLoader.of(jarOrFolder, ClassLoader.getSystemClassLoader().getParent())
                .toJkClassLoader();
        List<String> mainClasses = classLoader.findClassesHavingMainMethod();
        List<String> classWithSpringbootAppAnnotation = classLoader.findClassesMatchingAnnotations(
                annotationNames -> annotationNames.contains(SPRINGBOOT_APPLICATION_ANNOTATION_NAME));
        for (String name : mainClasses) {
            if (classWithSpringbootAppAnnotation.contains(name)) {
                return name;
            }
        }

        // Kotlin adds a special [mainClass]Kt class to host main method
        for (String name : mainClasses) {
            if (name.endsWith("Kt")) {
                String originalName = JkUtilsString.substringBeforeLast(name, "Kt");
                if (classWithSpringbootAppAnnotation.contains(originalName)) {
                    return name;
                }
            }
        }
        throw new IllegalStateException("No class annotated with @SpringBootApplication found.");
    }



}
