package dev.jeka.core.api.tooling.maven;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.project.JkProject;

public class JkMavenPublications {

    /**
     * Creates a JkMavenPublication for the specified JkProject.
     */
    public static JkMavenPublication of(JkProject project) {
        return JkMavenPublication.of(project.artifactLocator)
                .setModuleIdSupplier(project::getModuleId)
                .setVersionSupplier(project::getVersion)
                .configureDependencies(deps -> JkMavenPublication.computeMavenPublishDependencies(
                        project.compilation.getDependencies(),
                        project.packaging.getRuntimeDependencies(),
                        project.getDuplicateConflictStrategy()))
                .setBomResolutionRepos(project.dependencyResolver::getRepos)
                .putArtifact(JkArtifactId.MAIN_JAR_ARTIFACT_ID)
                .putArtifact(JkArtifactId.SOURCES_ARTIFACT_ID, project.packaging::createSourceJar)
                .putArtifact(JkArtifactId.JAVADOC_ARTIFACT_ID, project.packaging::createJavadocJar);
    }


}
