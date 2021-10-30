package build.common;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectCompilation;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.kotlin.JkKotlinModules;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.repos.JkPluginRepo;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static dev.jeka.core.api.java.project.JkJavaProjectCompilation.JAVA_SOURCES_COMPILE_ACTION;

@JkDefClasspath("org.jetbrains.kotlin:kotlin-compiler:1.5.31")
public class JkPluginKotlin extends JkPlugin {

    public static final String KOTLIN_JVM_SOURCES_COMPILE_ACTION = "kotlin-jvm-sources-compile";

    // used for kotlin-JVM
    private JkKotlinJvmProject jvm;

    private JKCommon common;

    public boolean addStdlib = true;

    public String kotlinVersion;

    protected JkPluginKotlin(JkClass jkClass) {
        super(jkClass);
        kotlinVersion = JkOptions.get(JkKotlinCompiler.KOTLIN_VERSION_OPTION);
    }

    public final JkKotlinJvmProject jvm() {
        if (jvm == null) {
            jvm = new JkKotlinJvmProject();
        }
        return jvm;
    }

    public final JKCommon common() {
        if (common == null) {
            common = new JKCommon();
        }
        return common;
    }

    @JkDoc("Displays loaded compiler plugins and options")
    public void showPluginOptions() {
        if (jvm != null) {
            JkLog.info("Kotlin-JVM compiler options :");
            List<String> options = new LinkedList<>(jvm.kotlinCompiler.getPlugins());
            options.addAll(jvm.kotlinCompiler.getPluginOptions());
            JkLog.info(String.join(" ", options));
        }
    }

    public JkArtifactId addFatJar(String classifier) {
        JkArtifactId artifactId = JkArtifactId.of(classifier, "jar");
        this.jvm.project.getPublication().getArtifactProducer()
                .putArtifact(artifactId,
                        path -> jvm.project.getConstruction().createFatJar(path));
        return artifactId;
    }

    @Override
    protected void afterSetup() throws Exception {
        if (common != null) {
            common.setupJvmProject(jvm());
        }
    }

    public class JkKotlinJvmProject {

        private JkJavaProject project;

        private JkKotlinCompiler kotlinCompiler;

        private String kotlinSourceDir = "src/main/kotlin-jvm";

        private String kotlinTestSourceDir = "src/test/kotlin-jvm";

        private JkKotlinJvmProject() {
        }

        public JkJavaProject getProject() {
            return project;
        }

        public JkKotlinCompiler getKotlinCompiler() {
            if (kotlinCompiler != null) {
                return kotlinCompiler;
            }
            if (JkUtilsString.isBlank(kotlinVersion)) {
                kotlinCompiler = JkKotlinCompiler.ofKotlinHomeCommand("kotlinc");
                JkLog.warn("No version of kotlin has been specified, will use the version installed on KOTLIN_HOME : "
                        + kotlinCompiler.getVersion());
            } else {
                JkPluginRepo pluginRepo = JkPluginKotlin.this.getJkClass().getPlugin(JkPluginRepo.class);
                kotlinCompiler = JkKotlinCompiler.ofJvm(pluginRepo.downloadRepository().toSet(), kotlinVersion);
            }
            kotlinCompiler.setLogOutput(true);
            return kotlinCompiler;
        }

        public JkKotlinJvmProject useFatJarForMainArtifact() {
            project.getPublication().getArtifactProducer()
                    .putArtifact(JkArtifactId.ofMainArtifact("jar"),
                            path -> project.getConstruction().createFatJar(path));
            return this;
        }

