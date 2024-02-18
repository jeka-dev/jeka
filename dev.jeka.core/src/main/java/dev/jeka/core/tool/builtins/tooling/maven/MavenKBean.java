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
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.util.Optional;

@JkDoc("Manages Maven publication for project and 'jeka-src'")
public final class MavenKBean extends KBean {

    private JkMavenPublication mavenPublication;

    @JkDoc("Indentation size for 'showPomDeps' output.")
    public int codeIndent = 4;

    @JkDoc("Displays Maven publication information on the console.")
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

        // If ProjectKBean is absent, try to configure wih BaseKBean if present
        if (!optionalProjectKBean.isPresent()) {
            getRunbase().findInstanceOf(BaseKBean.class).ifPresent(selfAppKBean -> {
                mavenPublication = createMavenPublication(selfAppKBean);
            });
        }

        if (mavenPublication == null) {
            throw new IllegalStateException("No ProjectKBean of BaseKBean found on runbase " + getBaseDir() + ". " +
                    "The MavenPublication KBean can't be configurated.");
        }

        // Add Publish Repos from JKProperties
        mavenPublication.setRepos(getPublishReposFromProps());
        return mavenPublication;
    }

    /**
     * Creates a Maven Publication based on the specified BaseKBean.
     */
    public static JkMavenPublication createMavenPublication(BaseKBean baseKBean) {
        JkArtifactLocator artifactLocator = JkArtifactLocator.of(baseKBean.getBaseDir(),
                baseKBean.getJarPathBaseName());
        return JkMavenPublication.of(artifactLocator)
                .setModuleIdSupplier(baseKBean::getModuleId)
                .setVersionSupplier(baseKBean::getVersion)
                .customizeDependencies(deps -> JkMavenPublication.computeMavenPublishDependencies(
                        baseKBean.getRunbase().getExportedDependencies(),
                        baseKBean.getRunbase().getExportedDependencies(),
                        JkCoordinate.ConflictStrategy.TAKE_FIRST))
                .setBomResolutionRepos(baseKBean.getRunbase().getDependencyResolver()::getRepos)
                .putArtifact(JkArtifactId.MAIN_JAR_ARTIFACT_ID)
                .putArtifact(JkArtifactId.SOURCES_ARTIFACT_ID, baseKBean::createSourceJar)
                .putArtifact(JkArtifactId.JAVADOC_ARTIFACT_ID, baseKBean::createJavadocJar);
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
