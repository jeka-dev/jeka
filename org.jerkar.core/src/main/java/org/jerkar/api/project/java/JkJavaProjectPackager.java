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

    public void mainJar(Path target) {
        JkJarMaker.jar(target, project.getManifest(), project.getOutLayout().classDir(),
                project.getExtraFilesToIncludeInJar());
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

    public void sourceJar(Path target) {
        project.getSourceLayout().sources().and(project.getOutLayout().generatedSourceDir()).zipTo(target);
    }

    public void javadocJar(Path target) {
        Path javadocDir = project.getOutLayout().getJavadocDir();
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + ". Can't create a javadoc jar until javadoc files has been generated.");
        }
        JkPathTree.of(javadocDir).zipTo(target);
    }

    public void testJar(Path target) {
        JkJarMaker.jar(target, project.getManifest(), project.getOutLayout().testClassDir(),  null);
    }

    public void testSourceJar(Path target) {
        project.getSourceLayout().tests().zipTo(target);
    }

}
