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

package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class ScaffoldTester extends JekaCommandLineExecutor {

    void run() {

        // Basic scaffold and checks
        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runWithDistribJekaShell(path, "--version");
        runWithDistribJekaShell(path, "--help");
        scaffoldAndCheckInTemp("base: scaffold", "hello base: info -v -Djeka.java.version=17", true);
        scaffoldAndCheckInTemp("base: scaffold scaffold.kind=APP -vi", "base: test runMain", true);
        scaffoldAndCheckInTemp("project: scaffold scaffold.kind=REGULAR", ": --help", true);

        // Scaffold template=PROPS + layout=SIMPLE
        Path tempDir = scaffoldAndCheckInTemp(
                "project: scaffold.kind=REGULAR layout.style=SIMPLE scaffold",
                "project: pack -v", false);
        JkUtilsAssert.state(Files.exists(tempDir.resolve(JkProject.DEPENDENCIES_TXT_FILE)),
                "dependencies.txt has not been generated");
        JkPathTree.of(tempDir).deleteRoot();

        // Scaffold for Jeka  plugin
        scaffoldAndCheckInTemp(
                "project: scaffold scaffold.kind=PLUGIN",
                "project: pack", true);

        // Check IntelliJ + Eclipse metadata
        Path workingDir = scaffoldAndCheckInTemp(
                "project: scaffold",
                "project: clean pack", false);


        runWithDistribJekaShell(workingDir, "eclipse: files");
        runWithDistribJekaShell(workingDir, "intellij: iml -D" + IntellijKBean.IML_SKIP_MODULE_XML_PROP + "=true");
        JkUtilsAssert.state(Files.exists(workingDir.resolve("src/main/java")),
                "No source tree has been created when scaffolding Java.");
        JkPathTree.of(workingDir).deleteRoot();
    }

    private Path scaffoldAndCheckInTemp(String scaffoldCmdLine, String checkCommandLine, boolean deleteAfter) {
        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runWithDistribJekaShell(path, scaffoldCmdLine);
        runWithDistribJekaShell(path, checkCommandLine);
        //runWithBaseDirJekaShell(path, checkCommandLine);

        if (deleteAfter) {
            JkPathTree.of(path).deleteRoot();
        }
        return path;
    }

}
