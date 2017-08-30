package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkTestSuiteResult;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.project.*;
import org.jerkar.api.project.JkArtifactFileId.JkArtifactFileIds;
import org.jerkar.api.utils.JkFileFilters;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


@Deprecated // Experimental !!!!
public class JkJarProject implements JkJavaProjectDefinition, JkArtifactProducer {

    public static final JkPathFilter RESOURCE_FILTER = JkPathFilter.exclude("**/*.java")
            .andExclude("**/package.html").andExclude("**/doc-files");

    public static final JkArtifactFileId FAT_JAR_FILE_ID = JkArtifactFileId.of("fat", "jar");

    public static final JkArtifactFileId SOURCES_FILE_ID = JkArtifactFileId.of("sources", "jar");

    public static final JkArtifactFileId JAVADOC_FILE_ID = JkArtifactFileId.of("javadoc", "jar");

    public static final JkArtifactFileId TEST_FILE_ID = JkArtifactFileId.of("test", "jar");

    public static final JkArtifactFileId TEST_SOURCE_FILE_ID = JkArtifactFileId.of("test-sources", "jar");

    private final File baseDir;

    private String artifactName;

    private JkProjectSourceLayout sourceLayout;

    private JkProjectOutLayout outLayout;

    private JkDependencies dependencies;

    private JkJavaCompileVersion compileVersion;

    private List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<JkResourceProcessor.JkInterpolator>();

    private JkManifest manifest = JkManifest.empty();

    private JkJavaProjectMakeContext makeContext = JkJavaProjectMakeContext.of();

    private String encoding = "UTF-8";

    private JkFileTreeSet extraFilesToIncludeInFatJar = JkFileTreeSet.empty();

    private boolean commonBuildDone;

    public JkJarProject(File baseDir) {
        this.baseDir = baseDir;
        this.artifactName = this.baseDir.getName();
        this.sourceLayout = JkProjectSourceLayout.mavenJava().withBaseDir(baseDir);
        this.outLayout = JkProjectOutLayout.classicJava().withOutputBaseDir(new File(baseDir, "build/output"));
        this.dependencies = JkDependencies.ofLocalScoped(new File(baseDir, "build/libs"));
    }

    public void clean() {
        commonBuildDone = false;
        makeContext.getCleaner().accept(this);
    }

    public void generateSourcesAndResources() {
        makeContext.getSourceGenerator().accept(this);
        makeContext.getResourceGenerator().accept(this);
    }

    public void compile() {
        JkJavaCompiler comp = applyEncodingAndVersion();
        comp = applyCompileSource(comp, this.makeContext.getDependencyResolver());
        comp = makeContext.getConfiguredProductionCompiler().apply(comp);
        comp.compile();
        makeContext.getPostCompiler().accept(this);
    }

    public void processResources() {
        makeContext.getResourceProcessor().accept(this);
    }

    public void generateTestResources() {
        makeContext.getTestResourceGenerator().accept(this);
    }

    public void compileTest() {
        JkJavaCompiler comp = applyEncodingAndVersion();
        comp = applyCompileTest();
        comp = makeContext.getConfiguredTestCompiler().apply(comp);
        comp.compile();
        makeContext.getTestPostCompiler().accept(this);
    }

    public void processTestResources() {
        makeContext.getTestResourceProcessor().accept(this);
    }

    public JkTestSuiteResult executeTests() {
        JkUnit juniter = juniter();
        juniter = makeContext.getConfiguredJuniter().apply(juniter);
        JkTestSuiteResult result = juniter.run();
        makeContext.getPostTestRunner().accept(this);
        return result;
    }

    public File packJar() {
        return jarMaker().mainJar(manifest, outLayout.classDir(), JkFileTreeSet.empty());
    }

    public File packFatJar(String suffix) {
        JkDependencyResolver depResolver = this.makeContext.getDependencyResolver();
        JkClasspath classpath = JkClasspath.of(depResolver.get(this.dependencies, JkJavaDepScopes.RUNTIME));
        return jarMaker().fatJar(manifest, suffix, this.outLayout.classDir(), classpath, this.extraFilesToIncludeInFatJar);
    }

    public File packTestJar() {
        return jarMaker().testJar(outLayout.testClassDir());
    }

    public File generateJavadoc() {
        JkJavadocMaker maker = JkJavadocMaker.of(this.sourceLayout.sources(), this.outLayout.getJavadocDir())
                .withDoclet(this.makeContext.getJavadocDoclet());
        maker.process();
        return this.outLayout.getJavadocDir();
    }

    public File packSourceJar() {
        return jarMaker().jar(sourceLayout.sources().and(sourceLayout.resources()), "sources");
    }

    public File packTestSourceJar() {
        return jarMaker().jar(sourceLayout.tests().and(sourceLayout.testResources()), "testSources");
    }

    public File packJavadocJar() {
        return jarMaker().jar(JkFileTreeSet.of(outLayout.getJavadocDir()), "javadoc");
    }

    public void runBinaryGenerationPhase() {
        this.generateSourcesAndResources();
        this.compile();
        this.processResources();
    }

    public void runUnitTestPhase() {
        this.generateTestResources();
        this.compileTest();
        this.processTestResources();
        this.executeTests();
    }

    protected void commonBuild() {
        if (commonBuildDone) {
            return;
        }
        runBinaryGenerationPhase();
        if (this.makeContext.getJuniter() != null) {
            runUnitTestPhase();
        }
        commonBuildDone = true;
    }

    public JkArtifactProducerDependency asDependency(JkArtifactFileId artifactId) {
        return new JkArtifactProducerDependency(this, artifactId, this.baseDir);
    }

