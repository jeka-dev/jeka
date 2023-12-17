package dev.jeka.core.tool.builtins.repos;

import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

import java.util.Optional;
import java.util.function.Consumer;

@JkDoc("Provides features to release Nexus repos (as OSSRH) after publication.")
public class NexusJkBean extends JkBean {

    @JkDoc("Comma separated filters for taking in account only repositories with specified profile names.")
    public String profileNamesFilter = "";

    private final JkConsumers<JkNexusRepos> nexusReposConfigurators = JkConsumers.of();

    protected NexusJkBean() {
        ProjectJkBean projectBean = getRuntime().getBeanOptional(ProjectJkBean.class).orElse(null);
        if (projectBean == null) {
            JkLog.warn("No project KBean present to configure repos for.");
        } else {
            projectBean.lazily(this::configureProject);
        }
    }

    @JkDoc("Closes and releases the nexus repositories used by project KBean to publish artifacts.")
    public void closeAndRelease() {
        Optional<ProjectJkBean> projectBean = getRuntime().getBeanOptional(ProjectJkBean.class);
        if (!projectBean.isPresent()) {
            JkLog.warn("No project plugin configured here.");
            return;
        }
        JkNexusRepos nexusRepos  = getJkNexusRepos(projectBean.get().getProject());
        if (nexusRepos == null) {
            return;
        }
        nexusRepos.closeAndRelease();
    }

    /**
     * Adds a JkNexusRepos consumer that will be executed just in time.
     */
    public NexusJkBean lazily(Consumer<JkNexusRepos> nexusReposConfigurator) {
        this.nexusReposConfigurators.append(nexusReposConfigurator);
        return this;
    }

    /**
     * Use {@link #lazily(Consumer)} instead
     */
    @Deprecated
    public NexusJkBean lately(Consumer<JkNexusRepos> nexusReposConfigurator) {
        return this.lazily(nexusReposConfigurator);
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
