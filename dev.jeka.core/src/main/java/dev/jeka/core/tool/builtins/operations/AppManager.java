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

package dev.jeka.core.tool.builtins.operations;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.utils.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

class AppManager {

    private static final String APP_PROP_PREFIX = "jeka.installed.app.";

    private static final String NATIVE_PROP_FLAG = ", native";

    private final Path appDir;

    private final PropFile globalPropFile;

    private final Path repoCacheDir;

    enum UpdateStatus {
        OUTDATED, UP_TO_DATE, FLAG_DELETED
    }

    AppManager(Path appDir, Path repoCacheDir, PropFile globalPropFile) {
        this.appDir = appDir;
        this.globalPropFile = globalPropFile;
        this.repoCacheDir = repoCacheDir;
    }

    void install(String appName, RepoAndTag repoAndTag, boolean isNative) {
        Path repoDir = repoDir(appName);
        JkUtilsPath.deleteQuietly(repoDir, false);
        JkUtilsPath.createDirectories(repoDir);
        JkLog.info("Cloning %s...", repoDir);
        JkGit.of(repoDir)
                .addParams("clone", "--quiet", "-c", "advice.detachedHead=false", "--depth", "1")
                .addParamsIf(repoAndTag.hasTag(), "--branch", repoAndTag.tag)
                .addParams(repoAndTag.repoUrl, repoDir.toString())
                .exec();
        JkLog.info("Build app...");
        buildAndInstall(appName, isNative, repoDir);
    }

    UpdateStatus update(String appName) {
        Path appFile = findAppFile(appName);
        if (!Files.exists(appFile)) {
            JkLog.info("App '%s' not found. Nothing to update.", appName);
            JkLog.info("Execute 'jeka app: list' to see installed apps.", appName);
            return null;
        }
        Path repoDir = repoDir(appName);
        String tag = getTag(repoDir);
        return updateWithTag(appName, tag);
    }

    UpdateStatus updateWithTag(String appName, String tag) {
        Path appFile = findAppFile(appName);
        if (!Files.exists(appFile)) {
            JkLog.info("App %s not found. Nothing to update.", appFile);
            return null;
        }
        Path repoDir = repoDir(appName);
        UpdateStatus outDatedStatus = updateRepoIfNeeded(appName, tag);
        if (outDatedStatus == UpdateStatus.OUTDATED) {
            boolean isNative = isNative(appFile);
            JkLog.info("Re-building the app...");
            buildAndInstall(appName, isNative, repoDir);
        }
        return outDatedStatus;
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
        JkGit git = JkGit.of(repoDir);
        String remoteRepoUrl = git.getRemoteUrl();
        String tag = getTag(repoDir);
        boolean isNative = isNative(findAppFile(appName));
        UpdateStatus updateStatus = null;
        try {
            updateStatus = getUpdateStatus(appName, tag);
        } catch (RuntimeException e) {
            JkLog.warn("Failed to get info from remote repo %.", remoteRepoUrl);
        }
        String tagValue = tag == null ? "<" + git.getCurrentBranch() + ">" : tag;
        RepoAndTag repoAndTag = new RepoAndTag(remoteRepoUrl, tagValue);
        return new AppInfo(appName, repoAndTag, isNative, updateStatus);
    }

    List<String> getRemoteTags(String appName) {
        Path repoDir = repoDir(appName);
        JkGit git = JkGit.of(repoDir);
        String remoteUrl = git.getRemoteUrl();
        return git.getRemoteTagAsStrings(remoteUrl);
    }

    TagBucket getRemteTagBucket(String remoteUrl) {
        List<JkGit.Tag> tags = JkGit.of().getRemoteTags(remoteUrl);
        return TagBucket.of(tags);
    }

    private void buildAndInstall(String appName, boolean isNative, Path repoDir) {
        Path artefact;
        try {
            artefact = AppBuilder.build(repoDir, isNative);
        } catch (RuntimeException e) {
            JkGit git = JkGit.of(repoDir);
            String remoteRepoUrl = git.getRemoteUrl();
            String tag = getTag(repoDir);
            String tagExpression = tag == null ? "HEAD" : "with tag " + tag;
            JkLog.error("Error building '%s' app from repo %s %s.", appName, remoteRepoUrl, tagExpression);
            JkLog.error("The version fetched may have errors. Try a different tag to install or update.");
            throw e;
        }

        String fileName = JkUtilsSystem.IS_WINDOWS ?
                ( isNative ? appName + ".exe" : appName + ".bat")
                : appName;
        JkUtilsPath.move(artefact, appDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    private UpdateStatus updateRepoIfNeeded(String appName, String tag) {
        UpdateStatus updateStatus = getUpdateStatus(appName, tag);

        // Update repo
        if (updateStatus == UpdateStatus.OUTDATED) {
            Path repoDir = repoDir(appName);
            updateRepo(repoDir, tag);
        }
        return UpdateStatus.OUTDATED;
    }

    private UpdateStatus getUpdateStatus(String appName, String tag) {
        Path repoDir = repoDir(appName);
        JkGit git = JkGit.of(repoDir);
        String remoteRepo = git.getRemoteUrl();
        String remoteTagCommit = git.getRemoteTagCommit(remoteRepo, tag);
        if (tag != null && remoteTagCommit == null) {
            JkLog.warn("Found tag %s on current commit of repo %s no longer exist in remote repo %s.",
                    tag, repoDir, remoteRepo);
            return UpdateStatus.FLAG_DELETED;
        }
        String localTagCommit = git.getCurrentCommit();
        if (JkUtilsObject.equals(localTagCommit, remoteTagCommit)) {
            return UpdateStatus.UP_TO_DATE;
        }
        return UpdateStatus.OUTDATED;
    }

    List<String> installedAppNames() {
        return JkUtilsPath.listDirectChildren(appDir).stream()
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().endsWith(".jar"))
                .filter(path -> !path.toString().endsWith(".ps1"))
                .filter(path -> !systemFiles().contains(path.getFileName().toString()))
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

    private void updateRepo(Path repoDir, String tag) {
        JkGit git = JkGit.of(repoDir);
        JkLog.startTask("update-git-repo");
        git.execCmd("fetch");
        if (tag != null) {
            JkLog.info("Checkout repo tag %s", tag);
            git.execCmd("checkout", tag);
        } else {
            String branch = git.getCurrentBranch();
            JkLog.info("Checkout branch %s", branch);
            git.execCmd("checkout", branch);
        }
        JkLog.endTask();
    }

    private String getTag(Path repoDir) {
        List<String> currentTags = JkGit.of(repoDir).getTagsOnCurrentCommit();
        if (currentTags.isEmpty()) {
            JkLog.debug("No repo tags found on local repo %s.", repoDir);
            return null;
        }
        return currentTags.get(0);
    }

    static boolean isAppNameValid(String name) {
        return name.matches("[a-zA-Z0-9\\-]+");
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

}
