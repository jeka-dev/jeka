package dev.jeka.core.tool.builtins.repos;

import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.util.Optional;

public class JkPluginNexus extends JkPlugin {

    @JkDoc("Close and Release automatically repository after publish.")
    public boolean closeAndRelease = true;

    @JkDoc("Comma separated filters for taking in account only specified repositories with specified profile name.")
    public String profileNamesFilter = "";

    protected JkPluginNexus(JkClass jkClass) {
        super(jkClass);
    }

    @Override
    protected void afterSetup() throws Exception {
        JkPluginJava pluginJava = getJkClass().getPlugins().getIfLoaded(JkPluginJava.class);
        String[] profileNames = profileNamesFilter.split(",");
        if (pluginJava != null) {
            configureForFirstRemoteRepo(pluginJava.getProject(), profileNames);
        }
    }

    public static void configureForOSSRHRepo(JkJavaProject project, String ...profileNames) {
        JkRepo repo = project.getPublication().getMaven().getRepos()
                .getRepoConfigHavingUrl(JkRepo.MAVEN_OSSRH_DEPLOY_RELEASE);
        configureForRepo(project, repo, profileNames);
    }

    public static void configureForFirstRemoteRepo(JkJavaProject project, String ...profileNames) {
        JkRepo repo = project.getPublication().getMaven().getRepos().getRepos().stream()
                .filter(repo1 -> !repo1.isLocal())
                .findFirst().orElse(null);
        configureForRepo(project, repo, profileNames);
    }

    public static void configureForRepo(JkJavaProject project, JkRepo repo, String ...profileNames) {
        if (repo == null) {
            JkLog.warn("No Nexus OSSRH repo found.");
            return;
        }
        JkNexusRepos nexusRepos = JkNexusRepos.ofUrlAndCredentials(repo);
        configureForRepo(project, nexusRepos, profileNames);
    }

    public static void configureForRepo(JkJavaProject project, JkNexusRepos nexusRepos, String ...profileNames) {
        project.getPublication().getPostActions().append("Closing and releasing repositories",
                () -> nexusRepos.closeAndRelease(profileNames));
    }

}
