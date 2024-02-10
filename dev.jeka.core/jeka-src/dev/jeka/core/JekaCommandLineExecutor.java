package dev.jeka.core;

import dev.jeka.core.api.java.JkJavaProcess;
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
        String relPath = "jeka-output/distrib/" + scriptName();
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
        JkProcess process = JkProcess.of(cmd.toString())
                .setWorkingDir(baseDir)
                .setDestroyAtJvmShutdown(true)
                .setLogCommand(true)
                //.setLogWithJekaDecorator(true)
                .setFailOnError(true)
                .addParamsIf(!cmdLine.contains("-Djeka.java.version="), "-Djeka.java.version=8")
                .addParams(JkUtilsString.parseCommandline(cmdLine))
                .inheritJkLogOptions()
                .addParams("-dcf", "-lst", "-lsu", "-lri")


                // set explicitly jeka-core.jar to use, otherwise it may fetch a Jeka version from maven
                // if jeka.properties contains a jeka.version prop, has it happens when scaffolding.
                .setEnv("jeka.distrib.location", jekaShellCmd.toAbsolutePath().getParent().normalize().toString());

        // Add jacoco agent for gathering test coverage info
        if (jacocoAgentPath != null) {
            String arg = "-javaagent:" + jacocoAgentPath + "=destfile=" + jacocoReportFile + ",append=true";
            process.setEnv("JEKA_OPTS", arg);
        }
        return process;
    }

    protected void runJeka(boolean useBaseDirJeka, Path baseDir, String cmdLine) {
        prepareJeka(useBaseDirJeka, baseDir, cmdLine).exec();
    }

    private static String scriptName() {
        return JkUtilsSystem.IS_WINDOWS ? "jekaw.bat" : "jeka";
    }

}
