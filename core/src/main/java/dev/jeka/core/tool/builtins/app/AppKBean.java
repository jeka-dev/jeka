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

import dev.jeka.core.api.system.*;
import dev.jeka.core.api.text.JkColumnText;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocUrl;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.util.List;
import java.util.Optional;

@JkDoc("Provides a way to install, update, or remove applications from the user PATH.\n" +
        "Applications are installed from a Git repository and built by the client before installation.\n" +
        "Applications can be installed as executable JARs or native apps.")
@JkDocUrl("https://jeka-dev.github.io/jeka/reference/kbeans-app/")
public class AppKBean extends KBean {

    private static final PropFile GLOBAL_PROP_FILE = new PropFile(JkLocator.getGlobalPropertiesFile());

    private final AppManager appManager =  AppManager.of();

    private static final String COLUMN_SEPARATOR = JkUtilsSystem.IS_WINDOWS ? " | " : " │ ";

    @JkDoc("Git Remote repository URL of the app to install.")
    public String repo;

    @JkDoc("Specifies the name of the app to update/uninstall.")
    public String name;

    @JkDoc("Short description of the app provided by the application author.")
    public String description;

    @JkDoc("Specifies the url to trust.")
    public String url;

    @JkDoc("Builds and installs the app to make it available in PATH.\n" +
            "Use `repo=[Git URL]` to set the source repository.\n" +
            "Use `native:` argument to install as a native app.")
    public void install() {
        String gitUrl = this.repo;
        if (JkUtilsString.isBlank(gitUrl)) {
            JkLog.info("You must specify the git url using 'remote=[Git URL]'.");
            return;
        }
        String remoteUrl = this.repo.trim();
        String suggestedAppName = name;
        boolean shouldAskAppName = JkUtilsString.isBlank(name);
        if (!remoteUrl.contains(":")) {

            // referencing catalog app
            if (remoteUrl.contains("@")) {
                suggestedAppName = JkUtilsString.substringBeforeFirst(remoteUrl, "@");
                shouldAskAppName = false;
                Catalog.AppInfo appInfo = Catalogs.findApp(remoteUrl);
                if (appInfo == null) {
                    JkLog.error("No app or catalog found for: " + remoteUrl);

                    return;
                }
                remoteUrl = appInfo.repo;
            } else {
                remoteUrl = Catalog.GITHUB_URL + remoteUrl;
            }

        }


        List<AppManager.AppVersion> installedAppsForRepo = appManager.getAppVersionsForRepo(remoteUrl);
        if (!installedAppsForRepo.isEmpty()) {
            JkLog.info("This repository has been already installed for following app/versions:");
            installedAppsForRepo.forEach(System.out::println);
            String response = JkPrompt.ask("Do you want to install another version? %s:", JkAnsi.yellow("[N,y]")).trim();
            if (!response.equalsIgnoreCase("y")) {
                JkLog.info("Installation aborted by user.");
                return;
            }
        }


        if (JkUtilsString.isBlank(suggestedAppName)) {
            suggestedAppName = JkUtilsString.substringAfterLast(remoteUrl, "/").toLowerCase();
            if (suggestedAppName.endsWith(".git")) {
                suggestedAppName = JkUtilsString.substringBeforeLast(suggestedAppName, ".git");
            }
        }


        // Ask for tag/version
        JkBusyIndicator.start(JkLog.getOutPrintStream(), "Fetching info from Git");
        String branch = appManager.getRemoteDefaultBranch(remoteUrl);
        JkLog.debug("Use default branch: %s", JkAnsi.magenta(branch));
        TagBucket tagBucket = appManager.getRemoteTagBucketFromGitUrl(remoteUrl);
        JkBusyIndicator.stop();

        boolean allowed = checkSecurityFlow(remoteUrl);
        if (!allowed) {
            JkLog.info("Installation aborted by user.");
            return;
        }

        final RepoAndTag repoAndTag;

        // No tag available
        if (tagBucket.tags.isEmpty()) {
            JkLog.info("No tags found in remote Git repository. Last commit from branch <%s> will be installed.",
                    JkAnsi.magenta(branch));
            repoAndTag = new RepoAndTag(remoteUrl, null);

            // 1 tag available
        } else if (tagBucket.tags.size() == 1) {
            String tag = tagBucket.tags.get(0).getName();
            JkLog.info("Found one tag '%s' in the remote Git repository.", JkAnsi.magenta(tag));
            String response = JkPrompt.ask("Do you want to install the tag '%s' or last commit on branch %s:? " +
                    "[%s = last commit, %s = '%s' tag]:",
                    JkAnsi.magenta(tag),
                    JkAnsi.magenta(branch),
                    JkAnsi.yellow("@"),
                    JkAnsi.yellow("<ENTER>"),
                    JkAnsi.magenta(tag));
            if ("@".equals(response)) {
                JkLog.info("Last commit from branch %s will be installed.", JkAnsi.magenta(branch));
                repoAndTag = new RepoAndTag(remoteUrl, null);
            } else {
                JkLog.info("Version %s will be installed.", JkAnsi.magenta(tag));
                repoAndTag = new RepoAndTag(remoteUrl, tag);
            }

            // many tags available
        } else {
            JkLog.info("List of available tags:");
            tagBucket.tags.forEach(System.out::println);
            String highest = tagBucket.getHighestVersion();
            String chooseTag = null; // empty mean choose last commit
            while (chooseTag == null) {
                String response = JkPrompt.ask("Enter the version to install [%s = last commit /" +
                        " %s = '%s' tag]:",
                                JkAnsi.yellow("@"),
                                JkAnsi.yellow("<ENTER>"),
                                JkAnsi.magenta(highest)
                        ).trim();
                if (JkUtilsString.isBlank(response)) {
                    JkLog.info("Version %s will be installed.", JkAnsi.magenta(highest));
                    chooseTag = highest;
                } else if (response.equalsIgnoreCase("@")) {
                    JkLog.info("Last commit from branch %s will be installed.", JkAnsi.magenta(branch));
                    chooseTag = "";
                } else {
                    if (tagBucket.hasTag(response)) {
                        JkLog.info("Version %s will be installed.", JkAnsi.magenta(response));
                        chooseTag = response;
                    } else {
                        JkLog.info("The tag '%s' is not on the list. Please, choose one from the list.",
                                JkAnsi.magenta(response));
                    }
                }
            }
            repoAndTag = new RepoAndTag(remoteUrl, chooseTag);
        }

        // Ask for name
        boolean nameOk = !shouldAskAppName && !systemFiles().contains(name);
        boolean retry = false;
        while(!nameOk) {
            String response;
            if (appManager.installedAppNames().contains(suggestedAppName)) {
                if (!retry) {
                    JkLog.info("An application named '%s' is already installed.", JkAnsi.yellow(suggestedAppName));
                }
                response = JkPrompt.ask("Choose a new name for the application:").trim();
            } else {
                String suggestName = appManager.suggestName(suggestedAppName);
                response = JkPrompt.ask("Choose a name for this application. Press %s to select '%s':",
                        JkAnsi.yellow("<ENTER>"),
                        JkAnsi.yellow(suggestName))
                        .trim();
                if (response.isEmpty()) {
                    response = suggestName;
                }
            }
            retry = true;
            if (!AppManager.isAppNameValid(response)) {
                JkLog.info("Sorry, application name should only contain alphanumeric characters or '-'.");
                continue;
            }
            if (response.length() > 32) {
                JkLog.info("Sorry, application name should contain between 0 and 32 characters.");
                continue;
            }
            if (systemFiles().contains(response)) {
                JkLog.info("Sorry, application name should not be a jeka system name as %s.",
                        JkAnsi.yellow(systemFiles().toString()));
                continue;
            }
            if (appManager.installedAppNames().contains(response)) {
                JkLog.info("Sorry, the application name `%s` is already used by another app", JkAnsi.yellow(response));
                JkLog.info("Installed programs are:");
                appManager.installedAppNames().forEach(JkLog::debug);
                continue;
            }
            suggestedAppName = response;
            nameOk = true;
        }
        String appName = suggestedAppName;

        // Install built app in target folder
        boolean isNative = find(NativeKBean.class).isPresent();
        appManager.install(appName, repoAndTag, isNative);

        JkLog.info("App has been installed in %s.", appManager.appDir);
        JkLog.info("Run with: %s", JkAnsi.yellow(suggestedAppName));
    }

