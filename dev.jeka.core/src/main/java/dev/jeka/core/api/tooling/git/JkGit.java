package dev.jeka.core.api.tooling.git;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.system.JkAbstractProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper for Git command line interface. This class assumes Git is installed on the host machine.
 */
public final class JkGit extends JkAbstractProcess<JkGit> {

    private JkGit() {
        this.setCommand("git");
    }

    private JkGit(JkGit other) {
        super(other);
    }

    /**
     * Creates a new JkGit instance pointing on the specified working directory.
     */
    public static JkGit of(Path dir) {
        return new JkGit().setWorkingDir(dir);
    }

    /**
     * Creates a new JkGit instance pointing on the specified working directory.
     */
    public static JkGit of(String dir) {
        return of(Paths.get(dir));
    }

    /**
     * Creates a new instance of JkGit pointing on the current working directory.
     */
    public static JkGit of() {
        return of("");
    }

    /**
     * Returns the current branch name.
     */
    public String getCurrentBranch() {
        List<String> branches = this.copy()
                .addParams("branch", "--show-current")
                .setLogWithJekaDecorator(false)
                .setCollectOutput(true)
                .exec().getOutputMultiline();
        if (branches.isEmpty()) {
            return null;
        }
        return branches.get(0);
    }

    /**
     * Checks if the local branch is in sync with the remote branch.
     */
    public boolean isSyncWithRemote() {
        String local = copy().setParams("rev-parse", "@").setCollectOutput(true).exec().getOutput();
        String remote = copy().setParams("rev-parse", "@{u}").setCollectOutput(true).exec().getOutput();
        return local.equals(remote);
    }

    /**
     * Checks if the workspace is dirty by executing the 'git diff HEAD --stat' command
     * and checking if the output is empty.
     *
     * @return true if the workspace is dirty, false otherwise.
     */
    public boolean isWorkspaceDirty() {
        return !copy()
                .setParams("diff", "HEAD", "--stat")
                .setLogWithJekaDecorator(false)
                .setCollectOutput(true)
                .exec().getOutput().isEmpty();
    }

    /**
     * Returns the current commit of the Git repository.
     */
    public String getCurrentCommit() {
        List<String> commits =  copy()
                .setParams("rev-parse", "HEAD")
                .setLogWithJekaDecorator(false)
                .setCollectOutput(true)
                .exec().getOutputMultiline();
        return commits.isEmpty() ? null : commits.get(0);
    }

    /**
     * Returns a list of tags associated with the current commit.
     */
    public List<String> getTagsOfCurrentCommit() {
        return copy()
                .setParams("tag", "-l", "--points-at", "HEAD")
                .setLogWithJekaDecorator(false)
                .setCollectOutput(true)
                .exec().getOutputMultiline()
                .stream()
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
    }

    /**
     * Returns the last commit message as a list of strings.
     * Each line of the message is represented as a separate element in the list.
     */
    public List<String> getLastCommitMessageMultiLine() {
        return copy()
                .setParams("log", "--oneline", "--format=%B", "-n 1", "HEAD")
                .setLogWithJekaDecorator(false)
                .setCollectOutput(true)
                .exec().getOutputMultiline();
    }

    /**
     * Returns the last commit message.
     */
    public String getLastCommitMessage() {
        return String.join("\n", getLastCommitMessageMultiLine());
    }

    /**
     * Extracts information from the last commit message title.
     * <p>
     * This method splits the commit message in separated words, then looks for the fist word starting
     * with the specified prefix. The returned suffix is the word found minus the prefix.
     * <p>
     * For example, if the title is 'Release:0.9.5.RC1 : Rework Dependencies', then
     * invoking this method with 'Release:' argument will return '0.9.5.RC1'.
     * <p>
     * This method returns <code>null</code> if no such prefix found.
     */
    public String extractSuffixFromLastCommitMessage(String prefix) {
        List<String> messageLines = getLastCommitMessageMultiLine();
        if (messageLines.isEmpty()) {
            return null;
        }
        String[] words = messageLines.get(0).split(" ");
        for (String word : words) {
            if (word.startsWith(prefix)) {
                return word.substring(prefix.length());
            }
        }
        return null;
    }

    /**
     * Puts a tag at the HEAD of the current branch and pushes it to the remote repository.
     */
    public JkGit tagAndPush(String name) {
        tag(name);
        copy().setParams("push", "origin", name).exec();
        return this;
    }

    /**
     * Adds a tag at the HEAD of the current branch.
     */
    public JkGit tag(String tagName) {
        copy().setParams("tag", tagName).exec();
        return this;
    }

