package org.jerkar.api.project.java;


import java.nio.file.Files;
import java.nio.file.Path;

import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJarMaker;

/**
 * Creates jar and others elements ofMany a java project.
 */
public final class JkJavaProjectPackager {

    public static final JkJavaProjectPackager of(JkJavaProject project) {
        return new JkJavaProjectPackager(project);
    }

    private final JkJavaProject project;

    private JkJavaProjectPackager(JkJavaProject project) {
        this.project = project;
    }

    public Path mainJar() {
        Path result = project.mainArtifactPath();
        JkJarMaker.jar(result, project.getManifest(), project.getOutLayout().classDir(), project.getExtraFilesToIncludeInJar());
        return result;
    }

    /**
     * @param classifier Can be <code>null</code>, id so the fat jar will stands for the main artifact file.
     */
    public Path fatJar(String classifier) {
        JkClasspath classpath = JkClasspath.ofMany(project.runtimeDependencies(project.mainArtifactFileId()));
        JkArtifactFileId artifactFileId = JkArtifactFileId.of(classifier, "jar");
        Path result = project.artifactPath(artifactFileId);
        JkJarMaker.fatJar(result, project.getManifest(), project.getOutLayout().classDir(),
                project.getExtraFilesToIncludeInJar(), classpath);
        return result;
    }

    public Path sourceJar() {
        Path result = project.artifactPath(JkJavaProject.SOURCES_FILE_ID);
        project.getSourceLayout().sources().and(project.getOutLayout().generatedSourceDir()).zipTo(result);
        return result;
    }

    public Path javadocJar() {
        Path javadocDir = project.getOutLayout().getJavadocDir();
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + " : can't create javadoc jar. Please, generate Javadoc prior to package it in jar.");
        }
        Path result = project.artifactPath(JkJavaProject.JAVADOC_FILE_ID);
        JkFileTree.of(javadocDir).zipTo(result);
        return  result;
    }

    public Path testJar() {
        Path result = project.artifactPath(JkJavaProject.TEST_FILE_ID);
        JkJarMaker.jar(result, project.getManifest(), project.getOutLayout().testClassDir(),  null);
        return result;
    }

    public Path testSourceJar() {
        Path result = project.artifactPath(JkJavaProject.SOURCES_FILE_ID);
        project.getSourceLayout().tests().zipTo(result);
        return result;
    }

}