    @JkDoc("Updates an app from the given name.\n" +
            "Use `name=[app-name]` to specify the app name.")
    public void update() {
        final String appName;
        if (JkUtilsString.isBlank(name)) {
            list();
            appName = JkPrompt.ask("Which app do you want to update?:").trim();
        } else {
            appName = name.trim();
        }
        if (!appManager.installedAppNames().contains(appName)) {
            JkLog.info("No app named `%s` found.", JkAnsi.yellow(appName));
            return;
        }
        Optional<GitTag> optionalGitTag = appManager.getCurrentTag(appName);
        boolean updated;

        boolean isVersion = optionalGitTag.map(GitTag::isVersion).orElse(false);
        if (isVersion) {
            updated = updateToNewerTag(appName, optionalGitTag.get().value);

        // Tag is not a version (as 'latest', 'dev',....) or there is no tag (last commit)
        // We keep the same tag, but update its content
        } else {
            updated = updateTagContentWorkflow(appName, optionalGitTag.orElse(null));
        }

        if (updated) {
            JkLog.info("App %s has been updated.", JkAnsi.yellow(appName));
        }
    }

    @JkDoc("Uninstalls an app from the user's PATH.\n" +
            "Use `name=[app-name]` to specify the app.")
    public void uninstall() {
        final String appName;
        if (JkUtilsString.isBlank(name)) {
            list();
            appName = JkPrompt.ask("Which app do you want to uninstall?:").trim();
        } else {
            appName = name.trim();
        }
        boolean found = appManager.uninstall(appName);
        if (!found) {
            JkLog.warn("No application '%s' found.", appName);
            JkLog.info("Installed applications are:");
            appManager.installedAppNames().forEach(JkLog::info);
        } else {
            JkLog.info("Application %s uninstalled.", JkAnsi.yellow(appName));
        }
    }

