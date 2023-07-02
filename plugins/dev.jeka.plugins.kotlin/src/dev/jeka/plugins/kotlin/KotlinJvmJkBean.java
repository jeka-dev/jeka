package dev.jeka.plugins.kotlin;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.kotlin.JkKotlinModules;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectCompilation;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A Jeka plugin cain contains zero, one or many KBeans.
 * If your plugin does not require one, fell free to delete this class.
 */
@JkDoc("Explain here what your plugin is doing.\n" +
    "No need to list methods or options here has you are supposed to annotate them directly.")
public class KotlinJvmJkBean extends JkBean {

    public static final String KOTLIN_JVM_SOURCES_COMPILE_ACTION = "kotlin-jvm-sources-compile";

    private static final String DEFAULT_VERSION = "1.8.0";

    @JkDoc("The Kotlin version for compiling and running")
    public String kotlinVersion;

    public String kotlinSourceDir = "src/main/kotlin";

    public String kotlinTestSourceDir = "src/test/kotlin";

    @JkDoc("Include standard lib in for compiling")
    public boolean addStdlib = true;

    @JkDoc("If true, the project KBean will be automatically configured to use Kotlin.")
    public boolean configureProject = true;

    private JkKotlinCompiler kotlinCompiler;

    private JkRepoSet downloadRepos;

    private final JkConsumers<JkKotlinCompiler> compilerConfigurators = JkConsumers.of();

    KotlinJvmJkBean() {
        kotlinVersion = Optional.of(getRuntime().getProperties().get("jeka.kotlin.version")).orElse(DEFAULT_VERSION);
        downloadRepos = JkRepoProperties.of(getRuntime().getProperties()).getDownloadRepos();
        Optional<ProjectJkBean> projectJkBean = getRuntime().getBeanOptional(ProjectJkBean.class);
        projectJkBean.ifPresent( bean -> automateConfigure(bean));
    }

    /**
     * Sets the download repositories to download Kotlin compilers and plugins
     */
    public KotlinJvmJkBean setDownloadRepos(JkRepoSet downloadRepos) {
        this.downloadRepos = downloadRepos;
        return this;
    }

    private void automateConfigure(ProjectJkBean projectJkBean) {
        if (!this.configureProject) {
            return;
        }
        projectJkBean.lately(this::configure);
    }

