package org.jerkar.api.java.project;


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

    public static JkJavaProjectPackager of(JkJavaProject project) {
        return new JkJavaProjectPackager(project);
    }

    private final JkJavaProject project;

    private JkJavaProjectPackager(JkJavaProject project) {
        this.project = project;
    }

    public void mainJar(Path target) {
        JkJarMaker.of(project.maker().getOutLayout().classDir())
                .withManifest(project.getManifest())
                .withExtraFiles(project.getExtraFilesToIncludeInJar())
                .makeJar(target);
    }

    /**
     * @param classifier Can be <code>null</code>, id so the fat jar will stands for the main artifact file.
     */
    public Path fatJar(String classifier) {
        JkClasspath classpath = JkClasspath.ofMany(project.maker().fetchRuntimeDependencies(project.maker().getMainArtifactId()));
        JkArtifactId artifactFileId = JkArtifactId.of(classifier, "jar");
        Path result = project.maker().getArtifactPath(artifactFileId);
        JkJarMaker.of( project.maker().getOutLayout().classDir())
                .withManifest(project.getManifest())
                .withExtraFiles(project.getExtraFilesToIncludeInJar())
                .makeFatJar(result, classpath);
        return result;
    }

    public void sourceJar(Path target) {
        project.getSourceLayout().sources().and(project.maker().getOutLayout().generatedSourceDir()).zipTo(target);
    }

    public void javadocJar(Path target) {
        Path javadocDir = project.maker().getOutLayout().getJavadocDir();
        if (!Files.exists(javadocDir)) {
            throw new IllegalStateException("No javadoc has not been generated in " + javadocDir.toAbsolutePath()
                    + ". Can't create a javadoc jar until javadoc files has been generated.");
        }
        JkPathTree.of(javadocDir).zipTo(target);
    }

    public void testJar(Path target) {
        JkJarMaker.of(project.maker().getOutLayout().testClassDir())
                .withManifest(project.getManifest())
                .makeJar(target);
    }

    public void testSourceJar(Path target) {
        project.getSourceLayout().tests().zipTo(target);
    }

}
