package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkArtifactProducer;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.file.JkFileSystemLocalizable;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Container for a Java project with classic characteristic :
 * <ul>
 *     <li>Contains Java source files to be compiled</li>
 *     <li>All Java sources file (prod + test) are wrote against the same Java version and encoding</li>
 *     <li>JkJavaProject may contain unit tests</li>
 *     <li>It can depends on any accepted dependencies (Maven module, other project, files on fs, ...)</li>
 *
 *     <li>It produces a bin jar, a source jar and a javadoc jar</li>
 *     <li>It can produce any other artifact files (fat-jar, test jar, doc, ...)</li>
 *     <li>It can be identified as a Maven module (means it can provide a group, artifact id, version) in order to be published/reused</li>
 *     <li>It can be published on any Maven/Ivy repository, including Maven central</li>
 *
 *     <li>Part of the sources/resources may be generated</li>
 *     <li>By default, passing test suite is required to produce artifact.</li>
 * </ul>
 *
 * Beside, java projects are highly extensible so you can add build tasks or alter existing ones. This
 * is done using {@link #getMaker()} object. For example you can easily add test cover or SonarQube analysis.
 *
 * It provides cache mechanism in order compile or unit test phases are executed once when generating
 * several artifact files so be aware of clean it if you want to replay some tasks with different settings.
 *
 * @See JkJavaProjectMaker
 */
public class JkJavaProject implements JkJavaIdeSupportSupplier, JkFileSystemLocalizable, Supplier<JkArtifactProducer> {

    private JkVersionedModule versionedModule;

    private JkProjectSourceLayout sourceLayout;

    private JkDependencySet dependencies;

    private JkPathTreeSet extraFilesToIncludeInFatJar = JkPathTreeSet.ofEmpty();

    private final JkJavaProjectMaker maker;

    private JkJavaProject(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        this.dependencies = JkDependencySet.of();
        this.versionedModule = JkVersionedModule.ofRootDirName(sourceLayout.getBaseDir().getFileName().toString());
        this.maker = new JkJavaProjectMaker(this);
    }

    public static JkJavaProject of(JkProjectSourceLayout layout) {
        return new JkJavaProject(layout);
    }

    public static JkJavaProject ofMavenLayout(Path baseDir) {
        return JkJavaProject.of(JkProjectSourceLayout.ofMavenStyle().withBaseDir(baseDir));
    }

    public static JkJavaProject ofMavenLayout(String baseDir) {
        return ofMavenLayout(Paths.get(baseDir));
    }

    public static JkJavaProject ofSimpleLayout(Path baseDir) {
        return JkJavaProject.of(JkProjectSourceLayout.ofSimpleStyle().withBaseDir(baseDir));
    }

    public static JkJavaProject ofSimpleLayout(String baseDir) {
        return ofSimpleLayout(Paths.get(baseDir));
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + this.sourceLayout.getBaseDir().getFileName();
    }

    // ---------------------------- Getters / setters --------------------------------------------

    @Override
    public Path getBaseDir() {
        return this.getSourceLayout().getBaseDir();
    }

    public JkProjectSourceLayout getSourceLayout() {
        return sourceLayout;
    }

    public JkDependencySet getDependencies() {
        return this.dependencies;
    }

    public JkJavaProjectMaker getMaker() {
        return maker;
    }

    public JkJavaProject setSourceLayout(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        return this;
    }

    public JkJavaProject removeDependencies() {
        this.maker.cleanDependencyCache();
        this.dependencies = JkDependencySet.of();
        return this;
    }

    public JkJavaProject addDependencies(JkDependencySet dependencies) {
        this.maker.cleanDependencyCache();
        this.dependencies = this.dependencies.and(dependencies);
        return this;
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
        return setVersionedModule(JkModuleId.of(groupAndName).withVersion(version));
    }



    public JkPathTreeSet getExtraFilesToIncludeInJar() {
        return this.extraFilesToIncludeInFatJar;
    }

    /**
     * File trees specified here will be added to the fat jar.
     */
    public JkJavaProject setExtraFilesToIncludeInFatJar(JkPathTreeSet extraFilesToIncludeInFatJar) {
        this.extraFilesToIncludeInFatJar = extraFilesToIncludeInFatJar;
        return this;
    }

    @Override
    public JkArtifactProducer get() {
        return getMaker();
    }

    public String getInfo() {
        return new StringBuilder("Project Location : " + this.getBaseDir() + "\n")
                .append("Published Module & version : " + this.versionedModule + "\n")
                .append(this.sourceLayout.getInfo()).append("\n")
                .append("Java Source Version : " + this.maker.getSteps().getCompilation().getComputedCompileSpec().getSourceVersion() + "\n")
                .append("Source Encoding : " + this.maker.getSteps().getCompilation().getComputedCompileSpec().getEncoding() + "\n")
                .append("Source file count : " + this.sourceLayout.getSources().count(Integer.MAX_VALUE, false) + "\n")
                .append("Download Repositories : " + this.maker.getDependencyResolver().getRepos() + "\n")
                .append("Publish repositories : " + this.maker.getSteps().getPublishing().getPublishRepos()  + "\n")
                .append("Declared Dependencies : " + this.getDependencies().toList().size() + " elements.\n")
                .append("Defined Artifacts : " + this.get().getArtifactIds())
                .toString();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return JkJavaIdeSupport.ofDefault()
                .withDependencies(this.dependencies)
                .withDependencyResolver(this.maker.getDependencyResolver())
                .withSourceLayout(this.sourceLayout)
                .withSourceVersion(this.maker.getSteps().getCompilation().getComputedCompileSpec().getSourceVersion());
    }
}