package dev.jeka.core.tool.builtins.tooling.maven;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.project.JkProjectPublications;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.maven.JkMavenProject;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.self.SelfKBean;

import java.util.Optional;

@JkDoc("Manages Maven publication for project and 'jeka-src'")
public final class MavenKBean extends KBean {

    private JkMavenPublication mavenPublication;

    @JkDoc("whitespace count to indentSpri dependency code.")
    public int codeIndent = 4;

    @JkDoc("Display Maven Publication information on the console.")
    public void info() {
        JkLog.info(getMavenPublication().info());
    }

    @JkDoc("Publishes the Maven publication on the repositories specified inside this publication.")
    public void publish() {
        getMavenPublication().publish();
    }

    @JkDoc("Publishes the Maven publication on the local JeKa repository.")
    public void publishLocal() {
        getMavenPublication().publishLocal();
    }

    @JkDoc("Publishes the Maven publication on the local M2 repository. This is the local repository of Maven.")
    public void publishLocalM2() {
        getMavenPublication().publishLocalM2();
    }

    @JkDoc("Displays Java code for declaring dependencies based on pom.xml. The pom.xml file is supposed to be in root directory.")
    public void showPomDeps()  {
        JkLog.info(JkMavenProject.of(getBaseDir()).getDependencyAsJeKaCode(codeIndent));
        JkLog.info(JkMavenProject.of(getBaseDir()).getDependenciesAsTxt());
    }

    /**
     * Returns the Maven Publication associated with this KBean
     */
    public JkMavenPublication getMavenPublication() {

        // maven Can't be instantiated in init(), cause it will fail if there is no project or self kbean,
        // that may happen when doing a 'showPomDeps'.

        if (mavenPublication != null) {
            return mavenPublication;
        }
        // Configure with ProjectKBean if present
        Optional<ProjectKBean> optionalProjectKBean = getRunbase().find(ProjectKBean.class);
        if (optionalProjectKBean.isPresent()) {
            mavenPublication = JkProjectPublications.mavenPublication(optionalProjectKBean.get().project);
        }
        optionalProjectKBean.ifPresent(
                projectKBean -> mavenPublication = JkProjectPublications.mavenPublication(projectKBean.project));

        // If ProjectKBean is absent, try to configure wih SelfKBean if present
        if (!optionalProjectKBean.isPresent()) {
            getRunbase().findInstanceOf(SelfKBean.class).ifPresent(selfAppKBean -> {
                mavenPublication = createMavenPublication(selfAppKBean);
            });
        }

        if (mavenPublication == null) {
            throw new IllegalStateException("No ProjectKBean of SelfKBean found on runbase " + getBaseDir() + ". " +
                    "The MavenPublication KBean can't be configurated.");
        }

        // Add Publish Repos from JKProperties
        mavenPublication.setRepos(getPublishReposFromProps());
        return mavenPublication;
    }

    /**
     * Creates a Maven Publication based on the specified SelfKBean.
     */
    public static JkMavenPublication createMavenPublication(SelfKBean selfKBean) {
        JkArtifactLocator artifactLocator = JkArtifactLocator.of(selfKBean.getBaseDir(),
                selfKBean.getJarPathBaseName());
        return JkMavenPublication.of(artifactLocator)
                .setModuleIdSupplier(selfKBean::getModuleId)
                .setVersionSupplier(selfKBean::getVersion)
                .customizeDependencies(deps -> JkMavenPublication.computeMavenPublishDependencies(
                        selfKBean.getRunbase().getExportedDependencies(),
                        selfKBean.getRunbase().getExportedDependencies(),
                        JkCoordinate.ConflictStrategy.TAKE_FIRST))
                .setBomResolutionRepos(selfKBean.getRunbase().getDependencyResolver()::getRepos)
                .putArtifact(JkArtifactId.MAIN_JAR_ARTIFACT_ID)
                .putArtifact(JkArtifactId.SOURCES_ARTIFACT_ID, selfKBean::createSourceJar)
                .putArtifact(JkArtifactId.JAVADOC_ARTIFACT_ID, selfKBean::createJavadocJar);
    }

    private JkRepoSet getPublishReposFromProps() {
        JkRepoProperties repoProperties = JkRepoProperties.of(this.getRunbase().getProperties());
        JkRepoSet result = repoProperties.getPublishRepository();
        if (result.getRepos().isEmpty()) {
            result = result.and(JkRepo.ofLocal());
        }
        return result;
    }

}
