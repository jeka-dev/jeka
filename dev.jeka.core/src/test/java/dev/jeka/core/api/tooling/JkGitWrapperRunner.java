package dev.jeka.core.api.tooling;

import dev.jeka.core.api.system.JkLog;

import java.nio.file.Paths;

public class JkGitWrapperRunner {

    public static void main(String[] args) {
        JkLog.setConsumer(JkLog.Style.INDENT);
        JkGitWrapper git = JkGitWrapper.of(Paths.get(""));
        System.out.println(git.getCurrentBranch());
        //git.exec("rev-parse --abbrev-ref HEAD");
        System.out.println(git.isWorkspaceDirty());
        System.out.println(git.getCurrentCommit());
        System.out.println(git.getTagsOfCurrentCommit());
        System.out.println(git.getVersionFromTags());
        System.out.println(git.getLastCommitMessage());
        System.out.println("***");
    }
}