    /**
     * Returns version according the last commit message.
     * <p>
     * If the commit message contains a word
     * starting with the specified prefix keyword then the substring following this suffix will be
     * returned.
     * <p>
     * If no such prefix found, then a version formatted as [branch]-SNAPSHOT will be returned
     */
    public String getVersionFromCommitMessage(String prefixKeyword) {
        String afterSuffix = extractSuffixFromLastCommitMessage(prefixKeyword);
        if (afterSuffix != null) {
            return afterSuffix;
        }
        String branch;
        try {
            branch = getCurrentBranch();
            return branch + "-SNAPSHOT";
        } catch (IllegalStateException e) {
            JkLog.warn(e.getMessage());
            return JkVersion.UNSPECIFIED.getValue();
        }
    }

    /**
     * Returns version according the tags found on current commit.
     * <p>
     * If the current commit has a tag starting by <code>prefix</code> and the workspace is not dirty,
     * then the version is this tag name on the current commit.
     * <p>
     * If there is many tags starting with <code>prefix</code> on the current commit, only the last one in alphabetical order is
     * taken in account.
     * <p>
     * If no tag starting with <code>prefix</code> is present on the current commit or the workspace is dirty,
     * the version will value <i>[branch]-SNAPSHOT</i>.
     * <p>
     * If <code>prefix</code> is empty, any tag will match.
     */
    public String getVersionFromTag(String prefix) {
        List<String> tags;
        String branch;
        boolean dirty;
        tags = getTagsOfCurrentCommit().stream()
                .filter(tag -> tag.startsWith(prefix))
                .collect(Collectors.toList());
        branch = getCurrentBranch();
        dirty = isWorkspaceDirty();
        if (branch == null) {   // detached Head
            JkLog.trace("Git detached branch. Infer version from exact matching tag.");
            return this.copy()
                    .setParams("describe", "--tags", "--exact-match")
                    .setCollectOutput(true).exec().getOutput();
        }
        if (tags.isEmpty() || dirty) {
            return branch + "-SNAPSHOT";
        } else {
            return tags.get(tags.size() - 1).substring(prefix.length());
        }
    }

    /**
     * Returns version according the tags found on current commit.
     *
     * @see #getVersionFromTag(String)
     */
    public String getVersionFromTag() {
        return getVersionFromTag("");
    }

    /**
     * Returns the JkVersion extracted from the tags of the current commit.
     *
     * @see #getVersionFromTag(String)
     */
    public JkVersion getJkVersionFromTag() {
        return JkVersion.of(getVersionFromTag());
    }

    /**
     * Returns the distinct last commit messages since last tag in the current branch.
     */
    public List<String> getCommitMessagesSinceLastTag() {
        String latestTag = getLatestTag();
        JkUtilsAssert.state(latestTag != null, "Latest tag not found");
        List<String> rawResults = copy()
                .setParams("log", "--oneline", getLatestTag() + "..HEAD")
                .setCollectOutput(true)
                .exec()
                .getOutputMultiline();
        List<String> result = new LinkedList<>();
        for (String line : rawResults) {
            String cleaned = JkUtilsString.substringAfterFirst(line, " "); // remove commit hash
            if (cleaned.startsWith("(")) {
                cleaned = JkUtilsString.substringAfterFirst(cleaned, ") ");
            }
            result.add(cleaned);
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Returns the latest tag in the Git repository.
     */
    public String getLatestTag() {
        List<String> tags = copy().setParams("describe", "--tags", "--abbrev=0")
                .setCollectOutput(true)
                .exec()
                .getOutputMultiline();
        return tags.isEmpty() ? null : tags.get(0);
    }

    /**
     * Tags the remote repository with a new tag.
     * <p>
     * This method performs the following steps:
     * <ul>
     * <li> Prints the existing tags on the origin.
     * <li> Checks if the Git workspace is dirty. If it is, it prints an error message and returns.
     * <li> Checks if the current tracking branch is aligned with the remote branch. If it is not, it prints an error message and returns.
     * <li> Prompts the user to enter a new tag.
     * <li> Tags the current commit with the new tag and pushes the tag to the remote repository.
     * </ul>
     *
     * Note: It is necessary to have a clean Git workspace and a tracking branch aligned with the remote branch
     *       in order to tag the remote repository.
     */
    public void tagRemote() {
        JkLog.info("Existing tags on origin :");
        this.copy().setLogWithJekaDecorator(true).setParams("ls remote", "--tag", "--sort=creatordate", "origin").exec();
        if (this.isWorkspaceDirty()) {
            JkLog.info("Git workspace is dirty. Please clean your Git workspace and retry");
            return;
        }
        if (!this.isSyncWithRemote()) {
            JkLog.info("The current tracking branch is not aligned with the remote. Please update/push and retry.");
            return;
        }
        JkLog.info("You are about to tag commit : " + this.getCurrentCommit());
        final String newTag = JkPrompt.ask("Enter new tag : ");
        this.setLogCommand(true).tagAndPush(newTag);
    }

    @Override
    public JkGit copy() {
        return new JkGit(this);
    }

}
