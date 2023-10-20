package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkPom;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 *  Configurator for configuring a {@link JkProject}  to be a Spring-Boot project.
 */
public final class JkSpringbootProjectAdapter {

    private static final String DEFAULT_SPRINGBOOT_VERSION = "3.1.4";

    public static final JkArtifactId ORIGINAL_ARTIFACT = JkArtifactId.of("original", "jar");

    private static final String SPRINGBOOT_APPLICATION_ANNOTATION_NAME =
            "org.springframework.boot.autoconfigure.SpringBootApplication";

    private static final String BOM_COORDINATE = "org.springframework.boot:spring-boot-dependencies::pom:";

    private static final String LOADER_COORDINATE = "org.springframework.boot:spring-boot-loader:";

    public static final String SPRING_BOOT_VERSION_MANIFEST_ENTRY = "Spring-Boot-Version";

    private String springbootVersion = DEFAULT_SPRINGBOOT_VERSION;

    private boolean createBootJar = true;

    private boolean createOriginalJar;

    private boolean createWarFile;

    private boolean useSpringRepos = true;

    private JkSpringbootProjectAdapter() {
    }

    public static JkSpringbootProjectAdapter of() {
        return new JkSpringbootProjectAdapter();
    }

    /**
     *  If true, Spring Milestone or Snapshot Repository will be used to fetch non release version of spring modules.
     */
    public JkSpringbootProjectAdapter setUseSpringRepos(boolean useSpringRepos) {
        this.useSpringRepos = useSpringRepos;
        return this;
    }

    public JkSpringbootProjectAdapter setSpringbootVersion(String springbootVersion) {
        this.springbootVersion = springbootVersion;
        return this;
    }

    public JkSpringbootProjectAdapter setCreateBootJar(boolean createBootJar) {
        this.createBootJar = createBootJar;
        return this;
    }

    public JkSpringbootProjectAdapter setCreateOriginalJar(boolean createOriginalJar) {
        this.createOriginalJar = createOriginalJar;
        return this;
    }

    public JkSpringbootProjectAdapter setCreateWarFile(boolean createWarFile) {
        this.createWarFile = createWarFile;
        return this;
    }

    /**
     * Configures the specified project for being a Spring-Boot project.
     */
    public void configure(JkProject project) {

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

        // Add springboot version to Manifest
        project.packaging.manifest.addMainAttribute(SPRING_BOOT_VERSION_MANIFEST_ENTRY,
                this.springbootVersion);

        // resolve dependency versions upon springboot provided ones
        project.compilation.configureDependencies(deps -> deps
            .andBom(BOM_COORDINATE + springbootVersion));

        // define bootable jar as main artifact
        JkStandardFileArtifactProducer artifactProducer = project.artifactProducer;
        if (this.createBootJar) {
            Consumer<Path> bootJarMaker = path -> this.createBootJar(project, path);
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

        // To deploy springboot app in a container, we don't need to create a jar
        // This is more efficient to keep the structure exploded to have efficient image layering.
        // In this case, just copy manifest in class dir is enough.
        if (!createBootJar && !createOriginalJar && !createWarFile) {
            artifactProducer.putMainArtifact(project.packaging::includeManifestInClassDir);
        }
    }

    /**
     * Creates the bootable jar at the conventional location.
     */
    public Path createBootJar(JkProject project) {
        JkStandardFileArtifactProducer artifactProducer = project.artifactProducer;
        Path target = artifactProducer.getMainArtifactPath();
        createBootJar(project, target);
        return target;
    }

    /**
     * Creates the bootable jar at the specified location.
     */
    public void createBootJar(JkProject project, Path target) {
        Path originalJarPath = project.artifactProducer.getArtifactPath(ORIGINAL_ARTIFACT);
        if (!Files.exists(originalJarPath)) {
            project.packaging.createBinJar(originalJarPath);
        }
        JkLog.startTask("Packaging bootable jar");
        JkStandardFileArtifactProducer artifactProducer = project.artifactProducer;
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        JkCoordinateFileProxy loaderProxy = JkCoordinateFileProxy.of(dependencyResolver.getRepos(),
                LOADER_COORDINATE + springbootVersion);
        Path bootloader = loaderProxy.get();
        final JkPathSequence embeddedJars = dependencyResolver.resolve(
                project.packaging.getRuntimeDependencies().normalised(project.getDuplicateConflictStrategy()))
                .getFiles();
        createBootJar(originalJarPath, embeddedJars, bootloader,
                target, springbootVersion);
        JkLog.endTask();
    }

    /**
     * Returns the latest GA Spring-Boot version
     */
    public static String latestSpringbootVersion(JkProject project) {
        try {
            List<String> springbootVersions = project.dependencyResolver
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

    /**
     * Returns the Pom of the
     * @param dependencyResolver
     * @param springbootVersion
     * @return
     */
    public static JkPom springbootBom(JkDependencyResolver dependencyResolver, String springbootVersion) {
        JkCoordinateDependency coordinateDependency = JkCoordinateDependency.of(
                "org.springframework.boot:spring-boot-dependencies::pom:" + springbootVersion);
        JkLog.info("Fetch Springboot dependency versions from " + coordinateDependency);
        Path pomFile = dependencyResolver.resolve(coordinateDependency).getFiles().getEntries().get(0);
        if (pomFile == null || !Files.exists(pomFile)) {
            throw new IllegalStateException(coordinateDependency + " not found");
        }
        JkLog.info("Springboot dependency versions will be resolved from " + pomFile);
        return JkPom.of(pomFile);
    }

    public static void createBootJar(Path original, JkPathSequence libsToInclude, Path bootLoaderJar, Path targetJar,
                                     String springbootVersion) {
        JkUtilsAssert.argument(Files.exists(original), "Original jar not found at " + original);
        String mainClassName = findMainClassName(original);

        SpringbootPacker.of(libsToInclude, bootLoaderJar, mainClassName,
                springbootVersion).makeExecJar(original, targetJar);
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

    /**
     * Returns fully qualified name of springboot main class. This method should be invoked only after compilation.
     */
    public static String getMainClass(JkProject project) {
        return JkSpringbootProjectAdapter.findMainClassName(project.compilation.layout.resolveClassDir());
    }

}
