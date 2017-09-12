package org.jerkar.api.project.java;

import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkTestSuiteResult;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.project.*;
import org.jerkar.api.system.JkLog;

import java.io.File;
import java.util.*;


@Deprecated // Experimental !!!!
public class JkJavaProject implements JkJavaProjectDefinition, JkArtifactProducer, JkFileSystemLocalizable {

    public static final JkPathFilter RESOURCE_FILTER = JkPathFilter.exclude("**/*.java")
            .andExclude("**/package.html").andExclude("**/doc-files");

    public static final JkArtifactFileId SOURCES_FILE_ID = JkArtifactFileId.of("sources", "jar");

    public static final JkArtifactFileId JAVADOC_FILE_ID = JkArtifactFileId.of("javadoc", "jar");

    public static final JkArtifactFileId TEST_FILE_ID = JkArtifactFileId.of("test", "jar");

    public static final JkArtifactFileId TEST_SOURCE_FILE_ID = JkArtifactFileId.of("test-sources", "jar");

    private final File baseDir;

    private String artifactName;

    private JkProjectSourceLayout sourceLayout;

    private JkProjectOutLayout outLayout;

    private JkDependencies dependencies;

    private JkJavaCompileVersion compileVersion = JkJavaCompileVersion.V8;

    private List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<JkResourceProcessor.JkInterpolator>();

    private JkManifest manifest = JkManifest.empty();

    private JkJavaProjectMakeContext makeContext = new JkJavaProjectMakeContext(this);

    private String encoding = "UTF-8";

    private JkFileTreeSet extraFilesToIncludeInFatJar = JkFileTreeSet.empty();

    private Map<JkArtifactFileId, Runnable> artifactProducers = new LinkedHashMap<>();

    private boolean commonBuildDone;

    private JkVersionedModule versionedModule;

    public JkJavaProject(File baseDir) {
        this.baseDir = baseDir;
        this.artifactName = this.baseDir.getName();
        this.sourceLayout = JkProjectSourceLayout.mavenJava().withBaseDir(baseDir);
        this.outLayout = JkProjectOutLayout.classicJava().withOutputDir(new File(baseDir, "build/output"));
        this.dependencies = JkDependencies.ofLocalScoped(new File(baseDir, "build/libs"));
        this.addDefaultArtifactFiles();
    }

    public void clean() {
        commonBuildDone = false;
        makeContext.getCleaner().run();
    }

    public void generateSourcesAndResources() {
        makeContext.getSourceGenerator().run();
        makeContext.getResourceGenerator().run();
    }

    public void compile() {
        JkJavaCompiler comp = applyEncodingAndVersion();
        comp = applyCompileSource(comp, this.makeContext.getDependencyResolver());
        comp = makeContext.getConfiguredProductionCompiler().apply(comp);
        comp.compile();
        makeContext.getPostCompilePhase().run();
    }

    public void processResources() {
        makeContext.getResourceProcessor().run();
    }

    public void generateTestResources() {
        makeContext.getTestResourceGenerator().run();
    }

    public void compileTest() {
        JkJavaCompiler comp = applyEncodingAndVersion();
        comp = applyCompileTest();
        comp = makeContext.getConfiguredTestCompiler().apply(comp);
        comp.compile();
        makeContext.getTestPostCompiler().run();
    }

    public void processTestResources() {
        makeContext.getTestResourceProcessor().run();
    }

    public JkTestSuiteResult executeTests() {
        JkUnit juniter = juniter();
        juniter = makeContext.getJuniterConfigurer().apply(juniter);
        JkTestSuiteResult result = juniter.run();
        makeContext.getPostTestPhase().run();
        return result;
    }

    protected void addDefaultArtifactFiles() {
        this.addArtifactFile(mainArtifactFileId(), () -> {commonBuild(); packMainJar();});
        this.addArtifactFile(SOURCES_FILE_ID, () -> packSourceJar());
        this.addArtifactFile(JAVADOC_FILE_ID, () -> {generateJavadoc(); packJavadocJar();});
    }

