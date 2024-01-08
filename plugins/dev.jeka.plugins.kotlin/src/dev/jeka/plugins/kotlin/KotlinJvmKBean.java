package dev.jeka.plugins.kotlin;

import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.util.Optional;

/**
 * Provides options for configuring the compiler, setting the Kotlin version,
 * specifying the locations of Kotlin sources and test sources,
 * and including the standard library in the compilation process.
 */
@JkDoc("Explain here what your plugin is doing.\n" +
    "No need to list methods or options here has you are supposed to annotate them directly.")
public class KotlinJvmKBean extends KBean {

    private static final String DEFAULT_VERSION = "1.8.0";

    @JkDoc("The Kotlin version for compiling and running")
    private String kotlinVersion;

    @JkDoc("Location of Kotlin sources")
    private final String kotlinSourceDir = "src/main/kotlin";

    @JkDoc("Location of Kotlin sources for tests")
    private final String kotlinTestSourceDir = "src/test/kotlin";

    @JkDoc("Include standard lib in for compiling")
    private final boolean addStdlib = true;

    @JkDoc("If true, the project KBean will be automatically configured to use Kotlin.")
    private final boolean autoConfigureProject = false;

    private JkKotlinJvmProject kotlinJvmProject;

    @Override
    protected void init() {
        if (autoConfigureProject) {
            load(ProjectKBean.class);
            getKotlinJvmProject().configure();
        }
    }

    /**
     * Retrieves the Kotlin JVM project. If the project has already been created, it is returned.
     * Otherwise, it creates a new Kotlin JVM project using the projectKBean found in runtime..
     * If no projectKean is found in the runtime, an IllegalStateException is thrown.
     *
     * @throws IllegalStateException if no projectKean is found in the runtime
     */
    public JkKotlinJvmProject getKotlinJvmProject() {
        if (kotlinJvmProject != null) {
            return kotlinJvmProject;
        }
        ProjectKBean projectKBean = getRuntime().find(ProjectKBean.class).orElseThrow(() ->
                new IllegalStateException("No projectKean found in runtime. " +
                        "Use #createKotlinJvmProject(JkProject) instead."));
        kotlinJvmProject = createKotlinJvmProject(projectKBean.project);
        return kotlinJvmProject;
    }

    /**
     * Creates a {@link JkKotlinJvmProject} object with the configured options..
     */
    public JkKotlinJvmProject createKotlinJvmProject(JkProject project) {

        String jvmVersion = getRuntime().getProperties().get(JkConstants.JAVA_VERSION_PROP);
        kotlinJvmProject = JkKotlinJvmProject.of(project)
                .setKotlinCompiler(getRuntime().getDependencyResolver().getRepos(), getKotlinVersion())
                .setKotlinSourceDir(this.kotlinSourceDir)
                .setKotlinTestSourceDir(this.kotlinTestSourceDir)
                .setAddStdlib(this.addStdlib)
                .setJvmVersion(jvmVersion);
        return kotlinJvmProject;
    }

    private String getKotlinVersion() {
        String result = kotlinVersion;
        if (result == null) {
            result = Optional.ofNullable(getRuntime().getProperties()
                    .get(JkKotlinCompiler.KOTLIN_VERSION_OPTION)).orElse(DEFAULT_VERSION);
        }
        return result;
    }

}