package dev.jeka.core.api.tooling;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Wrapper for Git command line interface. This class assumes Git is installed on the host machine.
 */
public final class JkGitWrapper {

    private final JkProcess git;

    private JkGitWrapper(JkProcess process) {
        this.git = process.withFailOnError(false);
    }

    public static JkGitWrapper of(Path dir) {
        return new JkGitWrapper(JkProcess.of("git").withWorkingDir(dir).withFailOnError(true));
    }

    public static JkGitWrapper of(String dir) {
        return of(Paths.get(""));
    }

    public static JkGitWrapper of() {
        return of("");
    }

    public JkGitWrapper withLogCommand(boolean log) {
        return new JkGitWrapper(this.git.withLogCommand(log));
    }

    public JkGitWrapper withFailOnError(boolean fail) {
        return new JkGitWrapper(this.git.withFailOnError(fail));
    }

    public JkGitWrapper withLogOutput(boolean log) {
        return new JkGitWrapper(this.git.withLogOutput(log));
    }

    public String getCurrentBranch() {
        return git.andParams("rev-parse", "--abbrev-ref", "HEAD").withLogOutput(false).runAndReturnOutputAsLines().get(0);
    }

    public boolean isRemoteEqual() {
        Object local = git.andParams("rev-parse", "@").runAndReturnOutputAsLines();
        Object remote = git.andParams("rev-parse", "@{u}").runAndReturnOutputAsLines();
        return  local.equals(remote);
    }

    public boolean isWorkspaceDirty() {
        return !git.andParams("diff", "HEAD", "--stat").withLogOutput(false).runAndReturnOutputAsLines().isEmpty();
    }

    public String getCurrentCommit() {
        return git.andParams("rev-parse", "HEAD").withLogOutput(false).runAndReturnOutputAsLines().get(0);
    }

    public List<String> getTagsOfCurrentCommit() {
        return git.andParams("tag", "-l", "--points-at", "HEAD").withLogOutput(false).runAndReturnOutputAsLines();
    }

    public List<String> getLastCommitMessage() {
        return git.andParams("log", "--oneline", "--format=%B", "-n 1", "HEAD").withLogOutput(false).runAndReturnOutputAsLines();
    }

    public JkGitWrapper tagAndPush(String name) {
        git.andParams("tag", name).runSync();
        git.andParams("push", "origin", "--tags").runSync();
        return this;
    }

    /**
     * If the current commit is tagged then the version is the tag name (last in alphabetical order). Otherwise
     * the version is [branch]-SNAPSHOT
     */
    public String getVersionFromTags() {
        List<String> tags;
        String branch;
        try {
            tags = getTagsOfCurrentCommit();
            branch = getCurrentBranch();
        } catch (IllegalStateException e) {
            JkLog.warn(e.getMessage());
            return JkVersion.UNSPECIFIED.getValue();
        }
        if (tags.isEmpty()) {
            return branch + "-SNAPSHOT";
        } else {
            return tags.get(tags.size() -1);
        }
    }

    /**
     * @see #getVersionFromTags()
     */
    public JkVersion getJkVersionFromTags() {
        return JkVersion.of(getVersionFromTags());
    }

    public JkGitWrapper exec(String... args) {
        JkProcess gitCommand = git.andParams(args);
        int code = gitCommand.runSync();
        JkUtilsAssert.state(code == 0, "Command " + git + " returned with error " + code);
        return this;
    }

}
