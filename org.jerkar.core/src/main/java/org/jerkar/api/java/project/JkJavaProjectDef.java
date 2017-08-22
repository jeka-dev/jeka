package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkVersionProvider;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkTestSuiteResult;
import org.jerkar.api.java.junit.JkUnit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Deprecated // Experimental !!!!
public class JkJavaProjectDef {

    public static final JkPathFilter RESOURCE_FILTER = JkPathFilter.exclude("**/*.java")
            .andExclude("**/package.html").andExclude("**/doc-files");

    public static JkJavaProjectDef of() {
        File baseDir = new File(".");
        JkProjectSourceLayout sourceLayout = JkProjectSourceLayout.mavenJava().withBaseDir(baseDir);
        JkProjectOutLayout outLayout = JkProjectOutLayout.classic().withOutputBaseDir(new File(baseDir, "build/output"));
        return new JkJavaProjectDef(sourceLayout, outLayout, null, null, null, null);
    }

    private final JkProjectSourceLayout sourceLayout;

    private final JkProjectOutLayout outLayout;

    private final JkDependencies dependencies;

    private final JkVersionProvider forcedVersions;

    private final JkJavaVersion sourceVersion;

    private final JkJavaVersion targetVersion;

    private List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<JkResourceProcessor.JkInterpolator>();

    private JkManifest manifest = JkManifest.empty();

    private String encoding = "UTF-8";

    public JkJavaProjectDef(JkProjectSourceLayout sourceLayout, JkProjectOutLayout outLayout,
                            JkDependencies dependencies, JkVersionProvider forcedVersions,
                            JkJavaVersion sourceVersion, JkJavaVersion targetVersion) {
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

    public void compile(final JkJavaCompiler baseCompiler, JkRepos dependencyRepositories, Map<String, String> options) {
        JkDependencyResolver dependencyResolver = JkDependencyResolver.managed(dependencyRepositories, this.dependencies);
        JkJavaCompiler comp = applyEncodingAndVersion(baseCompiler);
        comp = applyCompileSource(comp, dependencyResolver);
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

    public JkTestSuiteResult runTests(JkUnit baseUniter, JkDependencyResolver baseDependencyResolver, Map<String, String> options) {
        JkDependencyResolver depResolver = baseDependencyResolver.withDeps(this.dependencies).withTransitiveVersionOverride(this.forcedVersions);
        JkUnit juniter = apply(depResolver, baseUniter);
        return juniter.run();
    }

    public File makeJar(Map<String, String> options) {
        return jarMaker().mainJar(manifest, outLayout.classDir(), JkFileTreeSet.empty());
    }

    public File makeFatFar(JkDependencyResolver baseDependencyResolver, Map<String, String> options) {
        JkDependencyResolver depResolver = baseDependencyResolver.withDeps(this.dependencies).withTransitiveVersionOverride(this.forcedVersions);
        JkClasspath classpath = JkClasspath.of(depResolver.get(JkJavaDepScopes.RUNTIME));
        return jarMaker().fatJar(manifest, this.outLayout.classDir(), classpath, JkFileTreeSet.empty());
    }

    public File makeTestJar(Map<String, String> options) {
        return jarMaker().testJar(outLayout.testClassDir());
    }

    public File makeSourceJar(Map<String, String> options) {
        return jarMaker().jar(sourceLayout.sources().and(sourceLayout.resources()), "sources");
    }

    public File makeTestSourceJar(Map<String, String> options) {
        return jarMaker().jar(sourceLayout.tests().and(sourceLayout.testResources()), "testSources");
    }

    public File getJar(String suffix) {
        return jarMaker().file(suffix);
    }

    public File getMainJar() {
        return getJar(null);
    }


    protected JkJavaCompiler applyEncodingAndVersion(JkJavaCompiler compiler) {
        JkJavaVersion source = this.sourceVersion != null ? this.sourceVersion : this.targetVersion;
        JkJavaVersion target = this.targetVersion != null ? this.targetVersion : this.sourceVersion;
        return compiler.withSourceVersion(source).withTargetVersion(target).withEncoding(encoding);
    }

    protected JkJavaCompiler applyCompileSource(JkJavaCompiler baseCompiler, JkDependencyResolver dependencyResolver) {
        return baseCompiler
                .withClasspath(dependencyResolver.get(JkJavaDepScopes.SCOPES_FOR_COMPILATION))
                .andSources(this.sourceLayout.sources())
                .andSources(JkFileTree.of(this.outLayout.generatedSourceDir()))
                .withOutputDir(this.outLayout.classDir());
    }

    protected JkJavaCompiler applyCompileTest(JkJavaCompiler baseCompiler, JkDependencyResolver dependencyResolver) {
        return baseCompiler
                .withClasspath(dependencyResolver.get(JkJavaDepScopes.SCOPES_FOR_TEST))
                .andSources(this.sourceLayout.tests())
                .withOutputDir(this.outLayout.testClassDir());
    }

    protected JkUnit apply(JkDependencyResolver dependencyResolver, JkUnit juniter) {
        final JkClasspath classpath = JkClasspath.of(this.outLayout.testClassDir(), this.outLayout.classDir()).and(
                dependencyResolver.get(JkJavaDepScopes.SCOPES_FOR_TEST));
        final File junitReport = new File(this.outLayout.testReportDir(), "junit");
        return juniter.withClassesToTest(this.outLayout.testClassDir())
                .withClasspath(classpath)
                .withReportDir(junitReport);
    }

    protected JkJarMaker jarMaker() {
        return JkJarMaker.of(this.outLayout.outputDir(), "myname");
    }
}











