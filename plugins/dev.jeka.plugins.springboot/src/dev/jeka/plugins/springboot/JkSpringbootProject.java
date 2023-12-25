package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
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

    public static final JkArtifactId ORIGINAL_ARTIFACT = JkArtifactId.of("original", "jar");

    static final String BOM_COORDINATE = "org.springframework.boot:spring-boot-dependencies::pom:";

    private final JkProject project;


    private JkSpringbootProject(JkProject project) {
        this.project = project;
    }

    /**
     * Creates a {@link JkSpringbootProject} from the specified {@link JkProject}.
     */
    public static JkSpringbootProject of(JkProject project) {
        return new JkSpringbootProject(project);
    }

    /**
     * Configures the underlying project for Spring-Boot.
     * @param createBootJar       Should create or not, a bootable jar file. The bootable jar file will be set as the default artifact.
     * @param createWarFile       Should create or not, a .war file to be deployed in application server
     * @param createOriginalJar   Should create or not, the original jar, meaning the jar containing the application classes, without dependencies.
     */
    public JkSpringbootProject configure(boolean createBootJar, boolean createWarFile,
                          boolean createOriginalJar) {

        // run tests in forked mode
        project.testing.testProcessor.setForkingProcess(true);

        // Do not publish javadoc and sources
        project.includeJavadocAndSources(false, false);

        JkStandardFileArtifactProducer artifactProducer = project.artifactProducer;

        // define bootable jar as main artifact
        if (createBootJar) {
            Consumer<Path> bootJarMaker = path -> this.createBootJar(path);
            artifactProducer.putMainArtifact(bootJarMaker);
        }
        if (createWarFile) {
            Consumer<Path> warMaker = path -> JkJ2eWarProjectAdapter.of().generateWar(path, project);
            artifactProducer.putArtifact(JkArtifactId.MAIN_ARTIFACT_NAME, "war", warMaker);
        }
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

        return this;
    }

    /**
     * Configures the underlying project for Spring-Boot usinf sensitive default
     * @see #configure(boolean, boolean, boolean)
     */
    public JkSpringbootProject configure() {
        return configure(true, false, false);
    }

    /**
     * Includes org.springframework.boot:spring-boot-dependencies BOM in project dependencies.
     * This is the version that determines the effective Spring-Boot version to use.
     * @param version The Spring-Boot version to use.
     * @return This oject for chaining.
     */
    public JkSpringbootProject includeParentBom(String version) {
        project.compilation.configureDependencies(deps -> deps
                .and(BOM_COORDINATE + version));
        return this;
    }

    /**
     * Adds the specified Spring repository to the download repo. It may be necessary
     * to use pre-release versions.
     */
    public JkSpringbootProject addSpringRepo(JkSpringRepo springRepo) {
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        dependencyResolver.addRepos(springRepo.get());
        return this;
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
            project.compilation.runIfNeeded();
            project.testing.runIfNeeded();
        }
        JkLog.startTask("Packaging bootable jar");
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        final JkPathSequence embeddedJars = dependencyResolver.resolve(
                project.packaging.getRuntimeDependencies().normalised(project.getDuplicateConflictStrategy()))
                .getFiles();
        JkSpringbootJars.createBootJar(classTree, embeddedJars.getEntries(), dependencyResolver.getRepos(), target);
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
            JkProjectScaffold.BuildClassTemplate projectBuildClassTemplate) {

        String buildClassSource = scaffoldBuildKind == ScaffoldBuildKind.KBEAN ?
                "BuildKBean.java" : "BuildPureApi.java";
        String code = JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/" + buildClassSource));
        String defClasspath = scaffoldDefClasspath != null ? scaffoldDefClasspath.replace("\\", "/") : "dev.jeka:springboot-plugin";
        code = code.replace("${dependencyDescription}", defClasspath);
        code = code.replace("${springbootVersion}", latestSpringbootVersion());
        final String jkClassCode = code;
        if (projectBuildClassTemplate != JkProjectScaffold.BuildClassTemplate.CODE_LESS) {
            scaffold.setJekaClassCodeProvider(() -> jkClassCode);
        }
        scaffold.extraActions.append(this::scaffoldSample);
        String readmeContent = JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/README.md"));
        scaffold.extraActions.append(() -> {
            JkPathFile readmeFile = JkPathFile.of(project.getBaseDir().resolve("README.md")).createIfNotExist();
            readmeFile.write(readmeContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        });
        if (scaffoldBuildKind == ScaffoldBuildKind.KBEAN) {
            scaffold.addLocalPropsFileContent("springboot#springbootVersion=" + latestSpringbootVersion());
        }
    }

    /**
     * Returns the latest GA Spring-Boot version
     */
    private String latestSpringbootVersion() {
        try {
            List<String> springbootVersions = project.dependencyResolver
                    .searchVersions(JkSpringModules.Boot.STARTER_PARENT);
            return springbootVersions.stream()
                    .sorted(JkVersion.VERSION_COMPARATOR.reversed())
                    .findFirst().get();
        } catch (Exception e) {
            JkLog.warn(e.getMessage());
            JkLog.warn("Cannot find latest springboot version, choose default : " + JkSpringbootJars.DEFAULT_SPRINGBOOT_VERSION);
            return JkSpringbootJars.DEFAULT_SPRINGBOOT_VERSION;
        }
    }

}