    public void configure(JkProject project) {
        if (!JkUtilsString.isBlank(kotlinSourceDir)) {
            project.compilation.layout.setSources(kotlinTestSourceDir);
        }
        if (!JkUtilsString.isBlank(kotlinTestSourceDir)) {
            project.testing.compilation.layout.setSources(kotlinTestSourceDir);
        }
        JkProjectCompilation prodCompile = project.compilation;
        JkProjectCompilation testCompile = project.testing.compilation;
        prodCompile
                .configureDependencies(deps -> deps.andVersionProvider(kotlinVersionProvider()))
                .preCompileActions
                    .appendBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JkProjectCompilation.JAVA_SOURCES_COMPILE_ACTION,
                            () -> compileKotlin(getCompiler(), project));
        testCompile
                .preCompileActions
                .appendBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JkProjectCompilation.JAVA_SOURCES_COMPILE_ACTION,
                        () -> compileTestKotlin(getCompiler(), project));
        testCompile
                .layout
                    .addSource(kotlinTestSourceDir);
        JkPathTree javaInKotlinDir = JkPathTree.of(project.getBaseDir().resolve(kotlinSourceDir));
        JkPathTree javaInKotlinTestDir = JkPathTree.of(project.getBaseDir().resolve(kotlinTestSourceDir));
        prodCompile.layout.setSources(javaInKotlinDir);
        testCompile.layout.setSources(javaInKotlinTestDir);
        if (addStdlib) {
            prodCompile.configureDependencies(this::addStdLibsToProdDeps);
            testCompile.configureDependencies(this::addStdLibsToTestDeps);
        }

        /*
        project.setJavaIdeSupport(ideSupport -> {
            ideSupport.getProdLayout().addSource(project.getBaseDir().resolve(kotlinSourceDir));
            if (kotlinTestSourceDir != null) {
                ideSupport.getTestLayout().addSource(project.getBaseDir().resolve(kotlinTestSourceDir));
            }
            return ideSupport;
        });

         */
    }

    /**
     * Register a configurator to be applied at the first call of {@link #getCompiler()}
     */
    public KotlinJvmJkBean configureCompiler(Consumer<JkKotlinCompiler> compilerConsumer) {
        if (this.kotlinCompiler != null) {
            throw new IllegalStateException("The compiler has already been instantiated. " +
                    "Use this method in the constructor of your KBean in order to configure the kompiler " +
                    "at creation time.");
        }
        this.compilerConfigurators.append(compilerConsumer);
        return this;
    }

    private JkDependencySet addStdLibsToProdDeps(JkDependencySet deps) {
        return getCompiler().isProvidedCompiler()
                ? deps.andFiles(getCompiler().getStdLib())
                : deps.and(JkKotlinModules.STDLIB_JDK8).and(JkKotlinModules.REFLECT);
    }

    private JkDependencySet addStdLibsToTestDeps(JkDependencySet deps) {
        return getCompiler().isProvidedCompiler() ? deps.and(JkKotlinModules.TEST) : deps;
    }

    private JkVersionProvider kotlinVersionProvider() {
        return JkKotlinModules.versionProvider(getCompiler().getVersion());
    }


    public JkKotlinCompiler getCompiler() {
        if (kotlinCompiler != null) {
            return kotlinCompiler;
        }
        if (JkUtilsString.isBlank(kotlinVersion)) {
            kotlinCompiler = JkKotlinCompiler.ofKotlinHomeCommand("kotlinc");
            JkLog.warn("No version of kotlin has been specified, will use the version installed on KOTLIN_HOME : "
                    + kotlinCompiler.getVersion());
        } else {
            kotlinCompiler = JkKotlinCompiler.ofJvm(downloadRepos, kotlinVersion);
        }
        kotlinCompiler.setLogOutput(true);
        compilerConfigurators.accept(kotlinCompiler);
        return kotlinCompiler;
    }

    private void compileKotlin(JkKotlinCompiler kotlinCompiler, JkProject javaProject) {
        JkProjectCompilation compilation = javaProject.compilation;
        JkPathTreeSet sources = compilation.layout.resolveSources()
                .and(javaProject.getBaseDir().resolve(kotlinSourceDir));
        if (sources.count(1, false) == 0) {
            JkLog.info("No source to compile in " + sources);
            return;
        }
        JkJavaVersion targetVersion = javaProject.getJvmTargetVersion();
        if (targetVersion == null) {
            targetVersion = JkJavaVersion.of(
                    getRuntime().getProperties().get("jeka.java.version"));
        }
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(compilation.resolveDependencies().getFiles())
                .setOutputDir(compilation.layout.getOutputDir().resolve("classes"))
                .setTargetVersion(targetVersion)
                .setSources(sources);
        kotlinCompiler.compile(compileSpec);
    }

    private void compileTestKotlin(JkKotlinCompiler kotlinCompiler, JkProject javaProject) {
        JkProjectCompilation compilation = javaProject.testing.compilation;
        JkPathTreeSet sources = compilation.layout.resolveSources();
        if (kotlinTestSourceDir == null) {
            sources = sources.and(javaProject.getBaseDir().resolve(kotlinTestSourceDir));
        }
        if (sources.count(1, false) == 0) {
            JkLog.info("No source to compile in " + sources);
            return;
        }
        JkPathSequence classpath = compilation.resolveDependencies().getFiles()
                .and(compilation.layout.getClassDirPath());
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setSources(compilation.layout.resolveSources())
                .setClasspath(classpath)
                .setOutputDir(compilation.layout.getOutputDir().resolve("test-classes"))
                .setTargetVersion(javaProject.getJvmTargetVersion());
        kotlinCompiler.compile(compileSpec);
    }

}