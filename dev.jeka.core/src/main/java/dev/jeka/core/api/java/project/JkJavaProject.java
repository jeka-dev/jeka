package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkArtifactProducer;
import dev.jeka.core.api.depmanagement.JkDependencyManagement;
import dev.jeka.core.api.file.JkFileSystemLocalizable;
import dev.jeka.core.tool.JkConstants;

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

    private JkProjectSourceLayout sourceLayout;

    private JkProjectOutLayout outLayout;

    private final JkJavaProjectMaker maker;

    private final JkDependencyManagement<JkJavaProject> dependencyManagement;

    private JkJavaProject(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        outLayout = JkProjectOutLayout.ofClassicJava().withOutputDir(getBaseDir().resolve(JkConstants.OUTPUT_PATH));
        maker = new JkJavaProjectMaker(this);
        dependencyManagement = JkDependencyManagement.of(this);
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


    // ---------------------------- Getters / setters --------------------------------------------

    @Override
    public Path getBaseDir() {
        return this.getSourceLayout().getBaseDir();
    }

    public JkProjectSourceLayout getSourceLayout() {
        return sourceLayout;
    }

    public JkProjectOutLayout getOutLayout() {
        return outLayout;
    }

    public JkJavaProjectMaker getMaker() {
        return maker;
    }

    public JkJavaProject setSourceLayout(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        return this;
    }

    public JkJavaProject setOutLayout(JkProjectOutLayout outLayout) {
        if (outLayout.getOutputPath().isAbsolute()) {
            this.outLayout = outLayout;
        } else {
            this.outLayout = outLayout.withOutputDir(getBaseDir().resolve(outLayout.getOutputPath()));
        }
        return this;
    }

    public JkDependencyManagement<JkJavaProject> getDependencyManagement() {
        return dependencyManagement;
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + this.sourceLayout.getBaseDir().getFileName();
    }

    @Override
    public JkArtifactProducer get() {
        return getMaker();
    }

    public String getInfo() {
        return new StringBuilder("Project Location : " + this.getBaseDir() + "\n")
                .append("Published Module & version : " + this.maker.getSteps().getPublishing().getVersionedModule() + "\n")
                .append(this.sourceLayout.getInfo()).append("\n")
                .append("Java Source Version : " + this.maker.getSteps().getCompilation().getComputedCompileSpec().getSourceVersion() + "\n")
                .append("Source Encoding : " + this.maker.getSteps().getCompilation().getComputedCompileSpec().getEncoding() + "\n")
                .append("Source file count : " + this.sourceLayout.getSources().count(Integer.MAX_VALUE, false) + "\n")
                .append("Download Repositories : " + this.dependencyManagement.getResolver().getRepos() + "\n")
                .append("Publish repositories : " + this.maker.getSteps().getPublishing().getPublishRepos()  + "\n")
                .append("Declared Dependencies : " + this.dependencyManagement.getDependencies().toList().size() + " elements.\n")
                .append("Defined Artifacts : " + this.get().getArtifactIds())
                .toString();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return JkJavaIdeSupport.ofDefault()
                .withDependencies(this.dependencyManagement.getDependencies())
                .withDependencyResolver(this.dependencyManagement.getResolver())
                .withSourceLayout(this.sourceLayout)
                .withSourceVersion(this.maker.getSteps().getCompilation().getComputedCompileSpec().getSourceVersion());
    }

}