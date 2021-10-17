package build;

import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectCompilation;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.kotlin.JkKotlinModules;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkOptions;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.java.project.JkJavaProjectCompilation.JAVA_SOURCES_COMPILE_ACTION;
import static dev.jeka.core.api.kotlin.JkKotlinModules.REFLECT;
import static dev.jeka.core.api.kotlin.JkKotlinModules.STDLIB_JDK8;

@JkDefClasspath("org.jetbrains.kotlin:kotlin-compiler:1.5.31")
public class JkPluginKotlin extends JkPlugin {

    public static final String KOTLIN_SOURCES_COMPILE_ACTION = "kotlin-sources-compile";

    // used for kotlin-JVM
    private JkJavaProject jvmProject;

    public boolean addStdlib = true;

    public String kotlinVersion;

    protected JkPluginKotlin(JkClass jkClass) {
        super(jkClass);
        kotlinVersion = JkOptions.get(JkKotlinCompiler.KOTLIN_VERSION_OPTION);
    }

    public final JkJavaProject jvmProject() {
        if (jvmProject == null) {
            jvmProject = setupKotlinJvm();
        }
        return jvmProject;
    }

    private JkJavaProject setupKotlinJvm() {
        JkPluginJava javaPlugin = this.getJkClass().getPlugin(JkPluginJava.class);
        JkJavaProject javaProject = javaPlugin.getProject();
        final JkKotlinCompiler kotlinCompiler;
        if (JkUtilsString.isBlank(kotlinVersion)) {
            kotlinCompiler = JkKotlinCompiler.ofKotlinHomeCommand("kotlinc");
            JkLog.warn("No version of kotlin has been specified, will use the version installed on KOTLIN_HOME : "
                    + kotlinCompiler.getVersion());
        } else {
            kotlinCompiler = JkKotlinCompiler.ofJvm(javaProject.getConstruction().getDependencyResolver().getRepos(),
                    kotlinVersion);
        }
        String effectiveVersion = kotlinCompiler.getVersion();
        JkJavaProjectCompilation<?> prodCompile = javaProject.getConstruction().getCompilation();
        JkJavaProjectCompilation<?> testCompile = javaProject.getConstruction().getTesting().getCompilation();
        prodCompile.getPreCompileActions().appendBefore(KOTLIN_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                () -> compileKotlin(kotlinCompiler, javaProject));
        testCompile.getPreCompileActions().appendBefore(KOTLIN_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                () -> compileTestKotlin(kotlinCompiler, javaProject));
        JkVersionProvider versionProvider = versionProvider(effectiveVersion);
        prodCompile.setDependencies(deps -> deps.andVersionProvider(versionProvider));
        if (addStdlib) {
            if (kotlinCompiler.isProvidedCompiler()) {
                prodCompile.setDependencies(deps -> deps.andFiles(kotlinCompiler.getStdLib()));
            } else {
                prodCompile.setDependencies(deps -> deps
                        .and(JkKotlinModules.STDLIB_JDK8)
                        .and(JkKotlinModules.REFLECT));
                testCompile.setDependencies(deps -> deps.and(JkKotlinModules.TEST));
            }
        }
        return javaProject;
    }

    private void compileKotlin(JkKotlinCompiler kotlinCompiler, JkJavaProject javaProject) {
        JkJavaProjectCompilation compilation = javaProject.getConstruction().getCompilation();
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(compilation.resolveDependencies().getFiles())
                .setOutputDir(compilation.getLayout().getOutputDir().resolve("classes"))
                .setTargetVersion(javaProject.getConstruction().getJvmTargetVersion())
                .addSources(javaProject.getBaseDir().resolve("src/main/java"));
        kotlinCompiler.compile(compileSpec);
    }

    private void compileTestKotlin(JkKotlinCompiler kotlinCompiler, JkJavaProject javaProject) {
        JkJavaProjectCompilation compilation = javaProject.getConstruction().getTesting().getCompilation();
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(compilation.resolveDependencies().getFiles()
                        .and(javaProject.getConstruction().getCompilation().getLayout().getClassDirPath()))
                .setOutputDir(compilation.getLayout().getOutputDir().resolve("test-classes"))
                .setTargetVersion(javaProject.getConstruction().getJvmTargetVersion())
                .addSources(javaProject.getBaseDir().resolve("src/test/java"));
        kotlinCompiler.compile(compileSpec);
    }

    public void generateFatJar() {
        this.jvmProject.getPublication().getArtifactProducer()
                .putArtifact(JkArtifactId.of("all-deps", "jar"),
                        path -> jvmProject.getConstruction().createFatJar(path));
    }

    private static JkVersionProvider versionProvider(String kotlinVersion) {
        JkUtilsAssert.argument(!JkUtilsString.isBlank(kotlinVersion), "kotlin version cannot be blank.");
        return JkVersionProvider.of()
                .and(JkKotlinModules.STDLIB, kotlinVersion)
                .and(STDLIB_JDK8, kotlinVersion)
                .and(JkKotlinModules.STDLIB_JDK7, kotlinVersion)
                .and(JkKotlinModules.STDLIB_COMMON, kotlinVersion)
                .and(REFLECT, kotlinVersion)
                .and(JkKotlinModules.ANDROID_EXTENSION_RUNTIME, kotlinVersion)
                .and(JkKotlinModules.TEST, kotlinVersion)
                .and(JkKotlinModules.TEST_COMMON, kotlinVersion)
                .and(JkKotlinModules.TEST_JUNIT, kotlinVersion)
                .and(JkKotlinModules.TEST_JUNIT5, kotlinVersion);
    }

}
