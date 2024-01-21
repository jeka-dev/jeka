package dev.jeka.core.tool.builtins.tooling.nexus;

import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenPublicationKBean;

import java.util.function.Consumer;

@JkDoc("Provides features to release Nexus repos (as OSSRH) after publication.")
public class NexusKBean extends KBean {

    @JkDoc("Comma separated filters for taking in account only repositories with specified profile names.")
    public String profileNamesFilter = "";

    private final JkConsumers<JkNexusRepos> nexusReposConfigurators = JkConsumers.of();

    @Override
    protected void init() {
        MavenPublicationKBean mavenPublicationKBean = getRunbase().find(MavenPublicationKBean.class).orElse(null);
        if (mavenPublicationKBean == null) {
            JkLog.trace("Nexus KBean cannot find MavenPublication KBean in runbase. Can't configure any repo.");
        } else {
            configureMavenPublication(mavenPublicationKBean.getMavenPublication());
        }
    }

    @JkDoc("Closes and releases the nexus repositories used by project KBean to publish artifacts.")
    public void closeAndRelease() {
        MavenPublicationKBean mavenPublicationKBean = getRunbase().find(MavenPublicationKBean.class).orElse(null);
        if (mavenPublicationKBean == null) {
            JkLog.error("No MavenPublicationKBean found in runbase %s.", getBaseDir());
            return;
        }
        JkNexusRepos nexusRepos  = getJkNexusRepos(mavenPublicationKBean.getMavenPublication());
        if (nexusRepos == null) {
            return;
        }
        nexusRepos.closeAndRelease();
    }

    /**
     * Adds a JkNexusRepos consumer that will be executed just in time.
     */
    public NexusKBean configureNexusRepo(Consumer<JkNexusRepos> nexusReposConfigurator) {
        this.nexusReposConfigurators.add(nexusReposConfigurator);
        return this;
    }

    private void configureMavenPublication(JkMavenPublication mavenPublication) {
        JkNexusRepos nexusRepos  = getJkNexusRepos(mavenPublication);
        nexusRepos.autoReleaseAfterPublication(mavenPublication);
    }

    private String[] profiles() {
        return JkUtilsString.isBlank(profileNamesFilter) ? new String[0] : profileNamesFilter.split(",");
    }

    private JkNexusRepos getJkNexusRepos(JkMavenPublication mavenPublication) {
        JkNexusRepos result = JkNexusRepos.ofPublishRepo(mavenPublication).setProfileNameFilters(profiles());
        this.nexusReposConfigurators.accept(result);
        return result;
    }

}
