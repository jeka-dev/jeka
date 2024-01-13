package dev.jeka.core.api.tooling;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;

import java.nio.file.Paths;

public class JkGitWrapperRunner {

    public static void main(String[] args) {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkGit git = dev.jeka.core.api.tooling.git.JkGit.of(Paths.get(""));
        System.out.println(git.getCurrentBranch());
        //git.exec("rev-parse --abbrev-ref HEAD");
        System.out.println(git.isWorkspaceDirty());
        System.out.println(git.getCurrentCommit());
        System.out.println(git.getTagsOfCurrentCommit());
        System.out.println(git.getVersionFromTag());
        System.out.println(git.getLastCommitMessageMultiLine());
        System.out.println("***");
    }
}
