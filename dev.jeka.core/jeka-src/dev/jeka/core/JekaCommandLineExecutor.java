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

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for running jeka executables.
 */
public abstract class JekaCommandLineExecutor {

    private final Path jekaShellCmd;

    private Path jacocoAgentPath;

    private Path jacocoReportFile;

    protected JekaCommandLineExecutor() {
        super();
        String relPath = "jeka-output/distrib/bin/" + scriptName();
        Path candidate = Paths.get(relPath).toAbsolutePath().normalize();
        if (Files.exists(candidate)) {
            jekaShellCmd = candidate;
        } else {
            jekaShellCmd = Paths.get("../dev.jeka.core").resolve(relPath).toAbsolutePath().normalize();
            JkUtilsAssert.state(Files.exists(jekaShellCmd), "Cannot find Jeka shell %s", jekaShellCmd);
        }
    }

    public Path getJekaShellPath() {
        return jekaShellCmd;
    }

    public void setJacoco(Path agent, Path report) {
        jacocoAgentPath = agent.toAbsolutePath().normalize();
        jacocoReportFile = report.toAbsolutePath().normalize();
    }

    protected void runWithBaseDirJekaShell(Path baseDir, String cmdLine) {
        runJeka(true, baseDir, cmdLine);
    }

    protected JkProcess prepareWithBaseDirJekaShell(Path baseDir, String cmdLine) {
       return prepareJeka(true, baseDir, cmdLine);
    }

    protected void runWithDistribJekaShell(Path baseDir, String cmdLine) {
        runJeka(false, baseDir, cmdLine);
    }

    protected JkProcess prepareJeka(boolean useBaseDirJeka, Path baseDir, String cmdLine) {
        Path cmd = useBaseDirJeka ? baseDir.resolve(scriptName()) : jekaShellCmd;
        boolean usePowerShell = JkUtilsSystem.IS_WINDOWS && useBaseDirJeka;
        if (usePowerShell) {
            cmd = Paths.get("powershell.exe");
        }
        boolean showOutput = JkLog.isVerbose();
        //boolean showOutput = true;
        String javaVersion = (JkUtilsSystem.IS_MACOS && JkUtilsSystem.getProcessor().isAarch64()) ?
                "11" : "8";
        JkProcess process = JkProcess.of(cmd.toString())
                .setWorkingDir(baseDir)
                .setDestroyAtJvmShutdown(true)
                .setLogCommand(false)
                .setLogWithJekaDecorator(showOutput)
                .setCollectStdout(!showOutput)
                .setCollectStderr(!showOutput)
                .setFailOnError(true)
                .addParamsIf(!cmdLine.contains("-Djeka.java.version="), "-Djeka.java.version=" + javaVersion)
                .addParams(JkUtilsString.parseCommandline(cmdLine))
                //.inheritJkLogOptions()
                .addParams("--stacktrace")


                // set explicitly jeka-core.jar to use, otherwise it may fetch a Jeka version from maven
                // if jeka.properties contains a jeka.version prop, has it happens when scaffolding.
                .setEnv("jeka.distrib.location", jekaShellCmd.toAbsolutePath().getParent().normalize().toString());

        if (usePowerShell) {
            process.addParamsAt(1, baseDir.resolve("jeka.ps1").toString());
        }

        // Add jacoco agent for gathering test coverage info
        if (jacocoAgentPath != null) {
            String arg = "-javaagent:" + jacocoAgentPath + "=destfile=" + jacocoReportFile + ",append=true";
            process.setEnv("JEKA_OPTS", arg);
        }
        return process;
    }

    protected void runJeka(boolean useBaseDirJeka, Path baseDir, String cmdLine) {
        JkLog.startTask("Run in [%s]: jeka %s", baseDir, cmdLine);
        prepareJeka(useBaseDirJeka, baseDir, cmdLine).exec();
        JkLog.endTask();
    }

    private static String scriptName() {
        return JkUtilsSystem.IS_WINDOWS ? "jeka.bat" : "jeka";
    }

}
