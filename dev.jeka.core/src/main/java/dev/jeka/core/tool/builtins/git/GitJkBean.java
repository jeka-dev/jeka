package dev.jeka.core.tool.builtins.git;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.tooling.JkGit;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

@JkDoc({"Manages versioning of project (coming from ProjectJkBean) by extracting Git information.",
        "The version is inferred from git using following logic : ",
        "  - If git workspace is dirty (different than last commit), version values [branch]-SNAPSHOT",
        "  - If last commit contains a message containing [commentKeyword]xxxxx, version values xxxxx",
        "  - If last commit is tagged, version values [last tag on last commit]",
        "The inferred version can be  applied to project.publication.maven.version and project.publication.ivy.publication, " +
                "programmatically using 'handleVersioning' method."
})
public class GitJkBean extends JkBean {

    @JkDoc("Some like to tag versions using a prefix (e.g. using 'v1.3.1' for tagging version '1.3.1'. In this case " +
            "you can set this value to 'v' or whatever prefix.")
    public String versionTagPrefix = "";

    private final JkGit git;

    private transient JkVersionFromGit versionFromGit;

    protected GitJkBean() {
        git = JkGit.of(getBaseDir());
    }

    @JkDoc("Performs a dirty check first, put a tag at the HEAD and push it to remote." +
            " The user will be prompted to enter the tag name.")
    public void tagRemote() {
        JkGit aGit = git.copy().setLogCommand(false).setLogOutput(false);
        JkLog.info("Existing tags on origin :");
        aGit.copy().setLogOutput(true).exec("ls-remote", "--tag", "--sort=creatordate", "origin");
        if (aGit.isWorkspaceDirty()) {
            JkLog.info("Git workspace is dirty. Please clean your Git workspace and retry");
            return;
        }
        if (!aGit.isRemoteEqual()) {
            JkLog.info("The current tracking branch is not aligned with the remote. Please update/push and retry.");
            return;
        }
        JkLog.info("You are about to tag commit : " + git.getCurrentCommit());
        final String newTag = JkPrompt.ask("Enter new tag : ");
        aGit.setLogCommand(true).tagAndPush(newTag);
    }

    @JkDoc("Display version supplied to the project.")
    public void showVersion() {
        JkLog.info(versionFromGit().version());
    }

    @JkDoc("Handle versioning of the project managed in the projectKBean. " +
            "It is meant to be called from the property file cmd, prior other project#xxxxx commands. ")
    public void handleProjectVersioning() {
        getBean(ProjectJkBean.class).lately(project -> versionFromGit().handleVersioning(project));
    }

    /**
     * Gets the current version either from commit message if specified nor from git tag.
     */
    public String version() {
        return versionFromGit().version();
    }

    private JkVersionFromGit versionFromGit() {
        if (versionFromGit == null) {
            versionFromGit = JkVersionFromGit.of(getBaseDir(), versionTagPrefix);
        }
        return versionFromGit;
    }

}
