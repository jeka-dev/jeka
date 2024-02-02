package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkMemoryBufferLogDecorator;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;
import dev.jeka.core.tool.CommandLine.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import static dev.jeka.core.tool.Environment.logs;

@Command(name = "jeka",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        showDefaultValues = true,
        versionProvider = PicocliMain.VersionProvider.class,
        usageHelpAutoWidth = true,
        customSynopsis = {
            " ${COMMAND-NAME} [options] +<extra-classpath...> -D<prop-name=prop-value...> [COMMANDS]",
            "   or   jeka -r <git_url|path|@alias> [options] [COMMANDS]",
            "   or   jeka -r <git_url|path|@alias> [options] -p <program_args...>"
        },
        header = "Build and execute Java applications from source code.",
        description = {
            "",
            "Examples:",
            "  ${COMMAND-NAME} self: scaffold",
            "         (create a simple code base)",
            "  ${COMMAND-NAME} project: scaffold layout.style=SIMPLE",
            "         (create a Java project with simple layout ≠ Maven)",
            "  ${COMMAND-NAME} +dev.jeka:springboot-plugin springboot: project: scaffold ",
            "         (create a Spring-Boot project using plugin)",
            "  ${COMMAND-NAME} intellij: iml ",
            "         (Generate metadata iml file for Intellij)",
            "  ${COMMAND-NAME} -r https://github.com/myorg/myrepo#0.0.1 self: runJar",
            "         (Run the application hosted in this Git repo with tag 0.0.1)",
            ""
}
)
public class PicocliMainCommand implements Callable<Integer> {

    // injected by Main method
    final Path baseDir;

    @Option(names = {"-v", "--verbose"},
            description = "Log verbose messages.")
    private boolean logVerbose;

    @Option(names = { "-c", "--clean-output"},
            description = "Delete jeka-output directory prior running.")
    private boolean cleanOutput;

    @Option(names = { "--cw", "--clean-work"},
            description = "Delete .jeka-work directory prior running.")
    private boolean cleanWork;

    @Option(names = { "-i", "--info"},
            description = "Display info as versions, location, classpath,...")
    private boolean runtimeInfo;

    @Option(names = { "-r", "--remote"},
            paramLabel = "LOCATION",
            description = "Specify remote code base location. LOCATION may be a folder path, Git url or an alias.")
    private String fakeRemote;

    @Option(names = { "-f", "--remote-fresh"},
            description = "Forcing Git update when used with '-r'.")
    private boolean fakeRemoteFresh;

    @Option(names = { "--kb", "--kbean"},
            paramLabel = "KBEAN",
            description = "Set the KBean name to use as default.")
    private String defaultKBean;

    @Option(names = { "--lsu", "--log-startup"},
            description = "Log startup messages emitted during JeKa startup.")
    private boolean logStartUp;

    @Option(names = { "--ls"},
            paramLabel = "STYLE",
            defaultValue = "FLAT",
            description = "Set the JeKa log style : ${COMPLETION-CANDIDATES}.")
    private JkLog.Style logStyle = JkLog.Style.FLAT;

    @Option(names = { "--icf", "--ignore-fail"},
            description = "Try to keep running JeKa even if jeka-src compilation fails.")
    private boolean ignoreCompileFailure;

    public PicocliMainCommand(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public Integer call() throws Exception {

        final long start = System.nanoTime();

        // Set environment
        Environment.logs = logSettings();
        Environment.behavior = behaviorSettings();

        JkLog.setDecorator(logs.style);
        if (logs.banner) {
            Main.displayIntro();
        }
        if (logs.runtimeInformation) {
            JkInit.displayRuntimeInfo();
        }

        // By default, log working animation when working dir = base dir (this mean that we are not
        // invoking a tool packaged with JeKa.
        boolean logAnimation = baseDir.equals(Paths.get(""));
        if (logs.animation != null) {
            logAnimation = logs.animation;
        }
        JkLog.setAcceptAnimation(logAnimation);

        try {
            if (logVerbose) {
                JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
            }
            if (!logs.startUp) {  // log in memory and flush in console only on error
                JkBusyIndicator.start("Preparing Jeka classes and instance (Use -lsu option for details)");
                JkMemoryBufferLogDecorator.activateOnJkLog();
                JkLog.info("");   // To have a br prior the memory log is flushed
            }
            final Engine engine = new Engine(baseDir);
            engine.execute(Environment.parsedCmdLine);   // log in memory are inactivated inside this method if it goes ok
            if (logs.banner) {
                Main.displayOutro(start);
            }
            if (logs.totalDuration && !logs.banner) {
                Main.displayDuration(start);
            }
            return 0;
        } catch (final Throwable e) {
            JkBusyIndicator.stop();
            JkLog.restoreToInitialState();
            e.getStackTrace();
            if (e instanceof JkException) {
                if (logs.verbose) {
                    Main.handleRegularException(e);
                } else {
                    System.err.println(e.getMessage());
                }
            } else {
                if (JkMemoryBufferLogDecorator.isActive()) {
                    JkMemoryBufferLogDecorator.flush();
                }
                Main.handleRegularException(e);
            }
            if (logs.banner) {
                final int length = Main.printAscii(true, "text-failed.ascii");
                System.err.println(JkUtilsString.repeat(" ", length) + "Total run duration : "
                        + JkUtilsTime.durationInSeconds(start) + " seconds.");
            } else {
                System.err.println("Failed !");
            }
            return 1;
        }

    }

    private Environment.LogSettings logSettings() {
        return new Environment.LogSettings(
                logVerbose,
                false,
                logStartUp,
                false,
                runtimeInfo,
                false,
                logStyle,
                null,
                false);
    }

    private Environment.BehaviorSettings behaviorSettings() {
        return new Environment.BehaviorSettings(defaultKBean, cleanWork, cleanOutput, ignoreCompileFailure);
    }



}