        private void createJavaProject(JkJavaProject project) {
            JkKotlinCompiler kompiler = getKotlinCompiler();
            String effectiveVersion = kompiler.getVersion();
            JkJavaProjectCompilation<?> prodCompile = project.getConstruction().getCompilation();
            JkJavaProjectCompilation<?> testCompile = project.getConstruction().getTesting().getCompilation();
            JkVersionProvider versionProvider = JkKotlinModules.versionProvider(effectiveVersion);
            prodCompile
                    .getPreCompileActions()
                    .appendBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                            () -> compileKotlin(kompiler, project))
                    .__
                    .setDependencies(deps -> deps.andVersionProvider(versionProvider));
            testCompile
                    .getPreCompileActions()
                    .appendBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                            () -> compileTestKotlin(kompiler, project))
                    .__
                    .getLayout()
                        .addSource(jvm.kotlinTestSourceDir);
            JkPathTree javaInKotlinDir = JkPathTree.of(project.getBaseDir().resolve(kotlinSourceDir));
            JkPathTree javaInKotlinTestDir = JkPathTree.of(project.getBaseDir().resolve(kotlinTestSourceDir));
            prodCompile.getLayout().setSources(javaInKotlinDir);
            testCompile.getLayout().setSources(javaInKotlinTestDir);
            if (addStdlib) {
                if (kompiler.isProvidedCompiler()) {
                    prodCompile.setDependencies(deps -> deps.andFiles(kompiler.getStdLib()));
                } else {
                    prodCompile.setDependencies(deps -> deps
                            .and(JkKotlinModules.STDLIB_JDK8)
                            .and(JkKotlinModules.REFLECT));
                    testCompile.setDependencies(deps -> deps.and(JkKotlinModules.TEST));
                }
            }
            project.setJavaIdeSupport(ideSupport -> {
                ideSupport.getProdLayout().addSource(project.getBaseDir().resolve(kotlinSourceDir));
                if (kotlinTestSourceDir != null) {
                    ideSupport.getTestLayout().addSource(project.getBaseDir().resolve(kotlinTestSourceDir));
                }
                return ideSupport;
            });
        }

        private void compileTestKotlin(JkKotlinCompiler kotlinCompiler, JkJavaProject javaProject) {
            JkJavaProjectCompilation compilation = javaProject.getConstruction().getTesting().getCompilation();
            JkPathTreeSet sources = compilation.getLayout().resolveSources();
            if (kotlinTestSourceDir == null) {
                sources = sources.and(javaProject.getBaseDir().resolve(kotlinTestSourceDir));
            }
            if (common != null && !JkUtilsString.isBlank(common.testSrcDir)) {
                sources = sources.and(javaProject.getBaseDir().resolve(common.testSrcDir));
            }
            if (sources.count(1, false) == 0) {
                JkLog.info("No source to compile in " + sources);
                return;
            }
            JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                    .setClasspath(compilation.resolveDependencies().getFiles()
                            .and(compilation.getLayout().getClassDirPath()))
                    .setOutputDir(compilation.getLayout().getOutputDir().resolve("test-classes"))
                    .setTargetVersion(javaProject.getConstruction().getJvmTargetVersion())
                    .setSources(compilation.getLayout().resolveSources());
            kotlinCompiler.compile(compileSpec);
        }

        private void compileKotlin(JkKotlinCompiler kotlinCompiler, JkJavaProject javaProject) {
            JkJavaProjectCompilation compilation = javaProject.getConstruction().getCompilation();
            JkPathTreeSet sources = compilation.getLayout().resolveSources()
                    .and(javaProject.getBaseDir().resolve(kotlinSourceDir));
            if (common != null && !JkUtilsString.isBlank(common.srcDir)) {
                sources = sources.and(javaProject.getBaseDir().resolve(common.srcDir));
            }
            if (sources.count(1, false) == 0) {
                JkLog.info("No source to compile in " + sources);
                return;
            }
            JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                    .setClasspath(compilation.resolveDependencies().getFiles())
                    .setOutputDir(compilation.getLayout().getOutputDir().resolve("classes"))
                    .setTargetVersion(javaProject.getConstruction().getJvmTargetVersion())
                    .setSources(sources);
            kotlinCompiler.compile(compileSpec);
        }
    }


    public static class JKCommon {

        private String srcDir = "src/main/kotlin-common";

        private String testSrcDir = "src/test/kotlin-common";

        private JkPathTree resources = JkPathTree.of(Paths.get("src/main/resources-common"));

        private JkDependencySet compileDependencies = JkDependencySet.of();

        private JkDependencySet testDependencies = JkDependencySet.of();

        private boolean addCommonStdLibs = true;

        private JKCommon() {}

        private void setupJvmProject(JkKotlinJvmProject jvm) {
            JkJavaProjectCompilation<?> prodCompile = jvm.project.getConstruction().getCompilation();
            JkJavaProjectCompilation<?> testCompile = jvm.project.getConstruction().getTesting().getCompilation();
            if (testSrcDir != null) {
                testCompile.getLayout().addSource(testSrcDir);
                if (addCommonStdLibs) {
                    testCompile.setDependencies(deps -> deps
                            .and(JkKotlinModules.TEST_COMMON)
                            .and(JkKotlinModules.TEST_ANNOTATIONS_COMMON)
                    );
                }
            }
            prodCompile.setDependencies(deps -> deps.and(compileDependencies));
            testCompile.setDependencies(deps -> deps.and(testDependencies));
            jvm.project.setJavaIdeSupport(ideSupport -> {
                ideSupport.getProdLayout().addSource(srcDir);
                if (!JkUtilsString.isBlank(testSrcDir)) {
                    ideSupport.getTestLayout().addSource(testSrcDir);
                }
                return ideSupport;
            });
        }

        public String getSrcDir() {
            return srcDir;
        }

        public JKCommon setSrcDir(String srcDir) {
            this.srcDir = srcDir;
            return this;
        }

        public String getTestSrcDir() {
            return testSrcDir;
        }

        public JKCommon setTestSrcDir(String testDir) {
            this.testSrcDir = testDir;
            return this;
        }

        public JkDependencySet getCompileDependencies() {
            return compileDependencies;
        }

        public JKCommon setCompileDependencies(JkDependencySet compileDependencies) {
            this.compileDependencies = compileDependencies;
            return this;
        }

        public JkDependencySet getTestDependencies() {
            return testDependencies;
        }

        public JKCommon setTestDependencies(JkDependencySet testDependencies) {
            this.testDependencies = testDependencies;
            return this;
        }

        public boolean isAddCommonStdLibs() {
            return addCommonStdLibs;
        }

        public JKCommon setAddCommonStdLibs(boolean addCommonStdLibs) {
            this.addCommonStdLibs = addCommonStdLibs;
            return this;
        }
    }

}
