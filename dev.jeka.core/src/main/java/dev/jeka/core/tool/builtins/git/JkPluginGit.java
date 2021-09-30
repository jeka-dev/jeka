package dev.jeka.core.tool.builtins.git;

import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPlugin;

@JkDoc("Plugin providing common Git scripts/commands out of the box.")
public class JkPluginGit extends JkPlugin {

    private final JkGitProcess git;

    protected JkPluginGit(JkClass jkClass) {
        super(jkClass);
        git = JkGitProcess.of(jkClass.getBaseDir());
    }

    @JkDoc("Perform a dirty check first then put a tag at the HEAD and push it to remote.")
    public void tagRemote() {
        if (!git.setLogOutput(false).setLogCommand(true).isRemoteEqual()) {
            System.out.println("The current tracking branch is not aligned with the remote. Please update/push and retry.");
            return;
        }
        if (git.isWorkspaceDirty()) {
           System.out.println("Git workspace is dirty. Please clean your Git workspace and retry");
            //return;
        }
        System.out.println("Existing tags on origin :");
        git.addParams("ls-remote", "--tag", "--sort=creatordate", "origin").exec();
        System.out.println("You are about to tag commit : " + git.getCurrentCommit());
        final String newTag = JkPrompt.ask("Enter new tag : ");
        git.setLogCommand(true).tagAndPush(newTag);
    }

    public JkGitProcess getWrapper() {
        return git;
    }


}