    public JkArtifactProducerDependency asDependency() {
        return asDependency(mainArtifactFileId());
    }

    protected JkJavaCompiler applyEncodingAndVersion() {
        return this.makeContext.getBaseCompiler().withSourceVersion(compileVersion.source())
                .withTargetVersion(compileVersion.target()).withEncoding(encoding);
    }

    protected JkJavaCompiler applyCompileSource(JkJavaCompiler baseCompiler, JkDependencyResolver dependencyResolver) {
        JkPath classpath = dependencyResolver.get(
                this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME),
                JkJavaDepScopes.SCOPES_FOR_COMPILATION);
        return baseCompiler
                .withClasspath(classpath)
                .andSources(this.sourceLayout.sources())
                .andSources(JkFileTree.of(this.outLayout.generatedSourceDir()))
                .withOutputDir(this.outLayout.classDir());
    }

    protected JkJavaCompiler applyCompileTest() {
        return this.makeContext.getTestBaseCompiler()
                .withClasspath(makeContext.getDependencyResolver().get(
                        this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME),
                        JkJavaDepScopes.SCOPES_FOR_TEST).andHead(this.outLayout.classDir()))
                .andSources(this.sourceLayout.tests())
                .withOutputDir(this.outLayout.testClassDir());
    }

    protected JkUnit juniter() {
        final JkClasspath classpath = JkClasspath.of(this.outLayout.testClassDir(), this.outLayout.classDir()).and(
                this.makeContext.getDependencyResolver().get(this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME)
                        , JkJavaDepScopes.SCOPES_FOR_TEST));
        final File junitReport = new File(this.outLayout.testReportDir(), "junit");
        return this.makeContext.getJuniter().withClassesToTest(this.outLayout.testClassDir())
                .withClasspath(classpath)
                .withReportDir(junitReport);
    }

    protected JkJarMaker jarMaker() {
        return JkJarMaker.of(this.outLayout.outputDir(), getArtifactName());
    }

    /**
     * Short hand to build main jar artifact.
     */
    public File doMainJar() {
        this.doArtifactFile(mainArtifactFileId());
        return getArtifactFile(mainArtifactFileId());
    }

    @Override
    public String toString() {
        return this.baseDir.getName();
    }

    @Override
    public void doArtifactFile(JkArtifactFileId artifactFileId) {
        if (mainArtifactFileId().equals(artifactFileId)) {
            commonBuild();
            packJar();
        } else if (artifactFileId.equals(SOURCES_FILE_ID)) {
            packSourceJar();
        } else if (artifactFileId.equals(JAVADOC_FILE_ID)) {
            packJavadocJar();
        } else if (artifactFileId.equals(FAT_JAR_FILE_ID)) {
            commonBuild();
            packFatJar(FAT_JAR_FILE_ID.classifier());
        } else if (artifactFileId.equals(TEST_FILE_ID)) {
            commonBuild();
            packTestJar();
        } else if (artifactFileId.equals(TEST_SOURCE_FILE_ID)) {
            packTestSourceJar();
        } else {
            throw new IllegalArgumentException("No artifact with classifier/extension " + artifactFileId + " is defined on project " + this);
        }
    }

    @Override
    public JkArtifactFileIds extraArtifactFileIds() {
        return SOURCES_FILE_ID
                .and(JAVADOC_FILE_ID)
                .and(FAT_JAR_FILE_ID)
                .and(TEST_FILE_ID)
                .and(TEST_SOURCE_FILE_ID);
    }

    @Override
    public File getArtifactFile(JkArtifactFileId artifactId) {
        return jarMaker().file(artifactId.classifier(), artifactId.extension());
    }

    @Override
    public JkPath runtimeDependencies(JkArtifactFileId artifactFileId) {
        if (artifactFileId.equals(mainArtifactFileId())) {
            return this.makeContext.getDependencyResolver().get(
                    this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.RUNTIME);
        } else if (artifactFileId.isClassifier("test") && artifactFileId.isExtension("jar")) {
            return this.makeContext.getDependencyResolver().get(
                    this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.SCOPES_FOR_TEST);
        } else {
            return JkPath.of();
        }
    }

    // ---------------------------- Getters / setters --------------------------------------------

    public JkFileTree root() {
        return JkFileTree.of(this.baseDir);
    }

    @Override
    public JkProjectSourceLayout getSourceLayout() {
        return sourceLayout;
    }

    public JkProjectOutLayout getOutLayout() {
        return outLayout;
    }

    @Override
    public JkDependencies getDependencies() {
        return this.dependencies;
    }

    @Override
    public JkJavaCompileVersion getCompileVersion() {
        return compileVersion;
    }

    public JkJavaProjectMakeContext getMakeContext() {
        return makeContext;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public JkJarProject setArtifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    public JkJarProject setSourceLayout(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout.withBaseDir(this.baseDir);
        return this;
    }

    public JkJarProject setOutLayout(JkProjectOutLayout outLayout) {
        this.outLayout = outLayout.withOutputBaseDir(this.baseDir);
        return this;
    }

    public JkJarProject setDependencies(JkDependencies dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public JkJarProject setCompileVersion(JkJavaCompileVersion compileVersion) {
        this.compileVersion = compileVersion;
        return this;
    }

    public JkJarProject setMakeContext(JkJavaProjectMakeContext makeContext) {
        this.makeContext = makeContext;
        return this;
    }

    public List<JkResourceProcessor.JkInterpolator> getResourceInterpolators() {
        return resourceInterpolators;
    }
}