/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.tooling.git;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.system.JkAbstractProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class for executing Git commands within a specific directory.
 * This class extends JkAbstractProcess to facilitate the execution of process commands.
 */
public final class JkGit extends JkAbstractProcess<JkGit> {

    private JkGit() {
        this.addParams("git");
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
        String branch = this.copy()
                .addParams("branch", "--show-current")
                .setLogWithJekaDecorator(false)
                .setCollectStdout(true)
                .execAndCheck()
                .getStdoutAsString()
                .replace("\n", "");
        if (JkUtilsString.isBlank(branch)) {
            return null;
        }
        return branch;
    }

    public boolean isOnGitRepo() {
        String result = this.copy()
                .addParams("rev-parse ", "--is-inside-work-tree")
                .setCollectStdout(true)
                .exec()
                .getStdoutAsString();
        return "true".equals(result);
    }

    /**
     * Checks if the local branch is in sync with the remote branch.
     */
    public boolean isSyncWithRemote() {
        String local = copy().addParams("rev-parse", "@").setCollectStdout(true).execAndCheck().getStdoutAsString();
        String remote = copy().addParams("rev-parse", "@{u}").setCollectStdout(true).execAndCheck().getStdoutAsString();
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
                .addParams("diff", "HEAD", "--stat")
                .setLogWithJekaDecorator(false)
                .setCollectStdout(true)
                .execAndCheck()
                .getStdoutAsString()
                .replace("\n", "")
                .isEmpty();
    }

    /**
     * Returns the current commit of the Git repository.
     */
    public String getCurrentCommit() {
        String commit = copy()
                .addParams("rev-parse", "HEAD")
                .setLogWithJekaDecorator(false)
                .setCollectStdout(true)
                .execAndCheck()
                .getStdoutAsString()
                .replace("\n", "");
        return commit.isEmpty() ? null : commit;
    }

    /**
     * Returns a list of tags associated with the current commit.
     */
    public List<String> getTagsOnCurrentCommit() {
        return copy()
                .addParams("tag", "-l", "--points-at", "HEAD")
                .setLogWithJekaDecorator(false)
                .setCollectStdout(true)
                .execAndCheck()
                .getStdoutAsMultiline()
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
                .addParams("log", "--oneline", "--format=%B", "-n 1", "HEAD")
                .setLogWithJekaDecorator(false)
                .setCollectStdout(true)
                .execAndCheck()
                .getStdoutAsMultiline();
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
        copy().addParams("push", "origin", name).execAndCheck();
        return this;
    }

    /**
     * Adds a tag at the HEAD of the current branch.
     */
    public JkGit tag(String tagName) {
        copy().addParams("tag", tagName).execAndCheck();
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
        tags = getTagsOnCurrentCommit().stream()
                .filter(tag -> tag.startsWith(prefix))
                .collect(Collectors.toList());
        branch = getCurrentBranch();
        dirty = isWorkspaceDirty();
        if (branch == null) {   // detached Head
            JkLog.verbose("Git detached branch. Infer version from exact matching tag.");
            return this.copy()
                    .addParams("describe", "--tags", "--exact-match")
                    .setCollectStdout(true)
                    .execAndCheck()
                    .getStdoutAsString()
                    .replace("\n", "");
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
                .addParams("log", "--oneline", getLatestTag() + "..HEAD")
                .setCollectStdout(true)
                .execAndCheck()
                .getStdoutAsMultiline();
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
        List<String> tags = copy().addParams("describe", "--tags", "--abbrev=0")
                .setCollectStdout(true)
                .execAndCheck()
                .getStdoutAsMultiline();
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
        this.copy().setLogWithJekaDecorator(true).addParams("ls-remote", "--tag", "--sort=creatordate", "origin").execAndCheck();
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

    /**
     * Executes a Git diff command between the specified commits and returns a list of files changed.
     *
     * @param commitFrom The starting commit of the diff.
     * @param commitTo The ending commit of the diff. This can value <i>HEAD</i>.
     * @return A FileList instance containing the list of files changed between the two commits.
     */
    public FileList diiff(String commitFrom, String commitTo) {
        List<String> rawResults = copy()
                .addParams("--no-pager", "diff", "--name-only", commitFrom, commitTo)
                .setCollectStdout(true)
                .execAndCheck()
                .getStdoutAsMultiline();
        return new FileList(rawResults);
    }

    @Override
    public JkGit copy() {
        return new JkGit(this);
    }


    private JkProcResult execAndCheck() {
        JkProcResult procResult = exec();
        if (procResult.getExitCode() != 0) {
            if (!copy().isOnGitRepo()) {
                throw new JkException("Cannot find a Git repository from " + this.getWorkingDir());
            }
        }
        return procResult;
    }

    public static class FileList {

        private final List<String> files;

        private FileList(List<String> files) {
            this.files = files;
        }

        public List<String> get() {
            return Collections.unmodifiableList(files);
        }

        public boolean hasFileStartingWith(String prefix) {
            return files.stream().anyMatch(file -> file.startsWith(prefix));
        }
    }

}
