package dev.jeka.plugins.kotlin;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
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
import dev.jeka.core.api.utils.JkUtilsString;

import static dev.jeka.core.api.project.JkProjectCompilation.JAVA_SOURCES_COMPILE_ACTION;

public class JkKotlinJvm {

    public static final String KOTLIN_JVM_SOURCES_COMPILE_ACTION = "kotlin-jvm-sources-compile";

    private final JkKotlinCompiler kotlinCompiler;

    private boolean addStdlib = true;

    private String jvmVersion = JkJavaVersion.V8.toString();

    private JkKotlinJvm(JkKotlinCompiler kotlinCompiler) {
        this.kotlinCompiler = kotlinCompiler;
    }

    public static JkKotlinJvm of(JkKotlinCompiler kotlinCompiler) {
        return new JkKotlinJvm(kotlinCompiler);
    }

    public static JkKotlinJvm of() {
        return new JkKotlinJvm(null);
    }

    /**
     * Sets whether to add the standard Kotlin library to the classpath.
     */
    public JkKotlinJvm setAddStdlib(boolean addStdlib) {
        this.addStdlib = addStdlib;
        return this;
    }

    /**
     * Sets the JVM target version to be used for compiling Kotlin code.
     */
    public JkKotlinJvm setJvmVersion(String jvmVersion) {
        this.jvmVersion = jvmVersion;
        return this;
    }

    /**
     * Retrieves the Kotlin compiler associated with this JkKotlinJvm.
     */
    public JkKotlinCompiler getKotlinCompiler() {
        return kotlinCompiler;
    }

    /**
     * Configures the specified project for Kotlin compilation and testing.
     */
    public void configure(JkProject project, String kotlinSourceDir, String kotlinTestSourceDir) {
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
                        () -> compileKotlin(project, kotlinSourceDir));
        testCompile
                .preCompileActions
                    .replaceOrInsertBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                        () -> compileTestKotlin(project, kotlinTestSourceDir));

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

    private void compileKotlin(JkProject javaProject, String kotlinSourceDir) {
        JkProjectCompilation compilation = javaProject.compilation;
        JkPathTreeSet sources = compilation.layout.resolveSources();

        if (!JkUtilsString.isBlank(kotlinSourceDir)) {
            sources = sources .and(javaProject.getBaseDir().resolve(kotlinSourceDir));

        }
        if (sources.count(1, false) == 0) {
            JkLog.info("No source to compile in " + sources);
            return;
        }
        JkJavaVersion targetVersion = javaProject.getJvmTargetVersion();
        if (targetVersion == null) {
            targetVersion = JkJavaVersion.of(jvmVersion);
        }
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(compilation.resolveDependenciesAsFiles())
                .setOutputDir(compilation.layout.getOutputDir().resolve("classes"))
                .setTargetVersion(targetVersion)
                .setSources(sources);
        kotlinCompiler.compile(compileSpec);
    }

    private void compileTestKotlin(JkProject javaProject, String kotlinTestSourceDir) {
        JkProjectCompilation compilation = javaProject.testing.compilation;
        JkPathTreeSet sources = compilation.layout.resolveSources();
        if (JkUtilsString.isBlank(kotlinTestSourceDir)) {
            sources = sources.and(javaProject.getBaseDir().resolve(kotlinTestSourceDir));
        }
        if (sources.count(1, false) == 0) {
            JkLog.info("No source to compile in " + sources);
            return;
        }
        JkPathSequence classpath = JkPathSequence.of(compilation.resolveDependenciesAsFiles())
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
