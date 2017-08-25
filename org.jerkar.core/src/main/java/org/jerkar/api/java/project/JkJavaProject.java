package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkVersionProvider;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkTestSuiteResult;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.tool.JkProject;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Deprecated // Experimental !!!!
public class JkJavaProject {

    public static final JkPathFilter RESOURCE_FILTER = JkPathFilter.exclude("**/*.java")
            .andExclude("**/package.html").andExclude("**/doc-files");

    public static JkJavaProject of(File baseDir) {
        JkProjectSourceLayout sourceLayout = JkProjectSourceLayout.mavenJava().withBaseDir(baseDir);
        JkProjectOutLayout outLayout = JkProjectOutLayout.classic().withOutputBaseDir(new File(baseDir, "build/output"));
        return new JkJavaProject(baseDir, sourceLayout, outLayout, JkDependencies.of(), null, null, null);
    }

    public static JkJavaProject of() {
        return of(new File("."));
    }

    private final File baseDir;

    private JkProjectSourceLayout sourceLayout;

    private JkProjectOutLayout outLayout;

    private JkDependencies dependencies;

    private JkVersionProvider forcedVersions;

    private JkJavaVersion sourceVersion;

    private JkJavaVersion targetVersion;

    private List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<JkResourceProcessor.JkInterpolator>();

    private JkManifest manifest = JkManifest.empty();

    private String encoding = "UTF-8";

    public JkJavaProject(File baseDir, JkProjectSourceLayout sourceLayout, JkProjectOutLayout outLayout,
                            JkDependencies dependencies, JkVersionProvider forcedVersions,
                            JkJavaVersion sourceVersion, JkJavaVersion targetVersion) {
        this.baseDir = baseDir;
        this.sourceLayout = sourceLayout;
        this.outLayout = outLayout;
        this.dependencies = dependencies;
        this.forcedVersions = forcedVersions;
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
    }

    public void generateSourceAndResources(Map<String, String> options) {
        // Do nothing by default
    }

    public void generateTestResources(Map<String, String> options) {
        // Do nothing by default
    }

    public void compile(final JkJavaCompiler baseCompiler, JkDependencyResolver resolver, Map<String, String> options) {
        JkJavaCompiler comp = applyEncodingAndVersion(baseCompiler);
        comp = applyCompileSource(comp, resolver);
        comp.compile();
    }

    public void processResources(Map<String, String> options) {
        JkResourceProcessor.of(this.sourceLayout.resources())
                .and(JkFileTree.of(this.outLayout.classDir()).andFilter(RESOURCE_FILTER))
                .and(this.outLayout.generatedResourceDir())
                .and(resourceInterpolators)
                .generateTo(this.outLayout.classDir());
    }

    public void compileTest(final JkJavaCompiler baseCompiler, JkDependencyResolver dependencyResolver, Map<String, String> options) {
        JkJavaCompiler comp = applyEncodingAndVersion(baseCompiler);
        comp = applyCompileTest(comp, dependencyResolver);
        comp.compile();
    }

    public void processTestResources(Map<String, String> options) {
        JkResourceProcessor.of(this.sourceLayout.testResources())
                .and(JkFileTree.of(this.outLayout.testClassDir()).andFilter(RESOURCE_FILTER))
                .and(this.outLayout.generatedTestResourceDir())
                .and(resourceInterpolators)
                .generateTo(this.outLayout.testClassDir());
    }

    public JkTestSuiteResult runTests(JkUnit baseUniter, JkDependencyResolver dependencyResolver, Map<String, String> options) {
        JkUnit juniter = apply(dependencyResolver, baseUniter);
        return juniter.run();
    }

    public File makeJar(Map<String, String> options) {
        return jarMaker().mainJar(manifest, outLayout.classDir(), JkFileTreeSet.empty());
    }

    public List<String> extraJars() {
        return new LinkedList<String>();
    }

    public File makeFatFar(JkDependencyResolver baseDependencyResolver, Map<String, String> options) {
        JkDependencyResolver depResolver = baseDependencyResolver.withTransitiveVersionOverride(this.forcedVersions);
        JkClasspath classpath = JkClasspath.of(depResolver.get(this.dependencies, JkJavaDepScopes.RUNTIME));
        return jarMaker().fatJar(manifest, this.outLayout.classDir(), classpath, JkFileTreeSet.empty());
    }

    public File makeTestJar(Map<String, String> options) {
        return jarMaker().testJar(outLayout.testClassDir());
    }

    public File generateJavadoc(JkDependencyResolver baseDependencyResolver, Class<?> docletClass, Map<String, String> options) {
        JkJavadocMaker maker = JkJavadocMaker.of(this.sourceLayout.sources(), this.outLayout.getJavadocDir())
                .withDoclet(docletClass);
        maker.process();
        return this.outLayout.getJavadocDir();
    }

