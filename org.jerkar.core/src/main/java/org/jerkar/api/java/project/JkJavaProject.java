package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileSystemLocalizable;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.java.JkJavaCompileSpec;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.JkManifest;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.utils.JkUtilsAssert;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Container for a Java project with classic characteristic :
 * <ul>
 *     <li>Contains Java source files to be compiled</li>
 *     <li>All Java sources file (prod + test) are wrote against the same Java projectVersion and encoding</li>
 *     <li>JkEclipseProject may contain unit tests</li>
 *     <li>It can depends on any accepted dependencies (Maven module, other project, files on fs, ...)</li>
 *
 *     <li>It produces a bin jar, a source jar and a javadoc jar</li>
 *     <li>It can produce any other artifact files (fat-jar, test jar, doc, ...)</li>
 *     <li>It can be identified as a Maven module (means it can provide a group, artifact id, projectVersion) in order to be published/reused</li>
 *     <li>It can be published on any Maven/Ivy repository, including Maven central</li>
 *
 *     <li>Part of the sources/resources may be generated</li>
 *     <li>By default, passing test suite is required to produce artifact.</li>
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
public class JkJavaProject implements JkJavaProjectDefinition, JkFileSystemLocalizable, Supplier<JkArtifactProducer> {

    private JkVersionedModule versionedModule;

    private JkProjectSourceLayout sourceLayout;

    private JkDependencySet dependencies;

    private JkJavaCompileSpec compileSpec =
            new JkJavaCompileSpec().setSourceAndTargetVersion(JkJavaVersion.V8).setEncoding("UTF-8");

    private final List<JkResourceProcessor.JkInterpolator> resourceInterpolators = new LinkedList<>();

    private JkManifest manifest = JkManifest.ofEmpty();

    private JkPathTreeSet extraFilesToIncludeInFatJar = JkPathTreeSet.ofEmpty();

    private JkMavenPublicationInfo mavenPublicationInfo;

    private final JkJavaProjectMaker maker;

    private JkJavaProject(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        this.dependencies = JkDependencySet.of();
        this.versionedModule = JkVersionedModule.ofRootDirName(sourceLayout.baseDir().getFileName().toString());
        this.maker = new JkJavaProjectMaker(this);
    }

    public static JkJavaProject of(JkProjectSourceLayout layout) {
        return new JkJavaProject(layout);
    }

    public static JkJavaProject ofMavenLayout(Path baseDir) {
        return JkJavaProject.of(JkProjectSourceLayout.ofMavenStyle().withBaseDir(baseDir));
    }

    public static JkJavaProject ofSimpleLayout(Path baseDir) {
        return JkJavaProject.of(JkProjectSourceLayout.ofSimpleStyle().withBaseDir(baseDir));
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + this.sourceLayout.baseDir().getFileName();
    }

    // ---------------------------- Getters / setters --------------------------------------------

    @Override
    public Path baseDir() {
        return this.getSourceLayout().baseDir();
    }

    @Override
    public JkProjectSourceLayout getSourceLayout() {
        return sourceLayout;
    }

    @Override
    public JkDependencySet getDependencies() {
        return this.dependencies;
    }

    public JkJavaProjectMaker maker() {
        return maker;
    }

    public JkJavaProject setSourceLayout(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        return this;
    }

    public JkJavaProject setDependencies(JkDependencySet dependencies) {
        this.maker.cleanDepChache();
        this.dependencies = dependencies;
        return this;
    }

    public JkJavaProject addDependencies(JkDependencySet dependencies) {
        return this.setDependencies(this.dependencies.and(dependencies));
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

    /**
     * Returns the module name and version of this project. This information is used for naming produced artifact files,
     * publishing. It is also consumed by tools as SonarQube.
     */
    public JkVersionedModule getVersionedModule() {
        return versionedModule;
    }

    /**
     * Sets the specified module name and version for this project.
     * @see #getVersionedModule()
     */
    public JkJavaProject setVersionedModule(JkVersionedModule versionedModule) {
        JkUtilsAssert.notNull(versionedModule, "Can't set null value for versioned module.");
        this.versionedModule = versionedModule;
        return this;
    }

    /**
     * @see #setVersionedModule(JkVersionedModule)
     */
    public JkJavaProject setVersionedModule(String groupAndName, String version) {
        return setVersionedModule(JkModuleId.of(groupAndName).getVersion(version));
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

    @Override
    public JkArtifactProducer get() {
        return maker();
    }

    public String info() {
        return new StringBuilder("Project Location : " + this.baseDir() + "\n")
                .append("Published Module & version : " + this.versionedModule + "\n")
                .append(this.sourceLayout.info()).append("\n")
                .append("Java Source Version : " + this.getSourceVersion() + "\n")
                .append("Source Encoding : " + this.compileSpec.getEncoding() + "\n")
                .append("Download Repositories : " + this.maker.getDependencyResolver().getRepos() + "\n")
                .append("Publish repositories : " + this.maker.getPublishRepos()  + "\n")
                .append("Declared Dependencies : " + this.getDependencies().toList().size() + " elements.\n")
                .append("Defined Artifacts : " + this.get().getArtifactIds())
                .toString();
    }
}