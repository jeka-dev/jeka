package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ShellRemoteTester  extends JekaCommandLineExecutor {

    private static final String GIT_URL = "https://github.com/jeka-dev/sample-for-integration-test.git#0.0.1";

    private static final String SNAPSHOT_REPO = "https://oss.sonatype.org/content/repositories/snapshots";

    private static final String SNAPSHOT_VERSION = "0.11.x-SNAPSHOT";

    void run() {
        testWithRemoteGitHttp();
        testWithSnapshotDistribVersion();
        testWithSpecificJavaVersion();
    }

    private void testWithRemoteGitHttp() {
        JkLog.info("============ Testing 'jeka -r %s #ok =============================", GIT_URL);
        Path jekaShellPath = getJekaShellPath();

        // Test without alias
        JkProcResult result = JkProcess.of(jekaShellPath.toString(), "-rc", GIT_URL, "-lna", "#ok")
                .setLogCommand(true)
                .setCollectOutput(true)
                .setEnv("jeka.distrib.location", jekaShellPath.getParent().toString())
                .exec();
        String output = result.getOutput();
        JkUtilsAssert.state( output.endsWith("ok\n"), "Command output was '%s', expecting ending with 'ok'", output);
    }

    private void testWithSnapshotDistribVersion() {
        JkLog.info("============ Testing snapshot jeka version %s =============================", SNAPSHOT_VERSION);

        // Delete cached distrib to force reloading
        Path cachedDistrib = JkLocator.getCacheDir().resolve("distributions").resolve(SNAPSHOT_VERSION);
        JkPathTree.of(cachedDistrib).createIfNotExist().deleteRoot();

        Path jekaShellPath = getJekaShellPath();

        // Test without alias
        JkProcResult result = JkProcess.of(jekaShellPath.toString(), "-r", GIT_URL, "-lna", "#ok")
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .setEnv("jeka.distrib.repo", SNAPSHOT_REPO)
                .setEnv("jeka.version", SNAPSHOT_VERSION)
                .exec();
    }

    private void testWithSpecificJavaVersion() {
        String javaVersion = "20";
        JkLog.info("============ Testing specific Java version %s =========================", javaVersion);

        String distro = "corretto";
        // Delete cached distrib to force reloading
        Path cachedJdk= JkLocator.getCacheDir().resolve("jdks").resolve(distro + "-" + javaVersion);
        JkPathTree.of(cachedJdk).createIfNotExist().deleteRoot();

        Path jekaShellPath = getJekaShellPath();

        // Test without alias
        JkProcess.of(jekaShellPath.toString(), "-r", GIT_URL, "-lna", "#ok")
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .setEnv("jeka.distrib.location", jekaShellPath.getParent().toString())
                .setEnv("jeka.java.version", "20")
                .setEnv("jeka.java.distrib", distro)
                .exec();
        JkUtilsAssert.state(Files.exists(cachedJdk),"Jdk not found at %s", cachedJdk);
    }

}
