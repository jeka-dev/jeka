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
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

class AppBuilder {

    private  static final String PROGRAM_BUILD_PROP = "jeka.program.build";

    private  static final String PROGRAM_BUILD_NATIVE_PROP = "jeka.program.build.native";

    private  static final String PROGRAM_BUILD_BUNDLE_PROP = "jeka.program.build.bundle";

    private  static final String PROGRAM_BUNDLE_DIST_PROP = "jeka.program.bundle.dist";

    static final String SHE_BANG = "#!/bin/sh";

    static Path build(Path baseDir, RuntimeMode runtimeMode) {
        String[] buildArgs = buildArgs(baseDir, runtimeMode);
        JkLog.verbose("Use commands: %s", String.join(" ", buildArgs));
        JkProcess.ofWinOrUx("jeka.bat", "jeka")
                .setWorkingDir(baseDir)
                .addParams(buildArgs)
                .addParamsIf(JkLog.isVerbose(), "--verbose")
                .addParamsIf(JkLog.isDebug(),"--debug")
                .setInheritIO(true)
                .exec();

        Path buildDir = baseDir.resolve("jeka-output");
        if (!Files.exists(buildDir)) {
            throw new IllegalStateException("Build directory does not exist: " + buildDir
                    + ". Make sure that the application build has been executed successfully.");
        }

        // Find the executable or jar built artifact
        final Path result;
        if (runtimeMode == RuntimeMode.NATIVE) {
            if (JkUtilsSystem.IS_WINDOWS) {
                result = findFirst(buildDir, ".exe");
            } else {
                result = JkUtilsPath.listDirectChildren(buildDir).stream()
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.toString().endsWith(".jar"))
                        .findFirst().orElseThrow(() -> new IllegalStateException(
                                "Cannot find executable file in " + baseDir));
            }

        } else if (runtimeMode == RuntimeMode.BUNDLE) {
            Path propertyFile= baseDir.resolve(JkConstants.PROPERTIES_FILE);
            String dist = JkProperties.ofFile(propertyFile).get(PROGRAM_BUNDLE_DIST_PROP);

            if (JkUtilsSystem.IS_MACOS) {
                Path distDir = JkUtilsString.isBlank(dist) ? buildDir : baseDir.resolve(dist);
                result = findFirst(distDir, ".dmg");

            } else if (JkUtilsSystem.IS_WINDOWS) {
                if (!JkUtilsString.isBlank(dist)) {
                    result = baseDir.resolve(dist);
                } else {
                    result = findExecutableParent(baseDir);
                }
            } else  {
                throw new IllegalStateException("Cannot manage bundle on this system (only Windows or MACOS)");
            }

            // runtimeMode = JVM
        } else {
            // Create shell/bat wrapper files
            Path jarFile = findFirst(buildDir, ".jar");
            if (JkUtilsSystem.IS_WINDOWS) {
                result = createBatFile(baseDir, jarFile);
            } else {
                result = createShellFile(baseDir, jarFile);
            }
        }
        return result;
    }

    private static Path findFirst(Path dir, String ext) {
        return JkUtilsPath.listDirectChildren(dir).stream()
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(ext))
                .findFirst().orElseThrow(() ->
                        new IllegalStateException("Cannot find " + ext + " file in directory " + dir));
    }

    private static Path createBatFile(Path baseDir, Path jarPath) {
        String shellContent = "@echo off" + "\n" +
                "jeka -r \"" + baseDir + "\" -p %*\n";
        String fileName = jarPath.getFileName().toString().replace(".jar", ".bat");
        Path batFile = jarPath.resolveSibling(fileName);
        return JkPathFile.of(batFile).write(shellContent).get();
    }

    private static Path createShellFile(Path baseDir, Path jarPath) {
        String shellContent = SHE_BANG + "\n" +
                "jeka -r \"" + baseDir + "\" -p $@\n";
        String fileName = jarPath.getFileName().toString().replace(".jar", ".sh");
        Path shellFile = jarPath.resolveSibling(fileName);
        return JkPathFile.of(shellFile)
                .write(shellContent)
                .setPosixExecPermissions()
                .get();
    }

    private static String[] buildArgs(Path base, RuntimeMode runtimeMode) {
        Path jekaProperties = base.resolve(JkConstants.PROPERTIES_FILE);
        List<String> args = new LinkedList<>();
        if (Files.exists(jekaProperties)) {

            String buildCmd = chooseSpecificBuildCommand(JkProperties.ofFile(jekaProperties), runtimeMode);
            if (!JkUtilsString.isBlank(buildCmd)) {
                args.addAll(JkUtilsString.parseCommandlineAsList(buildCmd));
            }
        }
        if (args.isEmpty()) {
            if (runtimeMode == RuntimeMode.NATIVE) {
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
            if (runtimeMode == RuntimeMode.BUNDLE) {
                args.add("bundle:");
                args.add("pack");
                if (JkUtilsSystem.IS_WINDOWS) {

                    // The default type requires specific installation tools present on the local machine.
                    args.add("jpackage.options.windows.--type=app-image");
                }
            }
        }
        args.add("-Djeka.test.skip=true");
        args.add("--clean");
        args.add("--clean-work");
        args.add("--duration");
        return args.toArray(new String[0]);
    }

    private static String chooseSpecificBuildCommand(JkProperties properties, RuntimeMode runtimeMode) {
        if (runtimeMode == RuntimeMode.NATIVE) {
            String buildNative = properties.get(PROGRAM_BUILD_NATIVE_PROP);
            if (!JkUtilsString.isBlank(buildNative)) {
                return buildNative;
            } else {
                String build = properties.get(PROGRAM_BUILD_PROP);
                if (!JkUtilsString.isBlank(build)) {
                    return build + " native: compile";
                }
            }
        } else if (runtimeMode == RuntimeMode.BUNDLE) {
            String build = properties.get(PROGRAM_BUILD_BUNDLE_PROP);
            if (JkUtilsString.isBlank(build)) {
                build = properties.get(PROGRAM_BUILD_PROP);
            }
            if (!JkUtilsString.isBlank(build)) {
                return build;
            }
        } else {
            String build = properties.get(PROGRAM_BUILD_PROP);
            if (!JkUtilsString.isBlank(build)) {
                return build;
            }
        }
        return null;
    }

    private static Path findExecutableParent(Path baseDir) {
        return JkUtilsPath.walk(baseDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".exe"))
                .map(Path::getParent)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot find any .exe file in " + baseDir));
    }

}
