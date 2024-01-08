package dev.jeka.plugins.kotlin;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.kotlin.JkKotlinModules;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectCompilation;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import static dev.jeka.core.api.kotlin.JkKotlinCompiler.KOTLIN_COMPILER_COORDINATES;
import static dev.jeka.core.api.project.JkProjectCompilation.JAVA_SOURCES_COMPILE_ACTION;

public class JkKotlinJvmProject {

    public static final String KOTLIN_JVM_SOURCES_COMPILE_ACTION = "kotlin-jvm-sources-compile";

    private final JkProject project;

    private JkKotlinCompiler kotlinCompiler;

    private String kotlinSourceDir = "src/main/kotlin";

    private String kotlinTestSourceDir = "src/test/kotlin";

    private boolean addStdlib = true;

    private String jvmVersion;

    private JkKotlinJvmProject(JkProject project) {
        this.project = project;
    }

    /**
     * Creates a new JkKotlinJvmProject instance based on the provided JkProject.
     */
    public static JkKotlinJvmProject of(JkProject project) {
        return new JkKotlinJvmProject(project);
    }

    /**
     * Sets the Kotlin compiler for this JkKotlinJvmProject.
     */
    public JkKotlinJvmProject setKotlinCompiler(JkKotlinCompiler kotlinCompiler) {
        this.kotlinCompiler = kotlinCompiler;
        return this;
    }

    /**
     * Sets the Kotlin compiler for this JkKotlinJvmProject by specifying the repository where
     * to fetch the compiler and the compiler version.
     */
    public JkKotlinJvmProject setKotlinCompiler(
            JkRepoSet repos,
            @JkDepSuggest(versionOnly = true, hint = KOTLIN_COMPILER_COORDINATES) String kotlinVersion) {

        return setKotlinCompiler(JkKotlinCompiler.ofJvm(repos, kotlinVersion));
    }

    /**
     * Sets the Kotlin compiler version for this JkKotlinJvmProject using the specified Kotlin version.
     * The compiler will be fetched from Maven central.
     */
    public JkKotlinJvmProject setKotlinCompiler(
            @JkDepSuggest(versionOnly = true, hint = KOTLIN_COMPILER_COORDINATES) String kotlinVersion) {
        return setKotlinCompiler(JkRepo.ofMavenCentral().toSet(), kotlinVersion);
    }

    /**
     * Sets the directory, relative to project base dir, where Kotlin source files are located.
     */
    public JkKotlinJvmProject setKotlinSourceDir(String kotlinSourceDir) {
        this.kotlinSourceDir = kotlinSourceDir;
        return this;
    }

    /**
     * Sets the directory, relative to project base dir, where Kotlin test source files are located.
     */
    public JkKotlinJvmProject setKotlinTestSourceDir(String kotlinTestSourceDir) {
        this.kotlinTestSourceDir = kotlinTestSourceDir;
        return this;
    }

    /**
     * Sets whether to add the standard Kotlin library to the classpath.
     */
    public JkKotlinJvmProject setAddStdlib(boolean addStdlib) {
        this.addStdlib = addStdlib;
        return this;
    }

    /**
     * Sets the JVM target version to be used for compiling Kotlin code.
     */
    public JkKotlinJvmProject setJvmVersion(String jvmVersion) {
        this.jvmVersion = jvmVersion;
        return this;
    }

    /**
     * Retrieves the Kotlin compiler associated with this JkKotlinJvmProject.
     */
    public JkKotlinCompiler getKotlinCompiler() {
        return kotlinCompiler;
    }

    /**
     * Configures the specified project for Kotlin compilation and testing.
     */
    public void configure() {
        JkUtilsAssert.state(kotlinCompiler != null, "No kotlin compiler has been specified on this JkKotlinJvmProject.");
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
                    .replaceOrInsertBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                        () -> compileKotlin(kotlinCompiler, project));
        testCompile
                .preCompileActions
                    .replaceOrInsertBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                        () -> compileTestKotlin(kotlinCompiler, project));

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

    private JkVersionProvider kotlinVersionProvider() {
        return JkKotlinModules.versionProvider(kotlinCompiler.getVersion());
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
            targetVersion = JkJavaVersion.of(jvmVersion);
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

    private JkDependencySet addStdLibsToProdDeps(JkDependencySet deps) {
        return kotlinCompiler.isProvidedCompiler()
                ? deps.andFiles(kotlinCompiler.getStdLib())
                : deps.and(JkKotlinModules.STDLIB_JDK8).and(JkKotlinModules.REFLECT);
    }

    private JkDependencySet addStdLibsToTestDeps(JkDependencySet deps) {
        return kotlinCompiler.isProvidedCompiler() ? deps.and(JkKotlinModules.TEST) : deps;
    }

}
