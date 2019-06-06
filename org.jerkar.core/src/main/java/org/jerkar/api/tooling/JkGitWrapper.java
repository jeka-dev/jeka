package org.jerkar.api.tooling;

import org.jerkar.api.system.JkProcess;

import java.nio.file.Path;
import java.util.List;

/**
 * Wrapper for Git command line interface. This class assumes Git is installed on the host machine.
 */
public final class JkGitWrapper {

    private JkProcess git;

    private JkGitWrapper(JkProcess process) {
        this.git = process;
    }

    public static JkGitWrapper of(Path dir) {
        return new JkGitWrapper(JkProcess.of("git").withWorkingDir(dir));
    }

    public String getCurrentBranch() {
        return git.andParams("rev-parse", "--abbrev-ref", "HEAD").withLogOutput(false).runAndReturnOutputAsLines().get(0);
    }

    public boolean isDirty() {
        return !git.andParams("diff", "HEAD", "--stat").withLogOutput(false).runAndReturnOutputAsLines().isEmpty();
    }

    public String getCurrentCommit() {
        return git.andParams("rev-parse", "HEAD").withLogOutput(false).runAndReturnOutputAsLines().get(0);
    }

    public List<String> getTagsOfCurrentCommit() {
        return git.andParams("tag", "-l", "--points-at", "HEAD").withLogOutput(false).runAndReturnOutputAsLines();
    }

    public JkGitWrapper tagAndPush(String name) {
        git.andParams("tag", name).runSync();
        git.andParams("push", "origin", "--tags").runSync();
        return this;
    }

    /**
     * If the current commit is tagged then the version is the tag name (last in alphabetical order. Otherwise
     * the version is [branch]-SNAPSHOT
     */
    public String getVersionWithTagOrSnapshot() {
        List<String> tags = getTagsOfCurrentCommit();
        String branch = getCurrentBranch();
        if (tags.isEmpty()) {
            return branch + "-SNAPSHOT";
        } else {
            return tags.get(0);
        }
    }

    public JkGitWrapper exec(String... args) {
        git.andParams(args).runSync();
        return this;
    }


}
