package dev.jeka.core.api.tooling.git;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class JkGitTest {

    JkGit git = JkGit.of(Paths.get(""));

    @Test
    public void getCurrentBranch() {
        System.out.printf("--%s--%n", git.getCurrentBranch());
    }

    @Test
    public void isSyncWithRemote() {
        System.out.printf("--%s--%n", git.isSyncWithRemote());
    }

    @Test
    public void isWorkspaceDirty() {
        System.out.printf("--%s--%n", git.isWorkspaceDirty());
    }

    @Test
    public void getCurrentCommit() {
        System.out.printf("--%s--%n", git.getCurrentCommit());
    }

    @Test
    public void getTagsOfCurrentCommit() {
        System.out.printf("--%s--%n", git.getTagsOfCurrentCommit());
    }
}