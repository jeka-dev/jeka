package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class ShellRemoteTester  extends JekaCommandLineExecutor {

    private static final String GIT_URL = "https://github.com/jeka-dev/sample-for-integration-test.git#0.0.1";

    private static final String SNAPSHOT_REPO = "https://oss.sonatype.org/content/repositories/snapshots";

    private static final String SNAPSHOT_VERSION = "0.11.x-SNAPSHOT";

    public static void main(String[] args) {
        new ShellRemoteTester().run();
    }

    void run() {
        JkLog.startTask("test-remote");
        testWithRemoteGitHttp();
        try {
            testWithSnapshotDistribVersion();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        testWithSpecificJavaVersion();
        testWithShortHand();
        testImplicitBuildWithCowSay();
        JkLog.endTask();
    }

    private void testWithRemoteGitHttp() {
        Path jekaShellPath = getJekaShellPath();

        // Test without alias
        JkProcResult result = JkProcess.of(jekaShellPath.toString(), "-ru", GIT_URL, "ok")
                .setLogCommand(true)
                .setCollectStdout(true)
                .setEnv("jeka.distrib.location", jekaShellPath.getParent().toString())
                .exec();
        String output = result.getStdoutAsString();
        JkUtilsAssert.state(output.equals("ok" + System.lineSeparator()), "Command output was '%s', " +
                "expecting 'ok' followed by a breaking line)", output);
    }

    private void testWithSnapshotDistribVersion() throws IOException, InterruptedException {

        // Delete cached distrib to force reloading
        Path cachedDistrib = JkLocator.getCacheDir().resolve("distributions").resolve(SNAPSHOT_VERSION);
        JkPathTree.of(cachedDistrib).createIfNotExist().deleteRoot();

        Path jekaShellPath = getJekaShellPath();

        // Test without alias
        JkProcResult result = JkProcess.of(jekaShellPath.toString(), "-r", GIT_URL, "ok")
                .setLogCommand(true)
                .setLogWithJekaDecorator(false)
                .setCollectStdout(true)
                .setCollectStderr(true)
                .redirectErrorStream(false)
                .setEnv("jeka.distrib.repo", SNAPSHOT_REPO)
                .setEnv("jeka.version", SNAPSHOT_VERSION)
                .exec();

        // Tests if messages written in stderr by jeka shell are not
        // collected in the output
        String output = result.getStdoutAsString();
        JkUtilsAssert.state(output.equals("ok" + System.lineSeparator()), "Command output was '%s', " +
                "expecting 'ok' followed by a breaking line)", output);
    }

    private void testWithSpecificJavaVersion() {
        String javaVersion = "20";

        String distro = "corretto";
        // Delete cached distrib to force reloading
        Path cachedJdk = JkLocator.getCacheDir().resolve("jdks").resolve(distro + "-" + javaVersion);
        JkPathTree.of(cachedJdk).createIfNotExist().deleteRoot();

        Path jekaShellPath = getJekaShellPath();

        // Test without alias
        // We pass 'jeka.java.version" in cmdLine args to test this feature
        JkProcResult procResult = JkProcess.of(jekaShellPath.toString(), "-r", GIT_URL, "ok",
                        "-Djeka.java.version=" + javaVersion)
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .redirectErrorStream(false)
                .setCollectStdout(true)
                .setEnv("jeka.distrib.location", jekaShellPath.getParent().toString())
                .setEnv("jeka.java.distrib", distro)
                //.setEnv("jeka.java.version", "20")
                .exec();
        JkUtilsAssert.state(Files.exists(cachedJdk), "Jdk not downloaded at %s", cachedJdk);
        String output = procResult.getStdoutAsString();
        JkUtilsAssert.state(output.equals("ok" + System.lineSeparator()), "Command output was '%s', " +
                "expecting ending with 'ok 'followed by a breaking line)", output);
        System.setProperty("jeka.java.version", "");
    }

    private void testWithShortHand() {

        Path jekaShellPath = getJekaShellPath();

        // Test without alias
        JkProcResult result = JkProcess.of(jekaShellPath.toString(), "::myShortHand")
                .setLogCommand(true)
                .setLogWithJekaDecorator(false)
                .setCollectStdout(true)
                .redirectErrorStream(false)
                .setEnv("jeka.cmd.myShortHand", "-r https://github.com/jeka-dev/sample-for-integration-test.git ok")
                .setEnv("jeka.distrib.repo", SNAPSHOT_REPO)
                .setEnv("jeka.version", SNAPSHOT_VERSION)
                .exec();

        // Tests if messages written in stderr by jeka shell are not
        // collected in the output
        String output = result.getStdoutAsString();
        JkUtilsAssert.state(output.equals("ok" + System.lineSeparator()), "Command output was '%s', " +
                "expecting 'ok' followed by a breaking line)", output);
    }

    private void testImplicitBuildWithCowSay() {

        Path jekaShellPath = getJekaShellPath();

        // We want also testing that sys properties declared as program arguments are
        // handled as regular sys properties
        JkProcResult result = JkProcess.of(jekaShellPath.toString(),
                        "-ru", "https://github.com/jeka-dev/demo-cowsay", "-p",  "Hello JeKa", "-Dcowsay.prefix=Mooo")
                .setLogCommand(true)
                .setCollectStdout(true)
                .setCollectStderr(true)
                .setLogWithJekaDecorator(false)
                .setEnv("jeka.distrib.repo", SNAPSHOT_REPO)
                .setEnv("jeka.version", SNAPSHOT_VERSION)
                .exec();
        String stdout = result.getStdoutAsString();
        String stderr = result.getStderrAsString();
        JkUtilsAssert.state(stdout.contains("MoooHello JeKa"), "Expecting output containing 'MoooHello JeKa', " +
                "was %n%s %n stderr was %n%s", stdout, stderr);
        JkUtilsAssert.state(stdout.trim().startsWith("_____"), "Expecting output starting " +
                "with '_____', was %n%s %n stderr was %n%s", stdout, stderr);
    }

}
