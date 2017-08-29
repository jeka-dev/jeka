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
import org.jerkar.api.utils.JkUtilsFile;

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

    public static JkJarProject of(File baseDir) {
        JkProjectSourceLayout sourceLayout = JkProjectSourceLayout.mavenJava().withBaseDir(baseDir);
        JkProjectOutLayout outLayout = JkProjectOutLayout.classicJava().withOutputBaseDir(new File(baseDir, "build/output"));
        return new JkJarProject(baseDir, sourceLayout, outLayout, JkDependencies.of(), null, null);
    }

    public static JkJarProject of() {
        return of(new File("."));
    }

    private final File baseDir;

    private String artifactName;

    private JkProjectSourceLayout sourceLayout;

    private JkProjectOutLayout outLayout;

    private JkDependencies dependencies;

    private JkJavaVersion sourceVersion;

    private JkJavaVersion targetVersion;

    private List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<JkResourceProcessor.JkInterpolator>();

    private JkManifest manifest = JkManifest.empty();

    private JkJavaProjectMakeContext makeContext = JkJavaProjectMakeContext.of();

    private String encoding = "UTF-8";

    public JkJarProject(File baseDir, JkProjectSourceLayout sourceLayout, JkProjectOutLayout outLayout,
                        JkDependencies dependencies,
                        JkJavaVersion sourceVersion, JkJavaVersion targetVersion) {
        this.baseDir = baseDir;
        this.artifactName = this.baseDir.getName();
        this.sourceLayout = sourceLayout;
        this.outLayout = outLayout;
        this.dependencies = dependencies;
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
    }

    void clean() {
        JkUtilsFile.deleteDirContent(this.outLayout.outputDir());
    }

    public void generateSourcesAndResources() {
        // Do nothing by default
    }

    public void generateTestResources() {
        // Do nothing by default
    }

    public void compile() {
        JkJavaCompiler comp = applyEncodingAndVersion();
        comp = applyCompileSource(comp, this.makeContext.dependencyResolver());
        comp.compile();
    }

    public void processResources() {
        JkResourceProcessor.of(this.sourceLayout.resources())
                .and(JkFileTree.of(this.outLayout.classDir()).andFilter(RESOURCE_FILTER))
                .and(this.outLayout.generatedResourceDir())
                .and(resourceInterpolators)
                .generateTo(this.outLayout.classDir());
    }

    public void compileTest() {
        JkJavaCompiler comp = applyEncodingAndVersion();
        comp = applyCompileTest();
        comp.compile();
    }

    public void processTestResources() {
        JkResourceProcessor.of(this.sourceLayout.testResources())
                .and(JkFileTree.of(this.outLayout.testClassDir()).andFilter(RESOURCE_FILTER))
                .and(this.outLayout.generatedTestResourceDir())
                .and(resourceInterpolators)
                .generateTo(this.outLayout.testClassDir());
    }

    public JkTestSuiteResult runTests() {
        JkUnit juniter = applyJunit();
        return juniter.run();
    }

    public File packJar() {
        return jarMaker().mainJar(manifest, outLayout.classDir(), JkFileTreeSet.empty());
    }

    public File packFatJar() {
        JkDependencyResolver depResolver = this.makeContext.dependencyResolver();
        JkClasspath classpath = JkClasspath.of(depResolver.get(this.dependencies, JkJavaDepScopes.RUNTIME));
        return jarMaker().fatJar(manifest, this.outLayout.classDir(), classpath, JkFileTreeSet.empty());
    }

    public File packTestJar() {
        return jarMaker().testJar(outLayout.testClassDir());
    }

    public File generateJavadoc() {
        JkJavadocMaker maker = JkJavadocMaker.of(this.sourceLayout.sources(), this.outLayout.getJavadocDir())
                .withDoclet(this.makeContext.javadocDoclet());
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

    protected void commonBuild() {
        generateSourcesAndResources();
        compile();
        generateSourcesAndResources();
        if (this.makeContext.juniter() != null) {
            generateTestResources();
            compileTest();
            runTests();
        }
    }

    public JkArtifactProducerDependency asDependency(JkArtifactFileId artifactId) {
        return new JkArtifactProducerDependency(this, artifactId, this.baseDir);
    }

    public JkArtifactProducerDependency asDependency() {
        return asDependency(mainArtifactFileId());
    }

    protected JkJavaCompiler applyEncodingAndVersion() {
        JkJavaVersion source = this.sourceVersion != null ? this.sourceVersion : this.targetVersion;
        JkJavaVersion target = this.targetVersion != null ? this.targetVersion : this.sourceVersion;
        return this.makeContext.baseCompiler().withSourceVersion(source).withTargetVersion(target).withEncoding(encoding);
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
        return this.makeContext.baseCompiler()
                .withClasspath(makeContext.dependencyResolver().get(
                        this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME),
                        JkJavaDepScopes.SCOPES_FOR_TEST))
                .andSources(this.sourceLayout.tests())
                .withOutputDir(this.outLayout.testClassDir());
    }

    protected JkUnit applyJunit() {
        final JkClasspath classpath = JkClasspath.of(this.outLayout.testClassDir(), this.outLayout.classDir()).and(
                this.makeContext.dependencyResolver().get(this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME)
                        , JkJavaDepScopes.SCOPES_FOR_TEST));
        final File junitReport = new File(this.outLayout.testReportDir(), "junit");
        return this.makeContext.juniter().withClassesToTest(this.outLayout.testClassDir())
                .withClasspath(classpath)
                .withReportDir(junitReport);
    }

    protected JkJarMaker jarMaker() {
        return JkJarMaker.of(this.outLayout.outputDir(), getArtifactName());
    }

    /**
     * Short hand to build main jar artifact.
     */
    public File produceMainJar() {
        produceArtifactFile(mainArtifactFileId());
        return getArtifactFile(mainArtifactFileId());
    }

    @Override
    public String toString() {
        return this.baseDir.getName();
    }

    @Override
    public void produceArtifactFile(JkArtifactFileId artifactFileId) {
        if (mainArtifactFileId().equals(artifactFileId)) {
            commonBuild();
            packJar();
        } else if (artifactFileId.equals(SOURCES_FILE_ID)) {
            packSourceJar();
        } else if (artifactFileId.equals(JAVADOC_FILE_ID)) {
            packJavadocJar();
        } else if (artifactFileId.equals(FAT_JAR_FILE_ID)) {
            commonBuild();
            packFatJar();
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
    public Iterable<JkArtifactFileId> extraArtifactFileIds() {
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
            return this.makeContext.dependencyResolver().get(
                    this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.RUNTIME);
        } else if (artifactFileId.isClassifier("test") && artifactFileId.isExtension("jar")) {
            return this.makeContext.dependencyResolver().get(
                    this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.SCOPES_FOR_TEST);
        } else {
            return JkPath.of();
        }
    }

    // ---------------------------- Getters / setters --------------------------------------------


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
    public JkJavaVersion getSourceVersion() {
        return sourceVersion;
    }

    @Override
    public JkJavaVersion getTargetVersion() {
        return targetVersion;
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

    public JkJarProject setSourceVersion(JkJavaVersion sourceVersion) {
        this.sourceVersion = sourceVersion;
        return this;
    }

    public JkJarProject setTargetVersion(JkJavaVersion targetVersion) {
        this.targetVersion = targetVersion;
        return this;
    }

    public JkJarProject setSourceAndTargetVersion(JkJavaVersion version) {
        this.sourceVersion = version;
        this.targetVersion = version;
        return this;
    }

    public JkJarProject setMakeContext(JkJavaProjectMakeContext makeContext) {
        this.makeContext = makeContext;
        return this;
    }

}