    public JkJavaProject addTestArtifactFiles() {
        this.addArtifactFile(TEST_FILE_ID, () -> {commonBuild(); packTestJar();});
        this.addArtifactFile(TEST_SOURCE_FILE_ID, () -> {packTestSourceJar();});
        return this;
    }

    public JkJavaProject addFatJarArtifactFile(String classifier) {
        this.addArtifactFile(JkArtifactFileId.of(classifier, "jar"),
                () -> {commonBuild(); packFatJar(classifier);});
        return this;
    }

    public File packMainJar() {
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

    public void runCompilationPhase() {
        JkLog.startln("Running compilation phase");
        this.generateSourcesAndResources();
        this.compile();
        this.processResources();
        JkLog.done();
    }

    public void runUnitTestPhase() {
        JkLog.startln("Running unit test phase");
        this.generateTestResources();
        this.compileTest();
        this.processTestResources();
        this.executeTests();
        JkLog.done();
    }



    protected void commonBuild() {
        if (commonBuildDone) {
            return;
        }
        runCompilationPhase();
        if (this.makeContext.getJuniter() != null) {
            runUnitTestPhase();
        }
        commonBuildDone = true;
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
        return artifactFile(mainArtifactFileId());
    }

    @Override
    public String toString() {
        return this.baseDir.getName();
    }

    @Override
    public final void doArtifactFile(JkArtifactFileId artifactFileId) {
        if (artifactProducers.containsKey(artifactFileId)) {
            JkLog.startln("Producing artifact file " + artifactFile(artifactFileId).getName());
            artifactProducers.get(artifactFileId).run();
            JkLog.done();
        } else {
            throw new IllegalArgumentException("No artifact with classifier/extension " + artifactFileId + " is defined on project " + this);
        }
    }



    @Override
    public final Iterable<JkArtifactFileId> artifactFileIds() {
        return this.artifactProducers.keySet();
    }

    @Override
    public final File artifactFile(JkArtifactFileId artifactId) {
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

    public File baseDir() {
        return this.baseDir;
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

    public JkJavaProject setArtifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    public JkJavaProject setSourceLayout(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout.withBaseDir(this.baseDir);
        return this;
    }

    public JkJavaProject setOutLayout(JkProjectOutLayout outLayout) {
        if (outLayout.outputDir().isAbsolute()) {
            this.outLayout = outLayout;;
        } else {
            this.outLayout = outLayout.withOutputDir(new File(this.baseDir, outLayout.outputDir().getPath()));
        }
        return this;
    }

    public JkJavaProject setDependencies(JkDependencies dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public JkJavaProject setCompileVersion(JkJavaCompileVersion compileVersion) {
        this.compileVersion = compileVersion;
        return this;
    }

    public JkJavaProject setMakeContext(JkJavaProjectMakeContext makeContext) {
        this.makeContext = makeContext;
        return this;
    }

    public List<JkResourceProcessor.JkInterpolator> getResourceInterpolators() {
        return resourceInterpolators;
    }

    public JkJavaProject addArtifactFile(JkArtifactFileId artifactFileId, Runnable runnable) {
        this.artifactProducers.put(artifactFileId, runnable);
        return this;
    }

    public JkJavaProject removeArtifactFile(JkArtifactFileId artifactFileId) {
        this.artifactProducers.remove(artifactFileId);
        return this;
    }

    public boolean contains(JkArtifactFileId artifactFileId) {
        return this.artifactProducers.containsKey(artifactFileId);
    }

    public JkVersionedModule getVersionedModule() {
        return versionedModule;
    }

    public JkJavaProject setVersionedModule(JkVersionedModule versionedModule) {
        this.versionedModule = versionedModule;
        return this;
    }

    public JkJavaProject setVersionedModule(String groupAndName, String version) {
        return setVersionedModule(JkModuleId.of(groupAndName).version(version));
    }
}