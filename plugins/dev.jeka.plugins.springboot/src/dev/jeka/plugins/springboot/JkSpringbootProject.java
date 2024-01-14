package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPackaging;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkConstants;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Class for adding Spring-Boot configuration to  {@link JkProject}s.
 */
public final class JkSpringbootProject {

    public enum ScaffoldBuildKind {
        LIB,
        PROPS
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

        project.packActions.set();

        // define bootable jar as main artifact
        if (createBootJar) {
            JkArtifactId artifactId = JkArtifactId.MAIN_JAR_ARTIFACT_ID;
            Path artifactFile = project.artifactLocator.getArtifactPath(artifactId);
            project.packActions.append("Make bootable jar", () -> createBootJar(artifactFile));
            project.mavenPublication.putArtifact(artifactId, this::createBootJar);
        }
        if (createWarFile) {
            JkArtifactId artifactId = JkArtifactId.ofMainArtifact("war");
            Path artifactFile = project.artifactLocator.getArtifactPath(artifactId);
            Consumer<Path> warMaker = path -> JkJ2eWarProjectAdapter.of().generateWar(project, path);
            project.packActions.append("Make war file", () -> warMaker.accept(artifactFile) );
            project.mavenPublication.putArtifact(artifactId, warMaker);
        }
        if (createOriginalJar) {
            JkArtifactId artifactId = ORIGINAL_ARTIFACT;
            Path artifactFile = project.artifactLocator.getArtifactPath(artifactId);
            Consumer<Path> makeBinJar = project.packaging::createBinJar;
            project.packActions.append("Make original jar", () -> makeBinJar.accept(artifactFile));
            project.mavenPublication.putArtifact(artifactId, makeBinJar);
        }

        // To deploy spring-Boot app in a container, we don't need to create a jar
        // This is more efficient to keep the structure exploded to have efficient image layering.
        // In this case, just copy manifest in class dir is enough.
        if (!createBootJar && !createOriginalJar && !createWarFile) {
            project.packActions.append("Include manifest", project.packaging::copyManifestInClassDir);
        }

        project.packaging.setMainClass(JkProjectPackaging.AUTO_FIND_MAIN_CLASS);
        project.packaging.setMainClassFinder(() -> {
            try {
                return JkSpringbootJars.findMainClassName(project.compilation.layout.resolveClassDir());
            } catch (IllegalStateException e) {
                JkLog.info("No Springboot application class found in class dir. Force recompile and re-search.");
                project.compilation.runIfNeeded();
                return JkSpringbootJars.findMainClassName(project.compilation.layout.resolveClassDir());
            }

        });

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
        project.compilation.customizeDependencies(deps -> deps
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
        Path target = project.artifactLocator.getMainArtifactPath();
        createBootJar(target);
        return target;
    }

    /**
     * Creates the bootable jar at the specified location.
     */
    public void createBootJar(Path target) {
        JkPathTree classTree = JkPathTree.of(project.compilation.layout.resolveClassDir());
        if (!classTree.exists()) {
            project.compilation.runIfNeeded();
            project.testing.runIfNeeded();
        }
        JkLog.startTask("Packaging bootable jar");
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        final List<Path> embeddedJars = project.packaging.resolveRuntimeDependenciesAsFiles();
        JkSpringbootJars.createBootJar(classTree, embeddedJars, dependencyResolver.getRepos(), target,
                project.packaging.getManifest());
        JkLog.endTask();
    }

    /**
     * Scaffolds a sample Spring-Boot application at sources and test location.
     */
    public void scaffoldSample() {
        String basePackage = "app";  // Applications are not consumed as dependency, therefore we do not need to use a unique identified packages.
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

        // Generate Build class
        if (scaffoldBuildKind == ScaffoldBuildKind.LIB) {
            String buildClassSource = "BuildLib.java";
            String code = JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/" + buildClassSource));
            String defClasspath = scaffoldDefClasspath != null ? scaffoldDefClasspath.replace("\\", "/") : "dev.jeka:springboot-plugin";
            code = code.replace("${dependencyDescription}", defClasspath);
            code = code.replace("${springbootVersion}", latestSpringbootVersion());
            final String jkClassCode = code;
            if (projectBuildClassTemplate != JkProjectScaffold.BuildClassTemplate.CODE_LESS) {
                scaffold.setJekaClassCodeProvider(() -> jkClassCode);
            }
        } else if (scaffoldBuildKind == ScaffoldBuildKind.PROPS) {
            String propsContent =
                    JkConstants.CLASSPATH_INJECT_PROP + "=" + "dev.jeka:springboot-plugin\n" +
                    JkConstants.DEFAULT_KBEAN_PROP + "=" + SpringbootKBean.class.getName() + "\n\n" +
                    "springboot#springbootVersion=" + latestSpringbootVersion();
            scaffold.addLocalPropsFileContent(propsContent);
            JkProjectScaffold projectScaffold = JkProjectScaffold.of(project, scaffold);
            projectScaffold.createProjectDependenciesTxt(
                    Collections.singletonList("org.springframework.boot:spring-boot-starter-web"),
                    Collections.emptyList(),
                    Collections.singletonList("org.springframework.boot:spring-boot-starter-test"));
        }

        // Scaffold application sample
        scaffold.extraActions.append(this::scaffoldSample);
        String readmeContent = JkUtilsIO.read(SpringbootKBean.class.getClassLoader().getResource("snippet/README.md"));
        scaffold.extraActions.append(() -> {
            JkPathFile readmeFile = JkPathFile.of(project.getBaseDir().resolve("README.md")).createIfNotExist();
            readmeFile.write(readmeContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        });

    }

    /*
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
