package dev.jeka.plugins.springboot;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.j2e.JkJ2eWarProjectAdapter;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.scaffold.JkProjectScaffold;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkConstants;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;

/**
 * Class for adding Spring-Boot configuration to  {@link JkProject}s.
 */
public final class JkSpringbootProject {

    public enum SpringbootScaffoldTemplate {
        LIB,
        PROPS
    }

    public static final JkArtifactId ORIGINAL_ARTIFACT = JkArtifactId.of("original", "jar");

    // In some test scenario, we might want to inject a specific version of JeKA springboot plugin.
    // In this case we generally refers to a jar file on file system
    public static final String OVERRIDE_SCAFFOLDED_SPRINGBOOT_PLUGIN_DEPENDENCY_PROP_NAME
            = "jeka.springboot.plugin.dependency";

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
            project.setJarMaker(this::createBootJar);
            project.packActions.append("Make bootable jar", () -> createBootJar(artifactFile));
        }
        if (createWarFile) {
            JkArtifactId artifactId = JkArtifactId.ofMainArtifact("war");
            Path artifactFile = project.artifactLocator.getArtifactPath(artifactId);
            Consumer<Path> warMaker = path -> JkJ2eWarProjectAdapter.of().generateWar(project, path);
            project.packActions.append("Make war file", () -> warMaker.accept(artifactFile) );
        }
        if (createOriginalJar) {
            Path artifactFile = project.artifactLocator.getArtifactPath(ORIGINAL_ARTIFACT);
            Consumer<Path> makeBinJar = project.packaging::createBinJar;
            project.packActions.append("Make original jar", () -> makeBinJar.accept(artifactFile));
        }

        // To deploy spring-Boot app in a container, we don't need to create a jar
        // This is more efficient to keep the structure exploded to have efficient image layering.
        // In this case, just copy manifest in class dir is enough.
        if (!createBootJar && !createOriginalJar && !createWarFile) {
            project.packActions.append("Include manifest", project.packaging::copyManifestInClassDir);
        }

        project.packaging.setMainClass(JkProject.AUTO_FIND_MAIN_CLASS);
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
        JkLog.startTask("pack-bootable-jar");
        JkDependencyResolver dependencyResolver = project.dependencyResolver;
        final List<Path> embeddedJars = project.packaging.resolveRuntimeDependenciesAsFiles();
        JkSpringbootJars.createBootJar(classTree, embeddedJars, dependencyResolver.getRepos(), target,
                project.packaging.getManifest());
        JkLog.endTask();
    }

}
