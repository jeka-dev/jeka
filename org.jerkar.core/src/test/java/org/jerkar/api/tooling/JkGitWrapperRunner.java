package org.jerkar.api.tooling;

import org.jerkar.api.system.JkLog;

import java.nio.file.Paths;

public class JkGitWrapperRunner {

    public static void main(String[] args) {
        JkLog.registerHierarchicalConsoleHandler();
        JkGitWrapper git = JkGitWrapper.of(Paths.get(""));
        System.out.println(git.getCurrentBranch());
        //git.exec("rev-parse --abbrev-ref HEAD");
        System.out.println(git.isDirty());
        System.out.println(git.getCurrentCommit());
        System.out.println(git.getTagsOfCurrentCommit());
        System.out.println(git.getVersionWithTagOrSnapshot());
        System.out.println("***");
    }
}
