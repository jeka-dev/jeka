package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.java.JkJavaCompileSpec;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.project.JkProjectOutLayout;
import org.jerkar.api.project.JkProjectSourceLayout;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Container for a Java project with classic characteristic :
 * <ul>
 *     <li>Contains Java source files to be compiled</li>
 *     <li>All Java sources file (prod + test) are wriiten against the same Java version and encoding</li>
 *     <li>JkEclipseProject may contain unit tests</li>
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

    public static final JkArtifactFileId SOURCES_FILE_ID = JkArtifactFileId.of("sources", "jar");

    public static final JkArtifactFileId JAVADOC_FILE_ID = JkArtifactFileId.of("javadoc", "jar");

    public static final JkArtifactFileId TEST_FILE_ID = JkArtifactFileId.of("test", "jar");

    public static final JkArtifactFileId TEST_SOURCE_FILE_ID = JkArtifactFileId.of("test-sources", "jar");

    private final Path baseDir;

    // A project has either a name either a versioned module.
    private String artifactName;

    // A project has either a name either a versioned module.
    private JkVersionedModule versionedModule;

    private JkProjectSourceLayout sourceLayout;

    private JkProjectOutLayout outLayout;

    private JkDependencies dependencies;

    private JkJavaCompileSpec compileSpec =
            new JkJavaCompileSpec().setSourceAndTargetVersion(JkJavaVersion.V8).setEncoding("UTF-8");

    private final List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<>();

    private JkManifest manifest = JkManifest.empty();

    private JkPathTreeSet extraFilesToIncludeInFatJar = JkPathTreeSet.empty();

    private JkMavenPublicationInfo mavenPublicationInfo;

    private final JkJavaProjectMaker maker = new JkJavaProjectMaker(this);

    public JkJavaProject(Path baseDir) {
        baseDir = baseDir.toAbsolutePath().normalize();
        this.baseDir = baseDir;
        this.artifactName = this.baseDir.getFileName().toString();
        this.sourceLayout = JkProjectSourceLayout.mavenJava().withBaseDir(baseDir);
        this.outLayout = JkProjectOutLayout.classicJava().withOutputDir(baseDir.resolve("build/output"));
        this.dependencies = JkDependencies.ofLocalScoped(baseDir.resolve("build/libs").toFile());
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
     * JkEclipseProject will produces one artifact file for test binaries and one for test sources.
     */
    public JkJavaProject addTestArtifactFiles() {
        this.maker.addArtifactFile(TEST_FILE_ID, () -> this.maker.makeTestJar());
        this.maker.addArtifactFile(TEST_SOURCE_FILE_ID, () -> this.maker.getPackager().testSourceJar());
        return this;
    }

    /**
     * Convenient method.
     * JkEclipseProject will produces one artifact file for fat jar having the specified name.
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
    public Path artifactPath(JkArtifactFileId artifactId) {
        return maker.getArtifactFile(artifactId);
    }

    @Override
    public final Iterable<JkArtifactFileId> artifactFileIds() {
        return this.maker.getArtifactFileIds();
    }

    @Override
    public JkPathSequence runtimeDependencies(JkArtifactFileId artifactFileId) {
        if (artifactFileId.equals(mainArtifactFileId())) {
            return this.maker.getDependencyResolver().get(
                    this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.RUNTIME);
        } else if (artifactFileId.isClassifier("test") && artifactFileId.isExtension("jar")) {
            return this.maker.getDependencyResolver().get(
                    this.dependencies.withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.SCOPES_FOR_TEST);
        } else {
            return JkPathSequence.of();
        }
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + this.baseDir.getFileName();
    }

    // ---------------------------- Getters / setters --------------------------------------------

    @Override
    public Path baseDir() {
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
        if (outLayout.outputPath().isAbsolute()) {
            this.outLayout = outLayout;
        } else {
            this.outLayout = outLayout.withOutputDir(this.baseDir.resolve(outLayout.outputPath()));
        }
        return this;
    }

    public JkJavaProject setDependencies(JkDependencies dependencies) {
        this.maker.cleanDepChache();
        this.dependencies = dependencies;
        return this;
    }

    public JkJavaProject setSourceEncoding(String encoding) {
        this.compileSpec.setEncoding(encoding);
        return this;
    }

    public JkJavaProject setSourceVersion(JkJavaVersion version ) {
        compileSpec.setSourceAndTargetVersion(version);
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

    public JkJavaCompileSpec getCompileSpec() {
        return compileSpec;
    }

    public JkJavaProject setCompileSpec(JkJavaCompileSpec compileSpec) {
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

    public JkPathTreeSet getExtraFilesToIncludeInJar() {
        return extraFilesToIncludeInFatJar;
    }

    public JkJavaProject setExtraFilesToIncludeInFatJar(JkPathTreeSet extraFilesToIncludeInFatJar) {
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