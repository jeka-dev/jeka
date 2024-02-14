package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkMemoryBufferLogDecorator;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsTime;
import dev.jeka.core.tool.CommandLine.Command;
import dev.jeka.core.tool.CommandLine.Option;
import dev.jeka.core.tool.CommandLine.ParseResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static dev.jeka.core.tool.Environment.logs;

@Command(name = "jeka",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        showDefaultValues = true,
        versionProvider = PicocliMain.VersionProvider.class,
        usageHelpAutoWidth = true,
        customSynopsis = {
            " ${COMMAND-NAME} [options] [COMMANDS]",
            "           (for executing KBean actions)",
            "   or   ${COMMAND-NAME} [options] -p [PROGRAM_ARGS]",
            "           (for executing Java program)",
            "",
            "COMMANDS are in format: '<KBeanName:> [<propName>=[value]...] [methodName...]'",
            "KBEANS are scripts implemented as Java beans. The name could be either the full or short name of the bean class.",
            "The first letter's case-sensitivity and the 'KBean' suffix are optional.",
            "",
            "Command line can be interpolated by using '::<shorthand>' reference ",
            "to 'jeka.cmd.<shorthand>' property defined in global.properties file."

        },
        header = "Build and execute Java applications from source code.",
        description = {
            "",
            "Examples:",
            "  ${COMMAND-NAME} self: scaffold",
            "         (create a basic code base by invoking 'scaffold' method on 'SelfKBean')",
            "  ${COMMAND-NAME} project: scaffold layout.style=SIMPLE",
            "         (create a project, specifying 'ProjectKBean.layout.style' prop value)",
            "  ${COMMAND-NAME} -cp dev.jeka:springboot-plugin springboot: project: scaffold",
            "         (create a Spring-Boot project using Spring-Boot plugin)",
            "  ${COMMAND-NAME} myMethod myFieldA=8 myFieldB=false",
            "         (set props and invoke method on the first KBean found in 'jeka-src')",
            "  ${COMMAND-NAME} intellij: iml ",
            "         (Generate metadata iml file for Intellij)",
            "  ${COMMAND-NAME} -r https://github.com/myorg/myrepo#0.0.1 self: runJar",
            "         (Run the application hosted in this Git repo with tag 0.0.1)",
            "  ${COMMAND-NAME} -rp https://github.com/myorg/myscript#0.0.1 arg0 arg1 ...",
            "         (Run the app with specified args, bypassing JeKa engine if possible)",
            "  ${COMMAND-NAME} ::myscript arg0 arg1 ...",
            "         (Same but using cmd interpolation defined in user global.properties)",
            ""
        },
        optionListHeading = "Options:%n",
        subcommandsRepeatable = true,
        usageHelpWidth = 100,
        commandListHeading = "%nStandard KBeans (always available):%n"
)
public class PicocliMainCommand {

    // injected by Main method
    final Path baseDir;

    @Option(names = { "-c", "--clean-output"},
            description = "Delete jeka-output directory prior running.")
    private boolean cleanOutput;

    @Option(names = { "-cw", "--clean-work"},
            description = "Delete .jeka-work directory prior running.")
    private boolean cleanWork;

    @Option(names = { "-cp", "--classpath"},
            paramLabel = "<COORDINATE|PATH>",
            split = ",",
            description = {
                "Add elements to the classpath as 'groupId:artifactId[:version]' coordinates or file-system paths."
            }
    )
    private List<String> classpaths;

    @Option(names = {"-p", "--program"},
            description = "Indicate to run directly the built Java program when present, bypassing the JeKa execution engine.")
    private boolean fakeProgram;  // Handled at shell level

    @Option(names = { "-i", "--info"},
            description = "Display info as versions, location, classpath,...")
    private boolean runtimeInfo;

    @Option(names = { "-r", "--remote"},
            paramLabel = "LOCATION",
            description = "Specify remote code base location. LOCATION may be a folder path, Git url or an alias.")
    private String fakeRemote;  // Handled at shell level

    @Option(names = { "-f", "--remote-fresh"},
            description = "Forcing Git update when used with '-r'.")
    private boolean fakeRemoteFresh;  // Handled at shell level

    @Option(names = { "-icf", "--ignore-fail"},
            description = "Try to keep running JeKa even if jeka-src compilation fails.")
    private boolean ignoreCompileFailure;

    @Option(names = { "-kb", "--kbean"},
            paramLabel = "KBEAN",
            description = "Set the KBean name to use as default.")
    private String defaultKBean;

    @Option(names = { "-D<name>"},
            paramLabel = "<value>",
            description = "Define system property")
    private String[] fakeSysProp;  // Handled at shell level

    @Option(names = {"-v", "--verbose"},
            description = "Log verbose messages.")
    private boolean logVerbose;

    @Option(names = { "-ls"},
            paramLabel = "STYLE",
            defaultValue = "FLAT",
            description = "Set the JeKa log style : ${COMPLETION-CANDIDATES}.")
    private JkLog.Style logStyle = JkLog.Style.FLAT;

    @Option(names = {"--debug"},
            description = "Log debug level (very verbose)")
    private boolean logDebug;


    public PicocliMainCommand(Path baseDir) {
        this.baseDir = baseDir;
    }




    public int run(ParseResult parseResult) {

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

    EnvLogSettings logSettings() {
        return new EnvLogSettings(
                logVerbose,
                logDebug,
                false,
                runtimeInfo,
                false,
                logStyle,
                null,
                false);
    }

    EnvBehaviorSettings behaviorSettings() {
        return new EnvBehaviorSettings(defaultKBean, cleanWork, cleanOutput, ignoreCompileFailure);
    }

}

