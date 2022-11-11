package dev.jeka.core.tool.builtins.repos;

import dev.jeka.core.api.depmanagement.JkRepo;
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

    private static final String TASK_NAME = "Closing and releasing repositories";

    @JkDoc("Comma separated filters for taking in account only repositories with specified profile names.")
    public String profileNamesFilter = "";

    private final JkConsumers<JkNexusRepos, Void> nexusReposConfigurators = JkConsumers.of();

    protected NexusJkBean() {
        ProjectJkBean projectBean = getRuntime().getBeanOptional(ProjectJkBean.class).orElse(null);
        if (projectBean == null) {
            JkLog.warn("No project KBean present to configure repos for.");
        } else {
            JkProject project = projectBean.getProject();
            projectBean.configure(this::configureProject);
        }
    }

    @JkDoc("Closes and releases the nexus repositories used by project KBean to publish artifacts.")
    public void closeAndRelease() {
        JkNexusRepos nexusRepos  = getJkNexusRepos();
        if (nexusRepos == null) {
            return;
        }
        nexusRepos.closeAndRelease(profiles());
    }

    public NexusJkBean configure(Consumer<JkNexusRepos> nexusReposConsumer) {
        this.nexusReposConfigurators.append(nexusReposConsumer);
        return this;
    }

    private void configureProject(JkProject project) {
        JkNexusRepos nexusRepos  = getJkNexusRepos();
        if (nexusRepos == null) {
            return;
        }
        project.publication.postActions.append(TASK_NAME, () -> {
            nexusRepos.closeAndReleaseOpenRepositories(profiles());
        });
    }

    private String[] profiles() {
        return JkUtilsString.isBlank(profileNamesFilter) ? new String[0] : profileNamesFilter.split(",");
    }

    private JkNexusRepos getJkNexusRepos() {
        Optional<ProjectJkBean> projectBean = getRuntime().getBeanOptional(ProjectJkBean.class);
        if (!projectBean.isPresent()) {
            JkLog.warn("No project plugin configured here.");
            return null;
        }
        JkProject project = projectBean.get().getProject();
        JkRepo repo = project.publication.findFirstNonLocalRepo();
        if (repo == null) {
            JkLog.warn("No remote repository configured for publishing");
            return null;
        }
        if (repo.getCredentials() == null || repo.getCredentials().isEmpty()) {
            JkLog.warn("No credentials found on repo " + repo);
        }
        JkNexusRepos result = JkNexusRepos.ofRepo(repo);
        this.nexusReposConfigurators.accept(result);
        return result;
    }

}
