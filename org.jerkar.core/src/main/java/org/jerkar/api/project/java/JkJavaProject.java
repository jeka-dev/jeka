package org.jerkar.api.project.java;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.depmanagement.JkArtifactProducer;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkJavaCompilerSpec;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.project.JkProjectOutLayout;
import org.jerkar.api.project.JkProjectSourceLayout;

/**
 * Beware : Experimental !!!!!!!!!!!!!!!!!!!!!!!
 * The API is likely to change subsequently.
 *
 * Container for a Java project with classic characteristic :
 * <ul>
 *     <li>Contains Java source files to be compiled</li>
 *     <li>All Java sources file (prod + test) are wriiten against the same Java version and encoding</li>
 *     <li>Project may contain unit tests</li>
 *     <li>It can depends on any accepted dependencies (Maven module, other project, files on fs, ...)</li>
 *
 *     <li>It produces a jar a source jar and a javadoc jar</li>
 *     <li>It can produces any other artifact files (fat-jar, test jar, doc, ...)</li>
 *     <li>It can be identified as a Maven module (means it can provide a group, artifact id, version) in order to be published/reused</li>
 *     <li>It can be published on any Maven/Ivy repository, even Maven central</li>
 *
 *     <li>Part of the sources/resources may be generated</li>
 *     <li>By default, passing test suit is required to produce artifact.</li>
 * </ul>
 *
 * Beside, java projects are highly extensible so you can add build tasks or alter existing ones. This
 * is done using {@link #maker()} object. For example you can easily add test cover or SonarQube analysis.
 *
 * It provides cache mechanism in order compile or unit test phases are executed once when generating
 * several artifact files so be aware of clean it if you want to replay some tasks with different settings.
 *
 * @See JkJavaProjectMaker
 */
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

    private final List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<>();

    private JkManifest manifest = JkManifest.empty();

    private JkFileTreeSet extraFilesToIncludeInFatJar = JkFileTreeSet.empty();

    private JkMavenPublicationInfo mavenPublicationInfo;

    private JkJavaProjectMaker maker = new JkJavaProjectMaker(this);

    public JkJavaProject(File baseDir) {
        this.baseDir = baseDir;
        this.artifactName = this.baseDir.getName();
        this.sourceLayout = JkProjectSourceLayout.mavenJava().withBaseDir(baseDir);
        this.outLayout = JkProjectOutLayout.classicJava().withOutputDir(new File(baseDir, "build/output"));
        this.dependencies = JkDependencies.ofLocalScoped(new File(baseDir, "build/libs"));
        this.addDefaultArtifactFiles();
    }

    // ----------- artifact management --------------------------------------

    protected void addDefaultArtifactFiles() {
        this.maker.addArtifactFile(mainArtifactFileId(), () -> maker.makeBinJar());
        this.maker.addArtifactFile(SOURCES_FILE_ID, () -> maker.makeSourceJar());
        this.maker.addArtifactFile(JAVADOC_FILE_ID, () -> maker.makeJavadocJar());
    }

    public JkJavaProject addArtifactFile(JkArtifactFileId id, Runnable runnable) {
        this.maker().addArtifactFile(id, runnable);
        return this;
    }

    /**
     * Project will produces one artifact file for test binaries and one for test sources.
     */
    public JkJavaProject addTestArtifactFiles() {
        this.maker.addArtifactFile(TEST_FILE_ID, () -> this.maker.makeTestJar());
        this.maker.addArtifactFile(TEST_SOURCE_FILE_ID, () -> this.maker.getPackager().testSourceJar());
        return this;
    }

    /**
     * Convenient method.
     * Project will produces one artifact file for fat jar having the specified name.
     */
    public JkJavaProject addFatJarArtifactFile(String classifier) {
        this.maker().addArtifactFile(JkArtifactFileId.of(classifier, "jar"),
                () -> {maker.compileAndTestIfNeeded(); maker.getPackager().fatJar(classifier);});
        return this;
    }

    // artifact producers -----------------------------------------------------------

    @Override
    public final void makeArtifactFile(JkArtifactFileId artifactFileId) {
        maker.makeArtifactFile(artifactFileId);
    }

    @Override
    public File artifactFile(JkArtifactFileId artifactId) {
        return maker.getArtifactFile(artifactId);
    }

    @Override
    public final Iterable<JkArtifactFileId> artifactFileIds() {
        return this.maker.getArtifactFileIds();
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

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + this.baseDir.getName();
    }

    // ---------------------------- Getters / setters --------------------------------------------

    @Override
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
            this.outLayout = outLayout;
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

    public JkJavaProject setSourceEncoding(String encoding) {
        this.compileSpec = compileSpec.withEncoding(encoding);
        return this;
    }

    public JkJavaProject setSourceVersion(JkJavaVersion version ) {
        this.compileSpec = compileSpec.withSourceAndTargetVersion(version);
        return this;
    }

    public JkJavaProject setMaker(JkJavaProjectMaker maker) {
        this.maker = maker;
        return this;
    }

    public List<JkResourceProcessor.JkInterpolator> getResourceInterpolators() {
        return resourceInterpolators;
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

    public JkFileTreeSet getExtraFilesToIncludeInJar() {
        return extraFilesToIncludeInFatJar;
    }

    public JkJavaProject setExtraFilesToIncludeInFatJar(JkFileTreeSet extraFilesToIncludeInFatJar) {
        this.extraFilesToIncludeInFatJar = extraFilesToIncludeInFatJar;
        return this;
    }

    public JkMavenPublicationInfo getMavenPublicationInfo() {
        return mavenPublicationInfo;
    }

    public JkJavaProject setMavenPublicationInfo(JkMavenPublicationInfo mavenPublicationInfo) {
        this.mavenPublicationInfo = mavenPublicationInfo;
        return this;
    }

    public JkJavaProject removeArtifactFile(JkArtifactFileId ... artifactFileIds) {
        for (final JkArtifactFileId artifactFileId : artifactFileIds) {
            this.maker.removeArtifactFile(artifactFileId);
        }
        return this;
    }
}