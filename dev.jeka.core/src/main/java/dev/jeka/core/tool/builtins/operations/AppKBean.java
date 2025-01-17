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

import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.text.JkColumnText;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.util.List;

@JkDoc("Provides a way to install, update, or remove applications from the user PATH.\n" +
        "Applications are installed from a Git repository and built by the client before installation.\n" +
        "Applications can be installed as executable JARs or native apps.")
public class AppKBean extends KBean {

    private static final PropFile GLOBAL_PROP_FILE = new PropFile(JkLocator.getGlobalPropertiesFile());

    private final AppManager appManager = new AppManager(
            JkLocator.getJekaHomeDir(),
            JkLocator.getCacheDir().resolve("git").resolve("apps"),
            GLOBAL_PROP_FILE);


    @JkDoc("Specifies the URL of the remote Git repository used to install the app." +
            "t can be written as `https://.../my-app#[tag-name]` to fetch a specific tag.")
    public String remote;

    @JkDoc("Specifies the app name.")
    public String name;

    @JkDoc("Build and install the app to make it available in PATH. \n" +
            "Use 'remote=[Git URL]' to set the source repository.\n" +
            "Use 'native:' argument to install as a native app.")
    public void install() {
        String gitUrl = this.remote;
        if (JkUtilsString.isBlank(gitUrl)) {
            JkLog.info("You must specify the git url using 'remote=[Git URL]'.");
            return;
        }
        RepoAndTag repoAndTag = RepoAndTag.ofUrlRef(gitUrl.trim());

        String suggestedAppName = JkUtilsString.substringAfterLast(repoAndTag.repoUrl, "/").toLowerCase();
        if (suggestedAppName.endsWith(".git")) {
            suggestedAppName = JkUtilsString.substringBeforeLast(suggestedAppName, ".git");
        }
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

    @JkDoc("Update an app from the given PATH.\n" +
            "Use `name=[app-name]` to specify the app.")
    public void update() {
        if (JkUtilsString.isBlank(name)) {
            JkLog.info("You must specify the argument name=[applicationName].");
            return;
        }
        AppManager.UpdateStatus status = appManager.update(name);
        if (status == AppManager.UpdateStatus.FLAG_DELETED) {
            JkLog.info("Existing tags are:", name);
            appManager.getRemoteTags(name).forEach(JkLog::info);
            boolean ok = false;
            while (!ok) {
                String userResponse = JkPrompt.ask("Do you want to abort (A), update on the default branch (D)," +
                        " or choose a tag (T)?").trim().toUpperCase();
                if ("A".equals(userResponse)) {
                    JkLog.info("Update aborted by user.");
                    return;
                }
                if ("D".equals(userResponse)) {
                    status = appManager.updateWithTag(name, null);
                    if (status != AppManager.UpdateStatus.FLAG_DELETED) {
                        ok = true;
                    }
                }
                if ("T".equals(userResponse)) {
                    String tag = JkPrompt.ask("Choose a tag from the list above:").trim();
                    status = appManager.updateWithTag(name, tag);
                    if (status != AppManager.UpdateStatus.FLAG_DELETED) {
                        ok = true;
                    }
                }
            }
        }
        if (status == AppManager.UpdateStatus.OUTDATED) {
            JkLog.info("Application %s has been updated.", name);
        } else if (status == AppManager.UpdateStatus.UP_TO_DATE) {
            JkLog.info("Application %s is already up-to-date.", name);
        }
    }

    @JkDoc("Uninstalls an app from the user's PATH.\n" +
            "Use `name=[app-name]` to specify the app.")
    public void remove() {
        if (JkUtilsString.isBlank(name)) {
            JkLog.info("You must specify the argument name=[applicationName].");
        }
        boolean found = appManager.remove(name);
        if (!found) {
            JkLog.warn("No application '%s' found.", name);
            JkLog.info("Installed applications are:");
           appManager.installedAppNames().forEach(JkLog::info);
        } else {
            JkLog.info("Application %s uninstalled.", name);
        }
    }

    @JkDoc("Lists installed Jeka commands in the user's PATH.")
    public void list() {
        List<String> installedAppNames = appManager.installedAppNames();
        JkColumnText text = JkColumnText.ofSingle(4, 25)  // appName
                .addColumn(15, 50)  // repo url
                .addColumn(3, 10)   // tag
                .addColumn(8, 14)       // update status
                .addColumn(4, 8)       // native
                .setSeparator(" │ ");

        JkBusyIndicator.start(JkLog.getOutPrintStream(),"Querying Git repos...");
        for (String appName : installedAppNames) {
            AppInfo appInfo = appManager.getAppInfo(appName);
            String update = appInfo.updateStatus == AppManager.UpdateStatus.FLAG_DELETED ? "?" :
                    appInfo.updateStatus.toString().toLowerCase().replace("_", "-");
            text.add(appName, appInfo.repoAndTag.repoUrl, appInfo.repoAndTag.tag, update,
                    appInfo.isNative ? "native" : "jvm");
        }
        JkBusyIndicator.stop();
        JkLog.info("Installed app:");
        System.out.println(text);
        JkLog.info("%s found.", JkUtilsString.pluralize(installedAppNames.size(), "app"));
    }

    @JkDoc("Display some example on the console that you can play with.")
    public void examples() {
        JkColumnText columnText = JkColumnText
                .ofSingle(10, 110)   // repo url
                .addColumn(5, 15)    // app type
                .addColumn(5, 88)    // desc
                .addColumn(3, 80)
                .setSeparator(" │ ");
        String nativ = "allow native";
        columnText
                .add("https://github.com/djeang/demo-dir-checksum#latest", "CLI",
                        "Computes folder checksums on your computer.", nativ)
                .add("https://github.com/djeang/Calculator-jeka", "Swing GUI",
                        "Swing GUI providing a calculator", "")
                .add("https://github.com/jeka-dev/demo-cowsay", "CLI",
                        "Java port or the Cowsay famous CLI.", nativ)
                .add("https://github.com/jeka-dev/demo-build-templates-consumer.git", "Server GUI",
                        "A pringboot app with reactJS front-end to manage coffee shops.", nativ)
                .add("https://github.com/jeka-dev/working-examples/tree/master/springboot-kotlin-reactjs",
                        "Server GUI", "Same as previous but written in Kotlin and React Js.", nativ);
        System.out.println(columnText);
        System.out.println();
        System.out.println("To install an application: jeka app: install remote=https://github.com/jeka-dev/demo-cowsay");
        System.out.println("To execute directly without installing: jeka -r https://github.com/djeang/Calculator-jeka --program");


    }

    private static List<String> systemFiles() {
        return  JkUtilsIterable.listOf("jeka", "jeka.bat", "jeka-update");
    }

}
