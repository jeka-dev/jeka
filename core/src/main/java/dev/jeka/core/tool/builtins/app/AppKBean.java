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

@JkDoc("Provides a way to install, update, or remove applications from the user PATH.\n" +
        "Applications are installed from a Git repository and built by the client before installation.\n" +
        "Applications can be installed as executable JARs or native apps.")
@JkDocUrl("https://jeka-dev.github.io/jeka/reference/kbeans-app/")
public class AppKBean extends KBean {

    private static final PropFile GLOBAL_PROP_FILE = new PropFile(JkLocator.getGlobalPropertiesFile());

    private final AppManager appManager = new AppManager(
            JkLocator.getJekaHomeDir(),
            JkLocator.getCacheDir().resolve("git").resolve("apps"));

    private static final String COLUMN_SEPARATOR = JkUtilsSystem.IS_WINDOWS ? " | " : " │ ";

    @JkDoc("Git Remote repository URL of the app to install.")
    public String repo;

    @JkDoc("Specifies the name of the app to update/uninstall.")
    public String name;

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

        List<AppManager.AppVersion> installedAppsForRepo = appManager.getAppVersionsForRepo(remoteUrl);
        if (!installedAppsForRepo.isEmpty()) {
            JkLog.info("This repository has been already installed for following apps:");
            installedAppsForRepo.forEach(System.out::println);
            String response = JkPrompt.ask("Do you want to install another version? [N,y]:").trim();
            if (!response.equalsIgnoreCase("y")) {
                JkLog.info("Installation aborted by user.");
                return;
            }
        }

        String suggestedAppName = JkUtilsString.substringAfterLast(remoteUrl, "/").toLowerCase();
        if (suggestedAppName.endsWith(".git")) {
            suggestedAppName = JkUtilsString.substringBeforeLast(suggestedAppName, ".git");
        }

        // Ask for tag/version
        JkBusyIndicator.start(JkLog.getOutPrintStream(), "Fetching info from Git");
        String branch = appManager.getRemoteDefaultBranch(remoteUrl);
        TagBucket tagBucket = appManager.getRemoteTagBucketFromGitUrl(remoteUrl);
        JkBusyIndicator.stop();

        boolean allowed = checkSecurityFlow(gitUrl);
        if (!allowed) {
            JkLog.info("Installation aborted by user.");
            return;
        }

        final RepoAndTag repoAndTag;

        // No tag available
        if (tagBucket.tags.isEmpty()) {
            JkLog.info("No tags found in remote Git repository. Last commit from branch %s will be installed.", branch);
            repoAndTag = new RepoAndTag(remoteUrl, null);

            // 1 tag available
        } else if (tagBucket.tags.size() == 1) {
            String tag = tagBucket.tags.get(0).getName();
            JkLog.info("Found one tag '%s' in the remote Git repository.", tag);
            String response = JkPrompt.ask("Do you want to install the tag '%s' or latest commit on branch %s:? " +
                    "[@ = last commit, ENTER = '%s' tag]:", tag, branch, tag);
            if ("@".equals(response)) {
                JkLog.info("Last commit from branch %s will be installed.", branch);
                repoAndTag = new RepoAndTag(remoteUrl, null);
            } else {
                JkLog.info("Version %s will be installed.", tag);
                repoAndTag = new RepoAndTag(remoteUrl, tag);
            }

            // many tags available
        } else {
            JkLog.info("List of available tags:");
            tagBucket.tags.forEach(System.out::println);
            String highest = tagBucket.getHighestVersion();
            String chooseTag = null; // empty mean choose last commit
            while (chooseTag == null) {
                String response = JkPrompt.ask("Enter the version to install [@ = last commit /" +
                        " ENTER = '%s' tag]:", highest).trim();
                if (JkUtilsString.isBlank(response)) {
                    JkLog.info("Version %s will be installed.", highest);
                    chooseTag = highest;
                } else if (response.equalsIgnoreCase("@")) {
                    JkLog.info("Last commit from branch %s will be installed.", branch);
                    chooseTag = "";
                } else {
                    if (tagBucket.hasTag(response)) {
                        JkLog.info("Version %s will be installed.", response);
                        chooseTag = response;
                    } else {
                        JkLog.info("The tag '%s' is not on the list. Please, choose one from the list.", response);
                    }
                }
            }
            repoAndTag = new RepoAndTag(remoteUrl, chooseTag);
        }

