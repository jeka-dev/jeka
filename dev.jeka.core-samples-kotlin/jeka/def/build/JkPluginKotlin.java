package build;

import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectCompilation;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.kotlin.JkKotlinModules;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

@JkDefClasspath("org.jetbrains.kotlin:kotlin-compiler:1.5.31")
public class JkPluginKotlin extends JkPlugin {

    public static final String KOTLIN_SOURCES_COMPILE_ACTION = "kotlin-sources-compile";

    public final JkPluginJava java;

    public final JkJavaProject getProject() {
        return java.getProject();
    }

    private JkKotlinCompiler kotlinCompiler;

    public boolean addStdlib = true;

    protected JkPluginKotlin(JkClass jkClass) {
        super(jkClass);
        java = getJkClass().getPlugin(JkPluginJava.class);

    }

    @Override
    protected void beforeSetup() throws Exception {
        super.beforeSetup();
    }

    @Override
    protected void afterSetup() throws Exception {
        kotlinCompiler = JkKotlinCompiler.ofJvm(getProject().getConstruction().getDependencyResolver().getRepos());
        String kotlinVersion = kotlinCompiler.getVersion();
        JkVersionProvider versionProvider = JkVersionProvider.of()
                .and(JkKotlinModules.STDLIB, kotlinVersion)
                .and(JkKotlinModules.STDLIB_JDK8, kotlinVersion)
                .and(JkKotlinModules.STDLIB_JDK7, kotlinVersion)
                .and(JkKotlinModules.STDLIB_COMMON, kotlinVersion)
                .and(JkKotlinModules.REFLECT, kotlinVersion)
                .and(JkKotlinModules.ANDROID_EXTENSION_RUNTIME, kotlinVersion)
                .and(JkKotlinModules.TEST, kotlinVersion)
                .and(JkKotlinModules.TEST_COMMON, kotlinVersion)
                .and(JkKotlinModules.TEST_JUNIT, kotlinVersion)
                .and(JkKotlinModules.TEST_JUNIT5, kotlinVersion);
        java.getProject().getConstruction().getCompilation().setDependencies(
                deps -> deps.andVersionProvider(versionProvider));
        java.getProject().getConstruction().setRuntimeDependencies(
                deps -> deps.andVersionProvider(versionProvider));
        java.getProject().getConstruction().getTesting().getCompilation().setDependencies(
                deps -> deps.andVersionProvider(versionProvider));
        java.getProject()
                .getConstruction()
                    .getCompilation()
                        .getCompileActions()
                            .appendBefore(KOTLIN_SOURCES_COMPILE_ACTION,
                                    JkJavaProjectCompilation.JAVA_SOURCES_COMPILE_ACTION,
                                    this::compileKotlin)
                        .__
                    .__
                    .getTesting()
                        .getCompilation()
                            .getPreCompileActions()
                                .append(this::compileTestKotlin);
        if (addStdlib) {
            if (kotlinCompiler.isProvidedCompiler()) {
                getProject().getConstruction().getCompilation().setDependencies(deps -> deps
                        .andFiles(kotlinCompiler.getStdLib())
                );
            } else {
                getProject().getConstruction().getCompilation().setDependencies(deps -> deps
                        .and(JkKotlinModules.STDLIB_JDK8)
                        .and(JkKotlinModules.REFLECT)
                );
            }
        }
    }

    public String kotlinVersion() {
        return kotlinCompiler.getVersion();
    }

    private void compileKotlin() {
        JkJavaProjectCompilation compilation = java.getProject().getConstruction().getCompilation();
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(compilation.resolveDependencies().getFiles())
                .setOutputDir(compilation.getLayout().getOutputDir().resolve("classes"))
                .setTargetVersion(java.getProject().getConstruction().getJavaVersion())
                .addSources(java.getProject().getBaseDir().resolve("src/main/java"));
        kotlinCompiler.compile(compileSpec);
    }

    private void compileTestKotlin() {
        JkJavaProjectCompilation compilation = java.getProject().getConstruction().getTesting().getCompilation();
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(compilation.resolveDependencies().getFiles()
                        .and(java.getProject().getConstruction().getCompilation().getLayout().getClassDirPath()))
                .setOutputDir(compilation.getLayout().getOutputDir().resolve("test-classes"))
                .setTargetVersion(java.getProject().getConstruction().getJavaVersion())
                .addSources(java.getProject().getBaseDir().resolve("src/test/java"));
        kotlinCompiler.compile(compileSpec);
    }

    public void generateFatJar() {
        this.getProject().getPublication().getArtifactProducer()
                .putArtifact(JkArtifactId.of("all-deps", "jar"),
                        path -> getProject().getConstruction().createFatJar(path));
    }

}
