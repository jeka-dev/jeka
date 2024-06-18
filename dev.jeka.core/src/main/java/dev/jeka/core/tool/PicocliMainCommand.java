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
        versionProvider = Main.VersionProvider.class,
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
            "  @|yellow ${COMMAND-NAME} -cp=dev.jeka:springboot-plugin springboot: project: scaffold |@",
            "         (create a Spring-Boot project using Spring-Boot plugin)",
            "  @|yellow ${COMMAND-NAME} myMethod myFieldA=8 myFieldB=false |@",
            "         (set props and invoke method on the default KBean)",
            "  @|yellow ${COMMAND-NAME} intellij: iml |@",
            "         (Generate metadata iml file for Intellij)",
            "  @|yellow ${COMMAND-NAME} -r https://github.com/myorg/myscript#0.0.1 -p arg0 arg1 ... |@",
            "         (Run the app with specified args, bypassing JeKa engine if possible)",
            "  @|yellow ${COMMAND-NAME} ::myscript arg0 arg1 ... |@",
            "         (Same but using cmd interpolation defined in user global.properties)",
            ""
        },
        footer = {
            "",
            "Execute @|yellow jeka --doc |@     to display available KBean commands.",
            "Execute @|yellow jeka --inspect |@ to display runtime setup information."
        },
        optionListHeading = "Options:%n",
        subcommandsRepeatable = true,
        usageHelpWidth = 100,
        commandListHeading = "%nStandard KBeans (always available):%n"
)
public class PicocliMainCommand {

    @Option(names = { "-c", "--clean"},
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

    @Option(names = {"-p", "--program-args"},
            description = "Indicate to run directly the built Java program when present, bypassing the JeKa execution engine.")
    private boolean fakeProgram;  // Handled at shell level

    @Option(names = { "-f", "--force"},
            description = "Try to keep running JeKa even if jeka-src compilation fails.")
    private boolean forceMode;

    @Option(names = { "-i", "--inspect"},
            description = "Display information about runtime setup (versions, properties, locations, classpaths, kbeans...")
    private boolean runtimeInfo;

    @Option(names = { "-r", "--remote"},
            arity = "1",
            paramLabel = "LOCATION",
            description = "Specify remote code base location. LOCATION may be a folder path, Git url or an alias.")
    private String fakeRemote;  // Handled at shell level

    @Option(names = { "-u", "--remote-update"},
            description = "Forcing Git update when used with '--remote' or '-r'.")
    private boolean fakeRemoteUpdate;  // Handled at shell level

    @Option(names = { "-kb", "--kbean"},
            paramLabel = "KBEAN",
            description = "Set the KBean name to use as default.")
    private String defaultKBean;

    @Option(names = { "-sk", "--skip-compile"},
            description = "Do not compile jeka-src.")
    private boolean skipCompile;

    @Option(names = {"-v", "--verbose"},
            description = "Log verbose messages.")
    private boolean logVerbose;

    @Option(names = {"--debug"},
            description = "Log debug level (very verbose).")
    private boolean logDebug;

    @Option(names = {"-q", "--quiet"},
            description = "Turn off logs.")
    private boolean logQuiet;

    @Option(names = {"-st", "--stacktrace"},
            description = "Log stack traces.")
    private boolean logStacktrace;

    @Option(names = {"-ld", "--duration"},
            description = "Log task durations.")
    private boolean logDuration;

    @Option(names = { "-ls", "--log-style"},
            paramLabel = "STYLE",
            description = "Set the JeKa log style : ${COMPLETION-CANDIDATES}.")
    private final JkLog.Style logStyle = JkLog.Style.INDENT;

    @Option(names = {"-la", "--animations"},
            description = "Display animations on console.",
            arity = "0..1")
    private Boolean logAnimations;

    @Option(names = {"--stderr"},
            description = "Log on stderr instead of stdout.")
    private boolean logOnStderr;

    @Option(names = {"--ivy-verbose"},
            description = "Log verbose Ivy messages")
    private boolean logIvyVerbose;

    @Option(names = {"--doc"},
            description = "Display documentation on default KBean, or a specific KBean if mentioned as 'aKBean: --doc'.")
    private boolean fakeDoc;  // Handled at upper level

    @Option(names = "-D", mapFallbackValue = "", description = "Define system property as '-Dmy.key=my.value'.") // allow -Dkey
    void setProperty(Map<String, String> props) {
        props.forEach(System::setProperty);
    }


    LogSettings logSettings() {
        return new LogSettings(
                logVerbose,
                logDebug,
                logQuiet,
                logStacktrace,
                runtimeInfo,
                logDuration,
                logStyle,
                logAnimations,
                logOnStderr,
                logIvyVerbose);
    }

    BehaviorSettings behaviorSettings() {
        return new BehaviorSettings(
                defaultKBean,
                cleanWork,
                cleanOutput,
                forceMode,
                skipCompile);
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

