package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.file.JkZipper;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJarMaker;

import java.io.File;

/**
 * Creates jar and others elements of a java project.
 */
public final class JkJavaProjectPackager {

    public static final JkJavaProjectPackager of(JkJavaProject project) {
        return new JkJavaProjectPackager(project);
    }

    private final JkJavaProject project;

    private JkJavaProjectPackager(JkJavaProject project) {
        this.project = project;
    }

    public File mainJar() {
        File result = project.mainArtifactFile();
        JkJarMaker.jar(result, project.getManifest(), project.getOutLayout().classDir(), project.getExtraFilesToIncludeInJar());
        return result;
    }

    /**
     * @param classifier Can be <code>null</code>, id so the fat jar will stands for the main artifact file.
     */
    public File fatJar(String classifier) {
        JkClasspath classpath = JkClasspath.of(project.runtimeDependencies(project.mainArtifactFileId()));
        JkArtifactFileId artifactFileId = JkArtifactFileId.of(classifier, "jar");
        File result = project.artifactFile(artifactFileId);
        JkJarMaker.fatJar(result, project.getManifest(), project.getOutLayout().classDir(),
                project.getExtraFilesToIncludeInJar(), classpath);
        return result;
    }

    public File sourceJar() {
        File result = project.artifactFile(JkJavaProject.SOURCES_FILE_ID);
        project.getSourceLayout().sources().and(project.getOutLayout().generatedSourceDir()).zip().to(result);
        return result;
    }

    public File javadocJar() {
        File result = project.artifactFile(JkJavaProject.JAVADOC_FILE_ID);
        JkZipper.of(project.getOutLayout().getJavadocDir()).to(result);
        return  result;
    }

    public File testJar() {
        File result = project.artifactFile(JkJavaProject.TEST_FILE_ID);
        JkJarMaker.jar(result, project.getManifest(), project.getOutLayout().testClassDir(),  null);
        return result;
    }

    public File testSourceJar() {
        File result = project.artifactFile(JkJavaProject.SOURCES_FILE_ID);
        project.getSourceLayout().tests().zip().to(result);
        return result;
    }

}
