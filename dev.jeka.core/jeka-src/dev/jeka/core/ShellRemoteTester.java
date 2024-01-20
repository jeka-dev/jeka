package dev.jeka.core;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ShellRemoteTester  {

    public static final String GIT_URL = "https://github.com/jeka-dev/sample-for-integration-test.git#0.0.1";


    void run() {
        String scriptName = JkUtilsSystem.IS_WINDOWS ? "jeka.bat" : "jeka";
        Path distribPath = JkLocator.getJekaJarPath().getParent().normalize();
        Path cmd = distribPath.resolve(scriptName).normalize();

        // Test without alias
        JkProcResult result = JkProcess.of(cmd.toString(), "-r", GIT_URL, "-lna", "#ok")
                .setLogCommand(true)
                .setCollectOutput(true)
                .setEnv("jeka.distrib.location", distribPath.toString())
                .exec();
        JkUtilsAssert.state( "ok".equals(result.getOutput()), "Command output was %s", result.getOutput());




    }
}
