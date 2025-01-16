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
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.scaffold.JkScaffold;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.nativ.NativeKBean;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JkDoc("Provides convenient methods to perform admin tasks.")
public class OperationsKBean extends KBean {

    @JkDoc("Argument for method 'addGlobalProp'.")
    public String content;

    @JkDoc("Open a file explorer window on JeKA user home dir.")
    public void openHomeDir() throws IOException {
        Desktop.getDesktop().open(JkLocator.getJekaUserHomeDir().toFile());
    }

    @JkDoc("Edit global.properties file.")
    public void editGlobalProps() throws IOException {
        Path globalProps = JkLocator.getGlobalPropertiesFile();
        JkPathFile.of(globalProps).createIfNotExist();
        if (!GraphicsEnvironment.isHeadless()) {
            if (JkUtilsSystem.IS_WINDOWS) {
                JkProcess.of("notepad", globalProps.toString()).exec();
            } else {
                Desktop.getDesktop().edit(globalProps.toFile());
            }
        } else if (!JkUtilsSystem.IS_WINDOWS) {
            JkProcess.of("nano", globalProps.toString())
                    .setInheritIO(true)
                    .exec();
        }
    }

    @JkDoc("Adds a shorthand to the global properties file. " +
            "Use 'content=[shorthand-name]=[shorthand content]' argument," +
            " as 'jeka operations: addShorthand content=build=project: pack sonarqube: run'." )
    public void addShorthand() {
        if (JkUtilsString.isBlank(content) || !content.contains("=")) {
            JkLog.info("You must specify the shorthand using 'content=[shorthand-name]=[shorthand content].");
        } else {
            Path globalProps = JkLocator.getGlobalPropertiesFile();
            String name = JkUtilsString.substringBeforeFirst(content, "=").trim();
            String value = JkUtilsString.substringAfterFirst(content, "=").trim();
            String propName = "jeka.cmd." + name;
            insertProp(globalProps, propName, value);
        }

    }

    @JkDoc("Creates or replaces jeka.ps1 and jeka bash script in the current directory .%n" +
            "The running JeKa version is used for defining jeka scripts version to be created.")
    public void updateLocalScripts() {
        JkScaffold.createShellScripts(getBaseDir());
    }