    public File makeSourceJar(Map<String, String> options) {
        return jarMaker().jar(sourceLayout.sources().and(sourceLayout.resources()), "sources");
    }

    public File makeTestSourceJar(Map<String, String> options) {
        return jarMaker().jar(sourceLayout.tests().and(sourceLayout.testResources()), "testSources");
    }

    public File makeJavadocJar(Map<String, String> options) {
        return jarMaker().jar(JkFileTreeSet.of(outLayout.getJavadocDir()), "javadoc");
    }

    public void buildMainJar(JkDependencyResolver dependencyResolver, JkJavaCompiler compiler, JkUnit juniter, Map<String, String> options) {
        generateSourceAndResources(options);
        compile(compiler, dependencyResolver, options);
        generateTestResources(options);
        compileTest(compiler, dependencyResolver, options);
        runTests(juniter, dependencyResolver, options );
        makeJar(options);
    }

    public File getJar(String suffix) {
        return jarMaker().file(suffix);
    }

    public File getMainJar() {
        return getJar(null);
    }

    public JkJavaProjectDependency asDependency(JkDependencyResolver resolver, JkJavaCompiler compiler,
                                                JkUnit juniter, Map<String, String> options) {
        return new JkJavaProjectDependency(this, resolver, compiler, juniter, options);

    }

    public JkJavaProjectDependency asDependency() {
        return new JkJavaProjectDependency(this,
                JkDependencyResolver.managed(JkRepos.mavenCentral()),
                JkJavaCompiler.base(), JkUnit.of(), new HashMap<String, String>());

    }


    protected JkJavaCompiler applyEncodingAndVersion(JkJavaCompiler compiler) {
        JkJavaVersion source = this.sourceVersion != null ? this.sourceVersion : this.targetVersion;
        JkJavaVersion target = this.targetVersion != null ? this.targetVersion : this.sourceVersion;
        return compiler.withSourceVersion(source).withTargetVersion(target).withEncoding(encoding);
    }

    protected JkJavaCompiler applyCompileSource(JkJavaCompiler baseCompiler, JkDependencyResolver dependencyResolver) {
        JkPath classpath = dependencyResolver.get(this.dependencies, JkJavaDepScopes.SCOPES_FOR_COMPILATION);
        return baseCompiler
                .withClasspath(classpath)
                .andSources(this.sourceLayout.sources())
                .andSources(JkFileTree.of(this.outLayout.generatedSourceDir()))
                .withOutputDir(this.outLayout.classDir());
    }

    protected JkJavaCompiler applyCompileTest(JkJavaCompiler baseCompiler, JkDependencyResolver dependencyResolver) {
        return baseCompiler
                .withClasspath(dependencyResolver.get(this.dependencies, JkJavaDepScopes.SCOPES_FOR_TEST))
                .andSources(this.sourceLayout.tests())
                .withOutputDir(this.outLayout.testClassDir());
    }

    protected JkUnit apply(JkDependencyResolver dependencyResolver, JkUnit juniter) {
        final JkClasspath classpath = JkClasspath.of(this.outLayout.testClassDir(), this.outLayout.classDir()).and(
                dependencyResolver.get(this.dependencies, JkJavaDepScopes.SCOPES_FOR_TEST));
        final File junitReport = new File(this.outLayout.testReportDir(), "junit");
        return juniter.withClassesToTest(this.outLayout.testClassDir())
                .withClasspath(classpath)
                .withReportDir(junitReport);
    }

    protected JkJarMaker jarMaker() {
        return JkJarMaker.of(this.outLayout.outputDir(), "myname");
    }

    // ---------------------------- Getters / setters --------------------------------------------


    public JkProjectSourceLayout getSourceLayout() {
        return sourceLayout;
    }

    public JkProjectOutLayout getOutLayout() {
        return outLayout;
    }

    public JkDependencies dependencies(Map<String, String> options) {
        return this.dependencies;
    }

    public JkVersionProvider getForcedVersions() {
        return forcedVersions;
    }

    public JkJavaProject setSourceLayout(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout.withBaseDir(this.baseDir);
        return this;
    }

    public JkJavaProject setOutLayout(JkProjectOutLayout outLayout) {
        this.outLayout = outLayout.withOutputBaseDir(this.baseDir);
        return this;
    }

    public JkJavaProject setDependencies(JkDependencies dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public JkJavaProject setSourceVersion(JkJavaVersion sourceVersion) {
        this.sourceVersion = sourceVersion;
        return this;
    }

    public JkJavaProject setTargetVersion(JkJavaVersion targetVersion) {
        this.targetVersion = targetVersion;
        return this;
    }

    public JkJavaProject setSourceAndTargetVersion(JkJavaVersion version) {
        this.sourceVersion = version;
        this.targetVersion = version;
        return this;
    }




    @Override
    public String toString() {
        return this.baseDir.getName();
    }
}











