package org.jerkar.api.project.java;

import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.*;
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

    // A project has either a name either a versioned module.
    private String artifactName;

    // A project has either a name either a versioned module.
    private JkVersionedModule versionedModule;

    private JkProjectSourceLayout sourceLayout;

    private JkProjectOutLayout outLayout;

    private JkDependencies dependencies;

    private JkJavaCompilerSpec compileSpec =
            JkJavaCompilerSpec.of(JkJavaVersion.V8).withEncoding("UTF-8");

    private List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<JkResourceProcessor.JkInterpolator>();

    private JkManifest manifest = JkManifest.empty();

    private JkFileTreeSet extraFilesToIncludeInFatJar = JkFileTreeSet.empty();

    private JkMavenPublicationInfo mavenPublicationInfo;

    private Map<JkArtifactFileId, Runnable> artifactProducers = new LinkedHashMap<>();

    private JkJavaProjectMaker maker = new JkJavaProjectMaker(this);

    public JkJavaProject(File baseDir) {
        this.baseDir = baseDir;
        this.artifactName = this.baseDir.getName();
        this.sourceLayout = JkProjectSourceLayout.mavenJava().withBaseDir(baseDir);
        this.outLayout = JkProjectOutLayout.classicJava().withOutputDir(new File(baseDir, "build/output"));
        this.dependencies = JkDependencies.ofLocalScoped(new File(baseDir, "build/libs"));
        this.addDefaultArtifactFiles();
    }

    protected void addDefaultArtifactFiles() {
        this.addArtifactFile(mainArtifactFileId(), () -> {
            maker.commonBuild(); this.maker.packMainJar();});
        this.addArtifactFile(SOURCES_FILE_ID, () -> this.maker.packSourceJar());
        this.addArtifactFile(JAVADOC_FILE_ID, () -> {this.maker.generateJavadoc(); this.maker.packJavadocJar();});
    }

    public JkJavaProject addTestArtifactFiles() {
        this.addArtifactFile(TEST_FILE_ID, () -> {maker.commonBuild(); this.maker.packTestJar();});
        this.addArtifactFile(TEST_SOURCE_FILE_ID, () -> {this.maker.packTestSourceJar();});
        return this;
    }

    public JkJavaProject addFatJarArtifactFile(String classifier) {
        this.addArtifactFile(JkArtifactFileId.of(classifier, "jar"),
                () -> {maker.commonBuild(); this.maker.packFatJar(classifier);});
        return this;
    }



    protected JkJavaCompiler applyEncodingAndVersion() {
        return this.maker.getBaseCompiler().andOptions(this.compileSpec.asOptions());
    }

    protected JkJavaCompiler applyCompileTest() {
        return this.maker.getTestBaseCompiler()
                .withClasspath(maker.getDependencyResolver().get(
                        this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME),
                        JkJavaDepScopes.SCOPES_FOR_TEST).andHead(this.outLayout.classDir()))
                .andSources(this.sourceLayout.tests())
                .withOutputDir(this.outLayout.testClassDir());
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
        return maker.getJarMaker().file(artifactId.classifier(), artifactId.extension());
    }

    @Override
    public JkPath runtimeDependencies(JkArtifactFileId artifactFileId) {
        if (artifactFileId.equals(mainArtifactFileId())) {
            return this.maker.getDependencyResolver().get(
                    this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.RUNTIME);
        } else if (artifactFileId.isClassifier("test") && artifactFileId.isExtension("jar")) {
            return this.maker.getDependencyResolver().get(
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
    public JkJavaVersion getTargetVersion() {
        return this.compileSpec.getTargetVersion();
    }

    public JkJavaProjectMaker maker() {
        return maker;
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

    public JkJavaProject setCompilerSpecification(JkJavaCompilerSpec compileSpec) {
        this.compileSpec = compileSpec;
        return this;
    }

    public JkJavaProject setMaker(JkJavaProjectMaker maker) {
        this.maker = maker;
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

    public JkJavaCompilerSpec getCompileSpec() {
        return compileSpec;
    }

    public JkJavaProject setCompileSpec(JkJavaCompilerSpec compileSpec) {
        this.compileSpec = compileSpec;
        return this;
    }

    public JkManifest getManifest() {
        return manifest;
    }

    public JkJavaProject setManifest(JkManifest manifest) {
        this.manifest = manifest;
        return this;
    }

    public JkFileTreeSet getExtraFilesToIncludeInFatJar() {
        return extraFilesToIncludeInFatJar;
    }

    public JkJavaProject setExtraFilesToIncludeInFatJar(JkFileTreeSet extraFilesToIncludeInFatJar) {
        this.extraFilesToIncludeInFatJar = extraFilesToIncludeInFatJar;
        return this;
    }

    public JkMavenPublicationInfo getMavenPublicationInfo() {
        return mavenPublicationInfo;
    }
}