    @JkDoc("Build and install the app to make it accessible from PATH. " +
            "Use 'content=[Git URL]' to specify the source repository.")
    public void installApp() {
        String gitUrl = this.content;
        if (JkUtilsString.isBlank(gitUrl)) {
            JkLog.info("You must specify the git url using 'content=[Git URL].");
            return;
        }
        gitUrl = gitUrl.trim();
        String urlPath = urlToPathName(gitUrl);
        Path cacheDir = JkLocator.getCacheDir().resolve("git").resolve(urlPath);
        JkUtilsPath.deleteDir(cacheDir, true);  // on windows some files in .git dir cannot be removed
        JkUtilsPath.createDirectories(cacheDir);

        String repo = gitUrl;
        String tag = "";
        if (gitUrl.contains("#")) {
            repo = JkUtilsString.substringBeforeLast(gitUrl, "#");
            tag = JkUtilsString.substringAfterLast(gitUrl, "#");
        }

        // check app name
        String appName = JkUtilsString.substringAfterLast(repo, "/").toLowerCase();
        if (appName.endsWith(".git")) {
            appName = JkUtilsString.substringBeforeLast(appName, ".git");
        }
        if (systemFiles().contains(appName)) {
            JkLog.error("%s is a system application, ", appName);
            return;
        }

        // clone git repo
        // -- cannot clone on an non-empty dir. On windows some files won't be deleted
        // so we clone in a temp dir prior moving to target dir
        JkLog.info("Cloning %s ...", repo);
        Path tempDir = JkUtilsPath.createTempDirectory("jk-git-clone-");
        JkGit.of(cacheDir)
                .addParams("clone", "--quiet", "-c",  "advice.detachedHead=false", "--depth", "1" )
                .addParamsIf(!tag.isEmpty(), "--branch", tag)
                .addParams(repo, tempDir.toString())
                .exec();
        JkUtilsPath.move(tempDir, cacheDir, StandardCopyOption.REPLACE_EXISTING);

        // build app
        JkLog.info("Build application ...");
        boolean buildNative = find(NativeKBean.class).isPresent();
        JkProcess.ofWinOrUx("jeka.bat", "jeka")
                .setWorkingDir(cacheDir)
                .addParams(buildArgs(cacheDir, buildNative))
                .addParamsIf(JkLog.isVerbose(),"--verbose")
                .addParamsIf(JkLog.isDebug(),"--debug")
                .setInheritIO(true)
                .exec();

        Path buildDir = cacheDir.resolve("jeka-output");
        if (buildNative) {
            final Path exec;
            if (JkUtilsSystem.IS_WINDOWS) {
                exec = JkUtilsPath.listDirectChildren(buildDir).stream()
                        .filter(path -> path.toString().endsWith(".exe"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Cannot find exe in directory"));
                JkUtilsPath.copy(exec, JkLocator.getJekaHomeDir().resolve(appName + ".exe"), StandardCopyOption.REPLACE_EXISTING);
            } else {
                exec = JkUtilsPath.listDirectChildren(buildDir).stream()
                        .filter(path -> Files.isRegularFile(path))
                        .filter(path -> !path.toString().endsWith(".jar"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Cannot find exe in directory"));
                JkPathFile.of(exec).setPosixExecPermissions();
                JkUtilsPath.copy(exec, JkLocator.getJekaHomeDir().resolve(appName), StandardCopyOption.REPLACE_EXISTING);
            }

        } else {
            final StringBuilder shellContent = new StringBuilder();
            final String fileName;
            if (JkUtilsSystem.IS_WINDOWS) {
                shellContent.append("@echo off").append("\n");
                shellContent.append("jeka -r ").append(gitUrl).append(" -p %*\n");
                fileName = appName + ".bat";
            } else {
                shellContent.append("#!/bin/sh").append("\n");
                shellContent.append("jeka -r ").append(gitUrl).append(" -p $@\n");
                fileName = appName;
            }
            JkPathFile cmdFile = JkPathFile.of(JkLocator.getJekaHomeDir().resolve(fileName))
                    .deleteIfExist()
                    .createIfNotExist()
                    .write(shellContent.toString());
            if (!JkUtilsSystem.IS_WINDOWS) {
                cmdFile.setPosixExecPermissions();
            }
        }
        JkLog.info("%s is installed.", appName);
    }

    @JkDoc("List the installed jeka commands in user PATH.")
    public void listApps() {
        JkLog.info("Installed commands:");
        List<String> cmds = installedPrograms();
        cmds.forEach(System.out::println);
        JkLog.info("%s found.", JkUtilsString.pluralize(cmds.size(), "command"));
    }

    private static void insertProp(Path file, String propKey, String propValue) {
        Path globalProps = JkLocator.getGlobalPropertiesFile();
        JkUtilsPath.createFileSafely(globalProps);
        Properties properties = JkUtilsPath.readPropertyFile(file);
        boolean update = false;
        if (properties.containsKey(propKey)) {
            update = true;
            String existingValue = properties.getProperty(propKey);
            JkLog.warn("Property %s=%s already exists in %s.", propKey, existingValue, file.getFileName());
            String answer =
                    JkPrompt.ask("Do you want to update ? [y/N]");
            if (!"y".equals(answer.toLowerCase())) {
                JkLog.info("Property %s not updated.", propKey);
                return;
            }
        }
        String line = propKey + "=" + propValue;
        // find prefix
        if (propKey.contains(".")) {
            String prefix = JkUtilsString.substringBeforeLast(propKey, ".");
            insertBeforeFirst(globalProps, prefix, line);
        } else {
            JkPathFile.of(globalProps).write("\n" + line, StandardOpenOption.APPEND);
        }
        JkLog.info("Property %s=%s %s.", propKey, propValue, update ? "updated" : "added");
    }

    private static void insertBeforeFirst(Path file, String prefix, String lineToInsert) {
        List<String> lines = new LinkedList<>(JkUtilsPath.readAllLines(file));
        int lastMatchingIndex = -1;
        int i=0;
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                lastMatchingIndex = i;
            }
            i++;
        }
        for (int index=lastMatchingIndex; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.trim().endsWith("\\")) {
                lastMatchingIndex++;
            } else {
                break;
            }
        }
        if (lastMatchingIndex == -1) {
            lines.add(lineToInsert);
        } else if (lastMatchingIndex + 1 > lines.size() ) {
            lines.add(lineToInsert);
        } else {
            lines.add(lastMatchingIndex + 1, lineToInsert);
        }
        JkPathFile.of(file).write(java.lang.String.join( "\n",lines));
    }

    private static String urlToPathName(String url) {
        String[] protocols = {"https://", "ssh://", "git://", "git@"};
        String trimmedUrl = url;
        for (String protocol : protocols) {
            trimmedUrl = trimmedUrl.replaceAll(Pattern.quote(protocol), "");
        }
        return trimmedUrl.replace("/", "_");
    }

    private static String[] buildArgs(Path base, boolean nativeCompile) {
        Path jekaProperties = base.resolve("jeka.properties");
        List<String> args = new LinkedList<>();
        if (Files.exists(jekaProperties)) {
            String buildCmd = JkProperties.ofFile(jekaProperties).get("jeka.program.build");
            if (!JkUtilsString.isBlank(buildCmd)) {
                args = Arrays.asList(JkUtilsString.parseCommandline(buildCmd));
                if (nativeCompile && !args.contains("native:")) {
                    args.add("native:");
                    args.add("compile");
                }
            }
        }
        if (args.isEmpty()) {
            if (nativeCompile) {
                args.add("native:");
                args.add("compile");
            } else {
                String kbeanName = Files.exists(base.resolve("src")) ? "project:" : "base:";
                args.add(kbeanName);
                args.add("pack");
                args.add("pack.jarType=FAT");
                args.add("pack.detectMainClass=true");
            }
        }
        args.add("-Djeka.test.skip=true");
        return args.toArray(new String[0]);
    }

    private static List<String> installedPrograms() {
        Path home = JkLocator.getJekaHomeDir();
        return JkUtilsPath.listDirectChildren(home).stream()
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().endsWith(".jar"))
                .filter(path -> !path.toString().endsWith(".ps1"))
                .filter(path -> !systemFiles().contains(path.getFileName().toString()))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }

    private static List<String> systemFiles() {
        return  JkUtilsIterable.listOf("jeka", "jeka.bat", "jeka-update");
    }

}
