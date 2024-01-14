package dev.jeka.core.tool.builtins.tooling.maven;

import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.maven.JkMavenPublications;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.util.Optional;

@JkDoc("Provides a Maven Publication according ProjectKBean or SefApp found in the JkRuntime.")
public class MavenPublicationKBean extends KBean {

    private JkMavenPublication mavenPublication;

    @Override
    protected void init() {
        Optional<ProjectKBean> optionalProjectKBean = getRuntime().find(ProjectKBean.class);
        optionalProjectKBean.ifPresent(
                projectKBean -> mavenPublication = JkMavenPublications.of(projectKBean.project));
    }

    @JkDoc("Display Maven Publication information on the console.")
    public void info() {
        JkLog.info(mavenPublication.info());
    }

    @JkDoc("Publishes the Maven publication on the repositories specified inside this publication.")
    public void publish() {
        mavenPublication.publish();
    }

    @JkDoc("Publishes the Maven publication on the local JeKa repository.")
    public void publishLocal() {
        mavenPublication.publishLocal();
    }

    @JkDoc("Publishes the Maven publication on the local M2 repository. This is the local repository of Maven.")
    public void publishLocalM2() {
        mavenPublication.publishLocalM2();
    }

    /**
     * Returns the Maven Publication associated with this KBean
     */
    public JkMavenPublication getMavenPublication() {
        return mavenPublication;
    }

}
