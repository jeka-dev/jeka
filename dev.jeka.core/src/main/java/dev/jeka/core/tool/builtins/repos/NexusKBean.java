package dev.jeka.core.tool.builtins.repos;

import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.util.Optional;
import java.util.function.Consumer;

@JkDoc("Provides features to release Nexus repos (as OSSRH) after publication.")
public class NexusKBean extends KBean {

    @JkDoc("Comma separated filters for taking in account only repositories with specified profile names.")
    public String profileNamesFilter = "";

    private final JkConsumers<JkNexusRepos> nexusReposConfigurators = JkConsumers.of();

    protected void init() {
        ProjectKBean projectKBean = getRuntime().find(ProjectKBean.class).orElse(null);
        if (projectKBean == null) {
            JkLog.warn("No project KBean present to configure repos for.");
        } else {
            configureProject(projectKBean.project);
        }
    }

    @JkDoc("Closes and releases the nexus repositories used by project KBean to publish artifacts.")
    public void closeAndRelease() {
        Optional<ProjectKBean> projectKBean = getRuntime().find(ProjectKBean.class);
        if (!projectKBean.isPresent()) {
            JkLog.warn("No project plugin configured here.");
            return;
        }
        JkNexusRepos nexusRepos  = getJkNexusRepos(projectKBean.get().project);
        if (nexusRepos == null) {
            return;
        }
        nexusRepos.closeAndRelease();
    }

    /**
     * Adds a JkNexusRepos consumer that will be executed just in time.
     */
    public NexusKBean lazily(Consumer<JkNexusRepos> nexusReposConfigurator) {
        this.nexusReposConfigurators.append(nexusReposConfigurator);
        return this;
    }

    private void configureProject(JkProject project) {
        JkNexusRepos nexusRepos  = getJkNexusRepos(project);
        nexusRepos.autoReleaseAfterPublication(project);
    }

    private String[] profiles() {
        return JkUtilsString.isBlank(profileNamesFilter) ? new String[0] : profileNamesFilter.split(",");
    }

    private JkNexusRepos getJkNexusRepos(JkProject project) {
        JkNexusRepos result = JkNexusRepos.ofPublishRepo(project).setProfileNameFilters(profiles());
        this.nexusReposConfigurators.accept(result);
        return result;
    }

}