    @JkDoc("Lists installed Jeka commands in the user's PATH.")
    public void list() {
        List<String> installedAppNames = appManager.installedAppNames();
        if (installedAppNames.isEmpty()) {
            JkLog.info("No installed app found.");
            return;
        }
        JkColumnText text = JkColumnText.ofSingle(4, 55)  // appName
                .addColumn(15, 70)  // repo url
                .addColumn(3, 10)   // tag
                .addColumn(8, 32)       // update status
                .addColumn(4, 8)       // native
                .setSeparator(JkAnsi.yellow(COLUMN_SEPARATOR));

        JkBusyIndicator.start(JkLog.getOutPrintStream(),"Querying Git repos...");
        text.add(  // Can't use ansi color in table, cause ansi chars  will be truncated.
                "App Name",
                "Repo",
                "Version",
                "Status",
                "Runtime");
        for (String appName : installedAppNames) {
            AppInfo appInfo = appManager.getAppInfo(appName);
            String update = appInfo.updateInfo;
            text.add(appName, appInfo.repoAndTag.repoUrl, appInfo.repoAndTag.tag, update,
                    appInfo.isNative ? "native" : "jvm");
        }
        JkBusyIndicator.stop();
        JkLog.info(JkAnsi.magenta("Installed app:"));
        JkLog.info(text.toString());
    }

    @JkDoc("Adds permanently the url to the trusted list.\n" +
            "The urls starting with the specified prefix will be automatically trusted.\n" +
            "Use 'url=my.host/my.path/' to specify the prefix.")
    public void trustUrl() {
        SecurityChecker.addTrustedUrl(url);
    }



