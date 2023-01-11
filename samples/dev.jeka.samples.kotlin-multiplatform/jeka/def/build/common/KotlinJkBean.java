package build.common;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.kotlin.JkKotlinModules;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectCompilation;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectClasspath;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static dev.jeka.core.api.project.JkProjectCompilation.JAVA_SOURCES_COMPILE_ACTION;

@JkInjectClasspath("org.jetbrains.kotlin:kotlin-compiler:1.5.31")
public class KotlinJkBean extends JkBean {

    public static final String KOTLIN_JVM_SOURCES_COMPILE_ACTION = "kotlin-jvm-sources-compile";

    // used for kotlin-JVM
    private JkKotlinJvmProject jvm;

    private JKCommon common;

    public boolean addStdlib = true;

    public String kotlinVersion;

    protected KotlinJkBean() {
        kotlinVersion = getRuntime().getProperties().get(JkKotlinCompiler.KOTLIN_VERSION_OPTION);
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
        this.jvm.getProject().artifactProducer
                .putArtifact(artifactId,
                        path -> jvm.getProject().packaging.createFatJar(path));
        return artifactId;
    }


    protected void postInit() throws Exception {
        if (common != null) {
            common.setupJvmProject(jvm());
        }
    }

    public class JkKotlinJvmProject {

        private JkProject cachedProject;

        private JkKotlinCompiler kotlinCompiler;

        private String kotlinSourceDir = "src/main/kotlin-jvm";

        private String kotlinTestSourceDir = "src/test/kotlin-jvm";

        public JkConsumers<JkProject> configurators = JkConsumers.of();

        private JkKotlinJvmProject() {
        }

        public JkProject getProject() {
            if (cachedProject == null) {
                cachedProject = createJavaProject();
            }
            return cachedProject;
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
                kotlinCompiler = JkKotlinCompiler.ofJvm(JkRepoProperties.of(getRuntime().getProperties()).getDownloadRepos(), kotlinVersion);
            }
            kotlinCompiler.setLogOutput(true);
            return kotlinCompiler;
        }

        public JkKotlinJvmProject useFatJarForMainArtifact() {
            getProject().artifactProducer
                    .putArtifact(JkArtifactId.ofMainArtifact("jar"),
                            path -> getProject().packaging.createFatJar(path));
            return this;
        }

        private JkProject createJavaProject() {
            JkProject project = JkProject.of().setBaseDir(KotlinJkBean.this.getBaseDir());
            JkKotlinCompiler kompiler = getKotlinCompiler();
            String effectiveVersion = kompiler.getVersion();
            JkProjectCompilation prodCompile = project.prodCompilation;
            JkProjectCompilation testCompile = project.testing.testCompilation;
            JkVersionProvider versionProvider = JkKotlinModules.versionProvider(effectiveVersion);
            prodCompile
                    .configureDependencies(deps -> deps.andVersionProvider(versionProvider))
                    .preCompileActions
                        .appendBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                                () -> compileKotlin(kompiler, project));
            testCompile
                    .preCompileActions
                        .appendBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                                () -> compileTestKotlin(kompiler, project));
            testCompile
                    .layout
                        .addSource(jvm.kotlinTestSourceDir);
            JkPathTree javaInKotlinDir = JkPathTree.of(project.getBaseDir().resolve(kotlinSourceDir));
            JkPathTree javaInKotlinTestDir = JkPathTree.of(project.getBaseDir().resolve(kotlinTestSourceDir));
            prodCompile.layout.setSources(javaInKotlinDir);
            testCompile.layout.setSources(javaInKotlinTestDir);
            if (addStdlib) {
                if (kompiler.isProvidedCompiler()) {
                    prodCompile.configureDependencies(deps -> deps.andFiles(kompiler.getStdLib()));
                } else {
                    prodCompile.configureDependencies(deps -> deps
                            .and(JkKotlinModules.STDLIB_JDK8)
                            .and(JkKotlinModules.REFLECT));
                    testCompile.configureDependencies(deps -> deps.and(JkKotlinModules.TEST));
                }
            }
            project.setJavaIdeSupport(ideSupport -> {
                ideSupport.getProdLayout().addSource(project.getBaseDir().resolve(kotlinSourceDir));
                if (kotlinTestSourceDir != null) {
                    ideSupport.getTestLayout().addSource(project.getBaseDir().resolve(kotlinTestSourceDir));
                }
                return ideSupport;
            });
            configurators.accept(project);
            return project;
        }

        private void compileTestKotlin(JkKotlinCompiler kotlinCompiler, JkProject javaProject) {
            JkProjectCompilation compilation = javaProject.testing.testCompilation;
            JkPathTreeSet sources = compilation.layout.resolveSources();
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
                            .and(compilation.layout.getClassDirPath()))
                    .setOutputDir(compilation.layout.getOutputDir().resolve("test-classes"))
                    .setTargetVersion(javaProject.getJvmTargetVersion())
                    .setSources(compilation.layout.resolveSources());
            kotlinCompiler.compile(compileSpec);
        }

        private void compileKotlin(JkKotlinCompiler kotlinCompiler, JkProject javaProject) {
            JkProjectCompilation compilation = javaProject.prodCompilation;
            JkPathTreeSet sources = compilation.layout.resolveSources()
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
                    .setOutputDir(compilation.layout.getOutputDir().resolve("classes"))
                    .setTargetVersion(javaProject.getJvmTargetVersion())
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
            JkProjectCompilation prodCompile = jvm.getProject().prodCompilation;
            JkProjectCompilation testCompile = jvm.getProject().testing.testCompilation;
            if (testSrcDir != null) {
                testCompile.layout.addSource(testSrcDir);
                if (addCommonStdLibs) {
                    testCompile.configureDependencies(deps -> deps
                            .and(JkKotlinModules.TEST_COMMON)
                            .and(JkKotlinModules.TEST_ANNOTATIONS_COMMON)
                    );
                }
            }
            prodCompile.configureDependencies(deps -> deps.and(compileDependencies));
            testCompile.configureDependencies(deps -> deps.and(testDependencies));
            jvm.getProject().setJavaIdeSupport(ideSupport -> {
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
