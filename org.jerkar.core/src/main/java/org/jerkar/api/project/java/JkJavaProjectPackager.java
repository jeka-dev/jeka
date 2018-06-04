package org.jerkar.api.project.java;


import java.nio.file.Files;
import java.nio.file.Path;

import org.jerkar.api.depmanagement.JkArtifactId;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJarMaker;

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

    public Path mainJar() {
        Path result = project.maker().mainArtifactPath();
        JkJarMaker.jar(result, project.getManifest(), project.getOutLayout().classDir(), project.getExtraFilesToIncludeInJar());
        return result;
    }

    /**
     * @param classifier Can be <code>null</code>, id so the fat jar will stands for the main artifact file.
     */
    public Path fatJar(String classifier) {
        JkClasspath classpath = JkClasspath.ofMany(project.maker().runtimeDependencies(project.maker().mainArtifactId()));
        JkArtifactId artifactFileId = JkArtifactId.of(classifier, "jar");
        Path result = project.maker().artifactPath(artifactFileId);
        JkJarMaker.fatJar(result, project.getManifest(), project.getOutLayout().classDir(),
                project.getExtraFilesToIncludeInJar(), classpath);
        return result;
    }

    public Path sourceJar() {
        Path result = project.maker().artifactPath(JkJavaProjectMaker.SOURCES_ARTIFACT_ID);
        project.getSourceLayout().sources().and(project.getOutLayout().generatedSourceDir()).zipTo(result);
        return result;
    }

    public Path javadocJar() {
        Path javadocDir = project.getOutLayout().getJavadocDir();
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + ". Can't create a javadoc jar until javadoc files has been generated.");
        }
        Path result = project.maker().artifactPath(JkJavaProjectMaker.JAVADOC_ARTIFACT_ID);
        JkPathTree.of(javadocDir).zipTo(result);
        return  result;
    }

    public Path testJar() {
        Path result = project.maker().artifactPath(JkJavaProjectMaker.TEST_ARTIFACT_ID);
        JkJarMaker.jar(result, project.getManifest(), project.getOutLayout().testClassDir(),  null);
        return result;
    }

    public Path testSourceJar() {
        Path result = project.maker().artifactPath(JkJavaProjectMaker.SOURCES_ARTIFACT_ID);
        project.getSourceLayout().tests().zipTo(result);
        return result;
    }

}