    @JkDoc("List application catalogs")
    public void catalog() {
        if (JkUtilsString.isBlank(name)) {
            Catalogs.print();
            JkLog.info("Run: %s to list apps of a given catalog.",
                    JkAnsi.yellow("jeka app: catalog name=<catalogName>"));
        } else {
            Catalog catalog = Catalogs.getCatalog(name);
            if (catalog == null) {
                JkLog.error("Catalog not found: " + JkAnsi.magenta(name));
                return;
            }
            JkLog.info("Apps referenced in catalog %s:", JkAnsi.magenta(name));
            JkLog.info("");
            catalog.print(name);
        }
    }

    private static List<String> systemFiles() {
        return  JkUtilsIterable.listOf("jeka", "jeka.bat", "jeka-update");
    }

    private boolean checkSecurityFlow(String gitUrl) {
        if (SecurityChecker.isAllowed(gitUrl)) {
            return true;
        }
        String sanitizedUrl = SecurityChecker.parseGitUrl(gitUrl);
        String urlPath = sanitizedUrl;
        if (!sanitizedUrl.endsWith("/")) {
            urlPath += "/";
        }

        JkLog.info("Host/path '%s' is not in present in the trusted list.", JkAnsi.magenta(urlPath), GLOBAL_PROP_FILE);
        String response = JkPrompt.ask("Add? %s:", JkAnsi.yellow("[N,y]")).trim();
        if ("y".equalsIgnoreCase(response)) {
            SecurityChecker.addTrustedUrl(gitUrl);
            return true;
        }
        return false;
    }

    /**
     * @param gitTag Maybe null
     */
    private boolean updateTagContentWorkflow(String appName, GitTag gitTag) {
        if (gitTag == null) {
            JkLog.info("Updating content to last commit.");
        } else {
            JkLog.info("Updating content for tag '%s'.", JkAnsi.magenta(gitTag.value));
        }

        AppManager.UpdateStatus status = appManager.getTagCommitStatus(appName, gitTag);
        if (status == AppManager.UpdateStatus.TAG_DELETED) {
            JkLog.info("Remote tag `%s' has been delete. Existing tags are:", appName);
            appManager.getRemoteTags(appName).forEach(JkLog::info);
            boolean ok = false;
            while (!ok) {
                String userResponse = JkPrompt.ask("Do you want to abort (A), update on the last commit of default branch (D)," +
                        " or choose a tag (T)?").trim().toUpperCase();
                if ("A".equals(userResponse)) {
                    JkLog.info("Update aborted by user.");
                    return false;
                }
                if ("D".equals(userResponse)) {
                    appManager.updateWithTag(appName, null);
                    return true;
                }
                if ("T".equals(userResponse)) {
                    String tag = JkPrompt.ask("Choose a tag from the list above:").trim();
                    appManager.updateWithTag(appName, tag);
                    return true;
                }
            }
        }
        if (status == AppManager.UpdateStatus.UP_TO_DATE) {
            if (gitTag != null) {
                JkLog.info("Version %s is up-to-date. No action needed.", JkAnsi.magenta(gitTag.value));
            } else {
                JkLog.info("Up-to-date. No action needed.");
            }
            return false;
        }
        if (status == AppManager.UpdateStatus.OUTDATED) {
            if (gitTag != null) {
                appManager.updateWithTag(appName, gitTag.value);
                JkLog.info("Updating tag '%s' content.", JkAnsi.magenta(gitTag.value));
            } else {
                appManager.updateToLastCommit(appName);
            }
            return true;
        }
        return true;
    }

    private boolean updateToNewerTag(String appName, String currentTag) {
        TagBucket tagBucket = appManager.getRemoteTagBucketFromAppName(appName);
        List<String> higherVersions = tagBucket.getHighestVersionsThan(currentTag);
        if (!higherVersions.isEmpty()) {
            String highestVersion = tagBucket.getHighestVersion();
            String response = JkPrompt.ask("Found the latest version %s. Update now? [n/Y]", highestVersion).trim();
            if (response.equalsIgnoreCase("n")) {
                JkLog.info("Update aborted by user.", currentTag);
                return false;
            } else {
                JkLog.info("Updating version %s...", highestVersion);
                appManager.updateWithTag(appName, highestVersion);
                return true;
            }
        }
        JkLog.info("No new version available.");
        return false;
    }

}
