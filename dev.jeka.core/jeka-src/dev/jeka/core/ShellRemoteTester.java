package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcResult;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        testWithRemoteGitHttp();
        try {
            testWithSnapshotDistribVersion();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        testWithSpecificJavaVersion();
        testLsu();
    }

    private void testWithRemoteGitHttp() {
        JkLog.info("============ Testing 'jeka -r %s #ok =============================", GIT_URL);
        Path jekaShellPath = getJekaShellPath();


        // Test without alias
        JkProcResult result = JkProcess.of(jekaShellPath.toString(), "-rc", GIT_URL, "#ok")
                .setLogCommand(true)
                .setCollectOutput(true)
                .setEnv("jeka.distrib.location", jekaShellPath.getParent().toString())
                .exec();
        String output = result.getOutput();
        JkUtilsAssert.state(output.equals("ok\n"), "Command output was '%s', " +
                "expecting 'ok' followed by a breaking line)", output);
    }

    private void testWithSnapshotDistribVersion() throws IOException, InterruptedException {
        JkLog.info("============ Testing snapshot jeka version %s =============================", SNAPSHOT_VERSION);

        // Delete cached distrib to force reloading
        Path cachedDistrib = JkLocator.getCacheDir().resolve("distributions").resolve(SNAPSHOT_VERSION);
        JkPathTree.of(cachedDistrib).createIfNotExist().deleteRoot();

        Path jekaShellPath = getJekaShellPath();

        // Test without alias
        JkProcResult result = JkProcess.of(jekaShellPath.toString(), "-r", GIT_URL,  "#ok")
                .setLogCommand(true)
                .setLogWithJekaDecorator(false)
                .setCollectOutput(true)
                .redirectErrorStream(false)
                .setEnv("jeka.distrib.repo", SNAPSHOT_REPO)
                .setEnv("jeka.version", SNAPSHOT_VERSION)
                .exec();

        // Tests if messages written in stderr by jeka shell are not
        // collected in the output
        String output = result.getOutput();
        JkUtilsAssert.state(output.equals("ok\n"), "Command output was '%s', " +
                "expecting 'ok' followed by a breaking line)", output);
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
        // We pass 'jeka.java.version" in cmdLine args to test this feature
        JkProcResult procResult =JkProcess.of(jekaShellPath.toString(), "-r", GIT_URL, "#ok",
                        "-Djeka.java.version=" + javaVersion)
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .redirectErrorStream(false)
                .setCollectOutput(true)
                .setEnv("jeka.distrib.location", jekaShellPath.getParent().toString())
                .setEnv("jeka.java.distrib", distro)
                //.setEnv("jeka.java.version", "20")
                .exec();
        JkUtilsAssert.state(Files.exists(cachedJdk),"Jdk not downloaded at %s", cachedJdk);
        String output = procResult.getOutput();
        JkUtilsAssert.state(output.equals("ok\n"), "Command output was '%s', " +
                "expecting ending with 'ok 'followed by a breaking line)", output);
        System.setProperty("jeka.java.version", "");
    }

    // Test if -lsu let the distrib installation displayed on console
    private void testLsu() {
        JkLog.info("============ Testing snapshot jeka version %s =============================", SNAPSHOT_VERSION);

        // Delete cached distrib to force reloading
        Path cachedDistrib = JkLocator.getCacheDir().resolve("distributions").resolve(SNAPSHOT_VERSION);
        JkPathTree.of(cachedDistrib).createIfNotExist().deleteRoot();

        Path jekaShellPath = getJekaShellPath();

        // '-rc' is important to log git output
        JkProcResult result = JkProcess.of(jekaShellPath.toString(), "-rc", GIT_URL, "-lsu", "-la=false", "#ok")
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .setCollectOutput(true)
                .setEnv("jeka.distrib.repo", SNAPSHOT_REPO)
                .setEnv("jeka.version", SNAPSHOT_VERSION)
                .exec();
        String output = result.getOutput();
        JkUtilsAssert.state(!output.startsWith("ok"), "Command output was '%s', " +
                "expecting output from distrib installation", output);
    }


}