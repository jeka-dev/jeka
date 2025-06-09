/*
 * Copyright 2014-2025  the original author or authors.
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

package dev.jeka.core.tool.builtins.app;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.utils.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

class AppManager {

    private final Path appDir;

    private final Path repoCacheDir;

    enum UpdateStatus {
        OUTDATED, UP_TO_DATE, TAG_DELETED
    }

    AppManager(Path appDir, Path repoCacheDir) {
        this.appDir = appDir;
        this.repoCacheDir = repoCacheDir;
    }

    void install(String appName, RepoAndTag repoAndTag, boolean isNative) {
        Path repoDir = repoDir(appName);
        JkUtilsPath.deleteQuietly(repoDir, false);
        JkUtilsPath.createDirectories(repoDir);
        JkLog.info("Cloning to %s...", repoDir);
        JkGit.of(repoDir)
                .addParams("clone", "--quiet", "-c", "advice.detachedHead=false", "--depth", "1")
                .addParamsIf(repoAndTag.hasTag(), "--branch", repoAndTag.tag)
                .addParams(repoAndTag.repoUrl, repoDir.toString())
                .exec();
        JkLog.info("Build app...");
        buildAndInstall(appName, isNative, repoDir);
    }

    void updateWithTag(String appName, String tag) {
        Path appFile = findAppFile(appName);
        if (!Files.exists(appFile)) {
            JkLog.info("App %s not found. Nothing to update.", appFile);
            return;
        }
        Path repoDir = repoDir(appName);
        JkGit git = JkGit.of(repoDir);
        git.execCmd("fetch", "--tags");
        git.execCmd("pull");
        git.execCmd("checkout", tag);

        boolean isNative = isNative(appFile);
        JkLog.info("Re-building the app...");
        buildAndInstall(appName, isNative, repoDir);
    }

    void updateToLastCommit(String appName) {
        Path appFile = findAppFile(appName);
        if (!Files.exists(appFile)) {
            JkLog.info("App %s not found. Nothing to update.", appFile);
            return;
        }
        Path repoDir = repoDir(appName);
        JkGit git = JkGit.of(repoDir);
        JkLog.startTask("update-git-repo");
        String branch = git.getCurrentBranch();
        git.execCmd("fetch", "origin", branch);
        git.execCmd("merge", "--ff-only", "origin/" + branch);

        boolean isNative = isNative(appFile);
        JkLog.info("Re-building the app...");
        buildAndInstall(appName, isNative, repoDir);

        JkLog.endTask();
    }

    boolean uninstall(String appName) {
        Path appFile = findAppFile(appName);
        boolean found = false;
        if (!Files.exists(appFile)) {
            JkLog.info("[INFO] App %s not found. No executable to remove.", appFile);
        } else {
            JkLog.verbose("Delete app file %s.", appFile);
            JkUtilsPath.deleteQuietly(appFile, false);
            found = true;
        }
        Path repoDir = repoDir(appName);
        if (!Files.exists(repoDir)) {
            JkLog.verbose("APP repo %s not found. No repository to remove.", repoDir);
        } else {
            found = true;
            JkLog.verbose("Delete repo directory %s.", repoDir);
            JkUtilsPath.deleteQuietly(repoDir, true);
        }
        return found;
    }

    AppInfo getAppInfo(String appName) {
        Path repoDir = repoDir(appName);
        JkLog.debug("Resolve app %s git local repo to: %s", appName, repoDir);
        JkGit git = JkGit.of(repoDir);
        String remoteRepoUrl = git.getRemoteUrl();
        boolean isNative = isNative(findAppFile(appName));
        Optional<GitTag> optionalTag = getTag(repoDir);
        boolean isVersion = optionalTag.map(GitTag::isVersion).orElse(false);
        String status;
        try {
            if (isVersion) {
                TagBucket tagBucket = getRemoteTagBucketFromGitUrl(remoteRepoUrl);
                if (tagBucket.isLatestVersion(optionalTag.get().value)) {
                    status = "up-to-date";
                } else {
                    status = "outdated -> " + tagBucket.getHighestVersion();
                }
            } else {
                UpdateStatus updateStatus = getTagCommitStatus(appName, optionalTag.orElse(null));
                if (updateStatus.equals(UpdateStatus.UP_TO_DATE)) {
                    status = "up-to-date";
                } else if (updateStatus.equals(UpdateStatus.TAG_DELETED)) {
                    status = "Unknown: tag-deleted";
                } else {
                    status = "outdated: new-commits";
                }
            }
        } catch (RuntimeException e) {
            JkLog.warn("Failed to get info from remote repo %s.", remoteRepoUrl);
            status = "?";
        }
        String tagValue = optionalTag.map(GitTag::toString).orElse("<" + git.getCurrentBranch() + ">");
        RepoAndTag repoAndTag = new RepoAndTag(remoteRepoUrl, tagValue);
        return new AppInfo(appName, repoAndTag, isNative, status);
    }

    Optional<GitTag> getCurrentTag(String appName) {
        Path repoDir = repoDir(appName);
        return getTag(repoDir);
    }

    List<String> getRemoteTags(String appName) {
        Path repoDir = repoDir(appName);
        JkGit git = JkGit.of(repoDir);
        String remoteUrl = git.getRemoteUrl();
        return git.getRemoteTagAsStrings(remoteUrl);
    }

    TagBucket getRemoteTagBucketFromGitUrl(String remoteUrl) {
        List<JkGit.Tag> tags = JkGit.of().getRemoteTags(remoteUrl);
        return TagBucket.of(tags);
    }

    TagBucket getRemoteTagBucketFromAppName(String appName) {
        Path repoDir = repoDir(appName);
        JkGit git = JkGit.of(repoDir);
        String remoteRepoUrl = git.getRemoteUrl();
        return getRemoteTagBucketFromGitUrl(remoteRepoUrl);
    }

    List<String> installedAppNames() {
        return JkUtilsPath.listDirectChildren(appDir).stream()
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().endsWith(".jar"))
                .filter(path -> !path.toString().endsWith(".ps1"))
                .filter(path -> !systemFiles().contains(path.getFileName().toString()))
                .filter(path -> !path.getFileName().toString().equals("LICENSE"))
                .map(path -> path.getFileName().toString())
                .map(fileName -> fileName.endsWith(".bat") ?
                        JkUtilsString.substringBeforeLast(fileName, ".") : fileName)
                .collect(Collectors.toList());
    }

    String suggestName(String originalName) {
        String candidate = originalName;
        if (systemFiles().contains(originalName)) {
            candidate = randomName();
        }
        int i = 1;
        while (installedAppNames().contains(candidate)) {
            if (candidate.contains(originalName)) {
                candidate = randomName();
            }
            i++;
            if (i > 30) {
                candidate = randomName() + "-" + new Random().nextInt(999999999);
                break;
            }
        }
        return candidate;
    }

    String getRemoteDefaultBranch(String remoteUrl) {
        return JkGit.of().getRemoteDefaultBranch(remoteUrl);
    }

    List<AppVersion> getAppVersionsForRepo(String repoUrl) {
        List<AppVersion> result = new ArrayList<>();
        for (String appName : installedAppNames()) {
            Path repoDir = repoDir(appName);
            JkGit git = JkGit.of(repoDir);
            JkUtilsPath.createDirectories(repoDir);
            String remoteRepoUrl = git.getRemoteUrl();
            if (remoteRepoUrl.equals(repoUrl)) {
                String tag = getTag(repoDir).map(GitTag::toString).orElse("<" + git.getCurrentBranch() + ">");
                boolean isNative = isNative(findAppFile(appName));
                result.add(new AppVersion(appName, tag, isNative));
            }
        }
        return result;
    }

    /**
     * @param gitTag Maybe null. If null, look at the latest commit.
     */
    UpdateStatus getTagCommitStatus(String appName, GitTag gitTag) {
        Path repoDir = repoDir(appName);
        JkGit git = JkGit.of(repoDir);
        String remoteRepo = git.getRemoteUrl();
        String tagValue =  Optional.ofNullable(gitTag).map(GitTag::toString).orElse(null);
        String remoteTagCommit = git.getRemoteTagCommit(remoteRepo, tagValue);
        if (gitTag != null && remoteTagCommit == null) {
            JkLog.warn("Found tag %s on current commit of repo %s no longer exist in remote repo %s.",
                    gitTag, repoDir, remoteRepo);
            return UpdateStatus.TAG_DELETED;
        }
        String localTagCommit = git.getCurrentCommit();
        if (JkUtilsObject.equals(localTagCommit, remoteTagCommit)) {
            return UpdateStatus.UP_TO_DATE;
        }
        return UpdateStatus.OUTDATED;
    }

    static boolean isAppNameValid(String name) {
        return name.matches("[a-zA-Z0-9\\-]+");
    }

    private void buildAndInstall(String appName, boolean isNative, Path repoDir) {
        Path artefact;
        try {
            artefact = AppBuilder.build(repoDir, isNative);
        } catch (RuntimeException e) {
            JkGit git = JkGit.of(repoDir);
            String remoteRepoUrl = git.getRemoteUrl();
            Optional<GitTag> optionalGitTag = getTag(repoDir);
            String tagExpression = optionalGitTag.map(gitTag -> "with tag " + gitTag).orElse("HEAD");
            JkLog.error("Error building '%s' app from repo %s %s.", appName, remoteRepoUrl, tagExpression);
            JkLog.error("The version fetched may have errors. Try a different tag to install or update.");
            throw e;
        }

        String fileName = (JkUtilsSystem.IS_WINDOWS && !isNative) ? appName + ".bat" : appName;
        JkUtilsPath.move(artefact, appDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
    }



    private Optional<GitTag> getTag(Path repoDir) {
        List<String> currentTags = JkGit.of(repoDir).getTagsOnCurrentCommit();
        if (currentTags.isEmpty()) {
            JkLog.debug("No repo tags found on local repo %s.", repoDir);
            return Optional.empty();
        }
        return Optional.of(new GitTag(currentTags.get(0)));
    }

    private Path repoDir(String appName) {
        return repoCacheDir.resolve(appName);
    }

    private static String randomName() {
        String[] randomName = {
                "apollo", "nova", "eclipse", "atlas", "orion",
                "zephyr", "aquila", "nimbus", "lumen", "horizon",
                "solace", "phoenix", "cosmos", "aurora", "vortex",
                "pulse", "echo", "galaxy", "falcon", "summit"
        };
        return randomName[new Random().nextInt(randomName.length - 1)];
    }

    private static List<String> systemFiles() {
        return JkUtilsIterable.listOf("jeka", "jeka.bat", "jeka-update");
    }

    private Path findAppFile(String appName) {
        String fileName = appName;
        if (JkUtilsSystem.IS_WINDOWS) {
            fileName = appName + ".exe";
            if (!Files.exists(appDir.resolve(fileName))) {
                fileName = appName + ".bat";
            }
        }
        return appDir.resolve(fileName);
    }

    private boolean isNative(Path appFile) {
        if (JkUtilsSystem.IS_WINDOWS) {
            return appFile.toString().endsWith(".exe");
        }
        long size = JkUtilsPath.size(appFile);
        if (size > 1024 * 10) { // if it's that large, it cannot be a shell script
            return true;
        }
        String content = JkPathFile.of(appFile).readAsString();
        return !content.trim().startsWith(AppBuilder.SHE_BANG);
    }

    static class AppVersion {

        final String appName;

        final String version;

        final boolean isNative;

        public AppVersion(String appName, String version, boolean isNative) {
            this.appName = appName;
            this.version = version;
            this.isNative = isNative;
        }

        @Override
        public String toString() {
            return appName + " " + version + (isNative ? "  native" : " jvm");
        }
    }

}