        // Ask for name
        boolean nameOk = false;
        boolean retry = false;
        while(!nameOk) {
            String response;
            if (appManager.installedAppNames().contains(suggestedAppName)) {
                if (!retry) {
                    JkLog.info("An application named '%s' is already installed.", suggestedAppName);
                }
                response = JkPrompt.ask("Choose a new name for the application:").trim();
            } else {
                String suggestName = appManager.suggestName(suggestedAppName);
                response = JkPrompt.ask("Choose a name for tha application. Press ENTER to select '%s':",
                        suggestName).trim();
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
                JkLog.info("Sorry, application name should not be a jeka system name as %s.", systemFiles());
                continue;
            }
            if (appManager.installedAppNames().contains(response)) {
                JkLog.info("Sorry, the application name `%s` is already used by another app", response);
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

        JkLog.info("%s is installed.", suggestedAppName);
    }

    @JkDoc("Updates an app from the given PATH.\n" +
            "Use `name=[app-name]` to specify the app.")
    public void update() {
        final String appName;
        if (JkUtilsString.isBlank(name)) {
            list();
            appName = JkPrompt.ask("Which app do you want to update?:").trim();
        } else {
            appName = name.trim();
        }
        if (!appManager.installedAppNames().contains(appName)) {
            JkLog.info("No app named `%s` found.", appName);
            return;
        }
        String currentTag = appManager.getCurrentTag(appName);
        boolean updated;

        // Tag is not a version (as 'latest', 'dev',....)
        // We keep the same tag, but update its content
        if (new GitTag(currentTag).isLVersion()) {
            updated = updateToNewerTag(appName, currentTag);
        } else {
            updated = updateTagContentWorkflow(appName, currentTag);
        }
        if (updated) {
            JkLog.info("App %s has been updated.", appName);
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
            JkLog.info("Application %s uninstalled.", appName);
        }
    }

    @JkDoc("Lists installed Jeka commands in the user's PATH.")
    public void list() {
        List<String> installedAppNames = appManager.installedAppNames();
        if (installedAppNames.isEmpty()) {
            JkLog.info("No installed app found.");
            return;
        }
        JkColumnText text = JkColumnText.ofSingle(4, 25)  // appName
                .addColumn(15, 70)  // repo url
                .addColumn(3, 10)   // tag
                .addColumn(8, 32)       // update status
                .addColumn(4, 8)       // native
                .setSeparator(COLUMN_SEPARATOR);

        JkBusyIndicator.start(JkLog.getOutPrintStream(),"Querying Git repos...");
        for (String appName : installedAppNames) {
            AppInfo appInfo = appManager.getAppInfo(appName);
            String update = appInfo.updateInfo;
            text.add(appName, appInfo.repoAndTag.repoUrl, appInfo.repoAndTag.tag, update,
                    appInfo.isNative ? "native" : "jvm");
        }
        JkBusyIndicator.stop();
        JkLog.info("Installed app:");
        JkLog.info(text.toString());
    }

    @JkDoc("Adds permanently the url to the trusted list.\n" +
            "The urls starting with the specified prefix will be automatically trusted.\n" +
            "Use 'url=my.host/my.path/' to specify the prefix.")
    public void trustUrl() {
        SecurityChecker.addTrustedUrl(url);
    }

    @JkDoc("Displays some examples on the console that you can play with.")
    public void examples() {
        JkColumnText columnText = JkColumnText
                .ofSingle(10, 110)   // repo url
                .addColumn(5, 15)    // app type
                .addColumn(5, 88)    // desc
                .addColumn(3, 80)
                .setSeparator(COLUMN_SEPARATOR);
        String nativ = "allow native";
        columnText
                .add("https://github.com/jeka-dev/demo-cowsay", "CLI",
                        "Java port or the Cowsay famous CLI.", nativ)
                .add("https://github.com/djeang/demo-dir-checksum", "CLI",
                        "Computes folder checksums on your computer.", nativ)
                .add("https://github.com/djeang/Calculator-jeka", "Swing GUI",
                        "Swing GUI providing a calculator", "")
            //    .add("https://github.com/jeka-dev/demo-build-templates-consumer.git", "Server GUI",
              //          "A pringboot app with reactJS front-end to manage coffee shops.", nativ)
                .add("https://github.com/jeka-dev/demo-project-springboot-angular",
                        "Server UI", "Manage a list of users. Written in Springboot and ReactJs.", nativ)
                .add("https://github.com/jeka-dev/demo-maven-jeka-quarkus.git",
                        "Server UI", "Manage a basket of fruit. Written with Quarkus, built with Maven.", nativ);

        System.out.println(columnText);
        System.out.println();
        columnText = JkColumnText
                        .ofSingle(10, 40)
                        .addColumn(10, 80)
                        .setSeparator("    ");
        columnText.add("To install an app",
                "jeka app: install repo=https://github.com/jeka-dev/demo-cowsay");
        columnText.add("To install a native app",
                "jeka app: install repo=https://github.com/jeka-dev/demo-cowsay native:");
        columnText.add("To execute directly without installing",
                "jeka -r https://github.com/djeang/Calculator-jeka --program");
        System.out.println(columnText);
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

        JkLog.info("Host/path '%s' is not in present in the trusted list.", urlPath, GLOBAL_PROP_FILE);
        String response = JkPrompt.ask("Add? [N,y]:").trim();
        if ("y".equalsIgnoreCase(response)) {
            SecurityChecker.addTrustedUrl(gitUrl);
            return true;
        }
        return false;
    }

    private boolean updateTagContentWorkflow(String appName, String currentTag) {
        JkLog.info("Updating content of tag '%s'.", currentTag);
        AppManager.UpdateStatus status = appManager.getTagCommitStatus(appName, currentTag);
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
            JkLog.info("Version %s is up-to-date. No action needed.");
            return false;
        }
        if (status == AppManager.UpdateStatus.OUTDATED) {
            appManager.updateWithTag(appName, currentTag);
            JkLog.info("Updating tag %s content.", currentTag);
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
