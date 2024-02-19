package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.CommandLine.Command;
import dev.jeka.core.tool.CommandLine.Option;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "jeka",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        showDefaultValues = true,
        versionProvider = PicocliMain.VersionProvider.class,
        usageHelpAutoWidth = true,
        customSynopsis = {
            " @|yellow ${COMMAND-NAME} [options] [COMMAND...] |@",
            "           (for executing KBean actions)",
            "   or   @|yellow ${COMMAND-NAME} [options] -p [PROGRAM_ARG...] |@",
            "           (for executing Java program)",
            "",
            "COMMANDS are in format: '<kbeanName:> [<propName>=[value]...] [methodName...]'",
            "",
            "KBEANS are scripts implemented as Java beans.",
            "The name could be either the full or short name of the bean class.",
            "The first letter's case-sensitivity and the 'KBean' suffix are optional.",
            "",
            "Command line can be interpolated by using '::<shorthand>' reference ",
            "to 'jeka.cmd.<shorthand>' property defined in global.properties file."

        },
        header = "Build and execute Java applications from source code.",
        description = {
            "",
            "Examples:",
            "  @|yellow ${COMMAND-NAME} base: scaffold |@",
            "         (create a basic code base by invoking 'scaffold' method on 'BaseKBean')",
            "  @|yellow ${COMMAND-NAME} project: scaffold layout.style=SIMPLE |@",
            "         (create a project, specifying 'ProjectKBean.layout.style' prop value)",
            "  @|yellow ${COMMAND-NAME} -cp dev.jeka:springboot-plugin springboot: project: scaffold |@",
            "         (create a Spring-Boot project using Spring-Boot plugin)",
            "  @|yellow ${COMMAND-NAME} myMethod myFieldA=8 myFieldB=false |@",
            "         (set props and invoke method on the default KBean)",
            "  @|yellow ${COMMAND-NAME} intellij: iml |@",
            "         (Generate metadata iml file for Intellij)",
            "  @|yellow ${COMMAND-NAME} -r https://github.com/myorg/myrepo#0.0.1 self: runJar |@",
            "         (Run the application hosted in this Git repo with tag 0.0.1)",
            "  @|yellow ${COMMAND-NAME} -rp https://github.com/myorg/myscript#0.0.1 arg0 arg1 ... |@",
            "         (Run the app with specified args, bypassing JeKa engine if possible)",
            "  @|yellow ${COMMAND-NAME} ::myscript arg0 arg1 ... |@",
            "         (Same but using cmd interpolation defined in user global.properties)",
            ""
        },
        footer = {
            "",
            "Execute @|yellow jeka -cmd |@ to get a list of available commands.",
            "Execute @|yellow jeka -i |@ to get base information."
        },
        optionListHeading = "Options:%n",
        subcommandsRepeatable = true,
        usageHelpWidth = 100,
        commandListHeading = "%nStandard KBeans (always available):%n"
)
public class PicocliMainCommand {

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

    @Option(names = {"--debug"},
            description = "Log debug level (very verbose)")
    private boolean logDebug;

    @Option(names = {"-st", "--stacktrace"},
            description = "Log verbose messages.")
    private boolean logStacktrace;

    @Option(names = {"-ld", "--duration"},
            description = "Log task durations")
    private boolean logDuration;

    @Option(names = { "-ls"},
            paramLabel = "STYLE",
            description = "Set the JeKa log style : ${COMPLETION-CANDIDATES}.")
    private JkLog.Style logStyle = JkLog.Style.INDENT;

    @Option(names = {"-cmd", "--commands"},
            paramLabel = "<|kbeanName>",
            description = "Display contextual help on commands",
            arity = "0..1",
            fallbackValue = " ")
    private String commandHelp;

    @Option(names = {"-la", "--animations"},
            description = "Display animations on console",
            arity = "0..1")
    private Boolean logAnimations;

    @Option(names = "-D", mapFallbackValue = "") // allow -Dkey
    void setProperty(Map<String, String> props) {
        props.forEach(System::setProperty);
    }

    EnvLogSettings logSettings() {
        return new EnvLogSettings(
                logVerbose,
                logDebug,
                logStacktrace,
                runtimeInfo,
                logDuration,
                logStyle,
                logAnimations,
                false);
    }

    EnvBehaviorSettings behaviorSettings() {
        return new EnvBehaviorSettings(defaultKBean, cleanWork, cleanOutput, ignoreCompileFailure, commandHelp);
    }

    JkDependencySet dependencies() {
        if (classpaths == null) {
            return JkDependencySet.of();
        }
        List<JkDependency> dependencies = classpaths.stream()
                .map(JkDependency::of)
                .collect(Collectors.toList());
        return JkDependencySet.of(dependencies);
    }

}

