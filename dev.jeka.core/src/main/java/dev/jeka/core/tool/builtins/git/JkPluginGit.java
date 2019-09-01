package dev.jeka.core.tool.builtins.git;

import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.tooling.JkGitWrapper;
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPlugin;

@JkDoc("Plugin providing common Git scripts/commands out of the box.")
public class JkPluginGit extends JkPlugin {

    private final JkGitWrapper git;

    protected JkPluginGit(JkCommands commands) {
        super(commands);
        git = JkGitWrapper.of(commands.getBaseDir());
    }

    @JkDoc("Perform a dirty check first then put a tag at the HEAD and push it to remote.")
    public void tagRemote() {
        if (!git.withLogOutput(false).withLogCommand(true).isRemoteEqual()) {
            System.out.println("The current tracking branch is not aligned with the remote. Please update/push and retry.");
            return;
        }
        if (git.isWorkspaceDirty()) {
            System.out.println("Git workspace is dirty. Please clean your Git workspace and retry");
            return;
        }
        System.out.println("Existing tags :");
        git.exec("tag");
        System.out.println("You are about to tag commit : " + git.getCurrentCommit());
        final String newTag = JkPrompt.ask("Enter new tag : ");
        git.withLogCommand(true).tagAndPush(newTag);
    }
}
