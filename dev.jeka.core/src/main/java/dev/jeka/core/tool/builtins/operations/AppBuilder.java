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
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class AppBuilder {

    static final String SHE_BANG = "#!/bin/sh";

    static Path build(Path baseDir, boolean isNative) {
        String[] buildArgs = buildArgs(baseDir, isNative);
        JkLog.verbose("Use commands: %s", String.join(" ", buildArgs));
        JkProcess.ofWinOrUx("jeka.bat", "jeka")
                .setWorkingDir(baseDir)
                .addParams(buildArgs)
                .addParamsIf(JkLog.isVerbose(), "--verbose")
                //.addParamsIf(JkLog.isDebug(),"--debug")
                .setInheritIO(true)
                .exec();

        Path buildDir = baseDir.resolve("jeka-output");

        // Find the executable or jar built artefact
        if (isNative) {
            if (JkUtilsSystem.IS_WINDOWS) {
                return JkUtilsPath.listDirectChildren(buildDir).stream()
                        .filter(path -> path.toString().endsWith(".exe"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Cannot find exe in directory"));
            }
            return JkUtilsPath.listDirectChildren(buildDir).stream()
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.toString().endsWith(".jar"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Cannot find exe in directory"));
        }

        // Create shell/bat wrapper files
        Path jarFile = JkUtilsPath.listDirectChildren(buildDir).stream()
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".jar"))
                .findFirst().orElseThrow(() ->
                        new IllegalStateException("Cannot find jar file in directory " + buildDir));
        if (JkUtilsSystem.IS_WINDOWS) {
            return createBatFile(baseDir, jarFile);
        }
        return createShellFile(baseDir, jarFile);
    }

    private static Path createBatFile(Path baseDir, Path jarPath) {
        final StringBuilder shellContent = new StringBuilder();
        shellContent.append("@echo off").append("\n");
        shellContent.append("jeka -r \"").append(baseDir).append("\" -p %*\n");
        String fileName = jarPath.getFileName().toString().replace(".jar", ".bat");
        Path batFile = jarPath.resolveSibling(fileName);
        return JkPathFile.of(batFile).write(shellContent.toString()).get();
    }

    private static Path createShellFile(Path baseDir, Path jarPath) {
        final StringBuilder shellContent = new StringBuilder();
        shellContent.append(SHE_BANG).append("\n");
        shellContent.append("jeka -r \"").append(baseDir).append("\" -p $@\n");
        String fileName = jarPath.getFileName().toString().replace(".jar", ".sh");
        Path shellFile = jarPath.resolveSibling(fileName);
        return JkPathFile.of(shellFile)
                .write(shellContent.toString())
                .setPosixExecPermissions()
                .get();
    }

    private static String[] buildArgs(Path base, boolean nativeCompile) {
        Path jekaProperties = base.resolve(JkConstants.PROPERTIES_FILE);
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
                if (Files.exists(base.resolve("src"))) {  // this is a project
                    args.add("project:");
                    args.add("pack");
                    args.add("pack.jarType=FAT");
                    args.add("pack.detectMainClass=true");
                } else {
                    args.add("base:");
                    args.add("pack");
                }

            }
        }
        args.add("-Djeka.test.skip=true");
        return args.toArray(new String[0]);
    }

}
