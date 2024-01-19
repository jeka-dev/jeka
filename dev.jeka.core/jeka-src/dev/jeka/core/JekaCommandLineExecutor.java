package dev.jeka.core;

import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for running jeka executables.
 */
public abstract class JekaCommandLineExecutor {

    private final Path samplesRootDir;

    private final Path jekaDistribDir;

    private Path jacocoAgentPath;

    private Path jacocoReportFile;

    protected final JkProperties properties;

    protected JekaCommandLineExecutor(Path samplesRootDir, Path jekaDistribDir, JkProperties properties) {
        super();
        this.samplesRootDir = samplesRootDir;
        this.jekaDistribDir = jekaDistribDir;
        this.properties =properties;
    }

    public void setJacoco(Path agent, Path report) {
        jacocoAgentPath = agent.toAbsolutePath().normalize();
        jacocoReportFile = report.toAbsolutePath().normalize();
    }

    protected JekaCommandLineExecutor(Path projectRootDir, JkProperties properties) {
        this(
                projectRootDir.resolve("samples"),
                projectRootDir.resolve("dev.jeka.core/jeka-output/distrib"),
                properties);
    }

    protected void runBaseDirJeka(String projectDir, String cmdLine) {
        runJeka(true, projectDir, cmdLine);
    }

    protected void runDistribJeka(String projectDir, String cmdLine) {
        runJeka(false, projectDir, cmdLine);
    }

    protected void runDistribJeka(String projectDir, String cmdLine, String jekaJdkHome) {
        runJeka(false, projectDir, cmdLine, jekaJdkHome);
    }

    /**
     * @param useBaseDirJeka if true, will invoke 'jeka' shell script located in the basedir to run.
     */
    protected void runJeka(boolean useBaseDirJeka, String projectDir, String cmdLine) {
        runJeka(useBaseDirJeka, projectDir, cmdLine, null);
    }

    protected void runJeka(boolean useBaseDirJeka, String projectDir, String cmdLine, String jekaJdkHome) {
        String jdkHome = jekaJdkHome == null ?
                JkJavaProcess.CURRENT_JAVA_HOME.normalize().toString()
                        : jekaJdkHome;
        Path dir = this.samplesRootDir.resolve(projectDir);
        process(dir, useBaseDirJeka)
                .addParams(JkUtilsString.parseCommandline(cmdLine))
                .inheritJkLogOptions()
                .addParams("-dcf", "-lst", "-cw", "-lsu", "-lri")
                .setEnv("JEKA_JDK", jdkHome)
                .setEnv("jeka.distrib.location", jekaDistribDir.toAbsolutePath().toString())
                .run();
    }

    private static String localJekaCommand(Path baseDir) {
        String scriptName = JkUtilsSystem.IS_WINDOWS ? "jekaw.bat" : "jeka";
        return JkUtilsPath.relativizeFromWorkingDir(baseDir.resolve(scriptName)).toAbsolutePath().normalize().toString();
    }

    private String jekaCmd() {
        String scriptName = JkUtilsSystem.IS_WINDOWS ? "jeka.bat" : "jeka";
        System.out.println("=========== Base dir " + Paths.get("").toAbsolutePath());
        System.out.println("=========== jeka distrib dir " + jekaDistribDir);
        System.out.println("=========== jeka distrib dir absolute " + jekaDistribDir.toAbsolutePath());
        System.out.println("=========== jeka distrib dir relativize to working dir " + JkUtilsPath.relativizeFromWorkingDir(this.jekaDistribDir));
        return JkUtilsPath.relativizeFromWorkingDir(this.jekaDistribDir).resolve(scriptName)
                .toAbsolutePath().normalize().toString();
    }

    private JkProcess process(Path workingDir, boolean useWrapper) {
        String cmd = useWrapper ? localJekaCommand(workingDir) : jekaCmd();
        JkProcess result = JkProcess.of(cmd)
                .setWorkingDir(workingDir)
                .setLogCommand(true)
                .setLogWithJekaDecorator(true)
                .setFailOnError(true);
        if (jacocoAgentPath != null) {
            String arg = "-javaagent:" + jacocoAgentPath + "=destfile=" + jacocoReportFile + ",append=true";
            result.setEnv("JEKA_OPTS", arg);
        }
        return result;
    }


}
