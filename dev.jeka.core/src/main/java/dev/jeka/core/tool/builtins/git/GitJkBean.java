package dev.jeka.core.tool.builtins.git;

import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;

@JkDoc("Provides common Git scripts/commands out of the box.")
public class GitJkBean extends JkBean {

    private final JkGitProcess git;

    protected GitJkBean() {
        git = JkGitProcess.of(getBaseDir());
    }

    @JkDoc("Perform a dirty check first then put a tag at the HEAD and push it to remote.")
    public void tagRemote() {
        JkGitProcess aGit = git.clone().setLogCommand(false).setLogOutput(false);
        System.out.println("Existing tags on origin :");
        aGit.clone().setLogOutput(true).exec("ls-remote", "--tag", "--sort=creatordate", "origin");
        if (aGit.isWorkspaceDirty()) {
            System.out.println("Git workspace is dirty. Please clean your Git workspace and retry");
            return;
        }
        if (!aGit.isRemoteEqual()) {
            System.out.println("The current tracking branch is not aligned with the remote. Please update/push and retry.");
            return;
        }
        System.out.println("You are about to tag commit : " + git.getCurrentCommit());
        final String newTag = JkPrompt.ask("Enter new tag : ");
        aGit.setLogCommand(true).tagAndPush(newTag);
    }

    public JkGitProcess getGitProcess() {
        return git;
    }


}
